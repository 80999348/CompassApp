package com.example.compass.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 传感器中枢：融合旋转矢量传感器计算真北/磁北方位角，
 * 同时读取磁场、重力、加速度数据供水平仪与校准页使用。
 */
class CompassViewModel(context: Context) : SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gravitySensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: accelSensor
    private val pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE)

    // ---------- 对外状态 ----------
    /** 方位角 0..360（默认磁北；开启位置服务后可切换真北） */
    var heading by mutableFloatStateOf(0f); private set
    /** 平滑后的方位角（低通滤波，供动画插值） */
    var smoothHeading by mutableFloatStateOf(0f); private set
    /** 磁场强度 μT */
    var fieldStrength by mutableFloatStateOf(0f); private set
    /** 磁场是否可靠（>70μT 视为受干扰） */
    var fieldAbnormal by mutableStateOf(false); private set
    /** 俯仰/横滚（度） */
    var pitch by mutableFloatStateOf(0f); private set
    var roll by mutableFloatStateOf(0f); private set
    /** 是否已获取位置用于真北修正 */
    var trueHeadingOffset by mutableFloatStateOf(0f); private set
    var useTrueNorth by mutableStateOf(false)

    /** 当前经纬度（纬度 to 经度），null 表示尚未定位 */
    var latLon by mutableStateOf<Pair<Double, Double>?>(null); private set
    /** GPS 海拔（米），null 表示不可用 */
    var gpsAltitude by mutableStateOf<Float?>(null); private set
    /** 气压 hPa（百帕），0 表示无气压计 */
    var pressure by mutableFloatStateOf(0f); private set
    /** 气压基准校准量（米）：用 GPS 海拔反算，抵消天气导致的海平面气压偏差 */
    private var baroBias = 0f
    private var baroBiasReady = false
    /** 用户手动锁定的海拔修正量（米），非 0 时优先于自动校准 */
    var manualBias by mutableFloatStateOf(0f); private set
    var manualCalibrated by mutableStateOf(false); private set

    /** 联网校准：是否开启（默认开） */
    var onlineCalibEnabled by mutableStateOf(true)
    /** 联网校准获取的当地海平面气压 QNH（hPa），0 表示未获取 */
    var qnh by mutableFloatStateOf(0f); private set
    /** 联网校准状态：idle / loading / ok / failed */
    var onlineCalibState by mutableStateOf("idle"); private set
    private var lastQnhFetchAt = 0L

    /** 海平面气压基准：联网获取的 QNH 优先，否则用标准大气 1013.25 */
    private val qnhReference: Float
        get() = if (qnh > 0f) qnh else SensorManager.PRESSURE_STANDARD_ATMOSPHERE

    /** 气压反演海拔（米，基准 = 联网 QNH / GPS 自动校准 / 手动校准），null 表示无气压计 */
    val baroAltitude: Float?
        get() = rawBaroAltitude()?.let { it + baroBias + manualBias }

    private fun rawBaroAltitude(): Float? =
        if (pressure > 0f) SensorManager.getAltitude(qnhReference, pressure) else null

    /**
     * 最终展示海拔：优先 GPS（真实卫星高度），无 GPS 时用已校准的气压反演。
     * 这就是主流指南针 App 的做法，避免气压随天气波动导致负海拔。
     */
    val displayAltitude: Float?
        get() = if (gpsAltitude != null)
            gpsAltitude!! + manualBias
        else baroAltitude

    /** 海拔数据来源，用于 UI 提示 */
    val altitudeSource: String
        get() = when {
            gpsAltitude != null -> if (manualCalibrated) "GPS+校准" else "GPS"
            baroAltitude != null -> when {
                manualCalibrated -> "气压+校准"
                qnh > 0f -> "联网校准"
                else -> "气压"
            }
            else -> "--"
        }

    /** 气压来源且尚未校准（无 QNH / GPS / 手动校准）时提示 ⚠ */
    val showUncalibratedWarn: Boolean
        get() = gpsAltitude == null && pressure > 0f && qnh <= 0f &&
            !manualCalibrated && !baroBiasReady

    /**
     * 手动海拔校准：以当前显示的推算值为基准，把偏差量锁存为 manualBias。
     * 之后所有海拔读数都会叠加该修正量，解决室内无 GPS 时气压偏差问题。
     */
    fun calibrateAltitudeManually(realAltitude: Float) {
        val current = displayAltitude ?: run {
            manualBias = realAltitude
            manualCalibrated = true
            return
        }
        manualBias = realAltitude - current
        manualCalibrated = true
    }

    /** 清除手动校准，恢复自动（GPS / 气压）模式 */
    fun clearManualCalibration() {
        manualBias = 0f
        manualCalibrated = false
    }

    /**
     * 联网校准：请求 Open-Meteo 免费天气接口获取当前位置的海平面气压（pressure_msl, QNH）。
     * 以 QNH 为基准反演海拔，等效于参考 App 的"联网校准海拔"。
     */
    fun fetchSeaLevelPressure() {
        val ll = latLon ?: return
        lastQnhFetchAt = System.currentTimeMillis()
        onlineCalibState = "loading"
        kotlin.concurrent.thread(name = "qnh-fetch") {
            try {
                val url = java.net.URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=${"%.4f".format(ll.first)}" +
                        "&longitude=${"%.4f".format(ll.second)}" +
                        "&current=pressure_msl&timezone=auto"
                )
                val body = url.openConnection().apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }.getInputStream().bufferedReader().use { it.readText() }
                val msl = org.json.JSONObject(body)
                    .getJSONObject("current").getDouble("pressure_msl")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    qnh = msl.toFloat()
                    onlineCalibState = "ok"
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onlineCalibState = "failed"
                }
            }
        }
    }

    /** 切换联网校准开关；打开时立即刷新一次 */
    fun toggleOnlineCalib(enabled: Boolean) {
        onlineCalibEnabled = enabled
        if (enabled && qnh <= 0f) fetchSeaLevelPressure()
    }

    /** 位置服务：读取最后已知位置 + 持续监听，更新经纬度/海拔并做真北修正 */
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager

    private val locationListener = android.location.LocationListener { loc ->
        latLon = loc.latitude to loc.longitude
        acceptGpsAltitude(loc)
        updateDeclination(loc)
        // 联网校准：拿到定位后每 30 分钟刷新一次当地海平面气压
        if (onlineCalibEnabled && System.currentTimeMillis() - lastQnhFetchAt > 30 * 60_000L) {
            fetchSeaLevelPressure()
        }
    }

    /**
     * GPS 海拔有效性验证：
     * 许多手机在垂直定位未收敛时会返回 0（但 hasAltitude() 为 true），
     * 盲目采信会把真实海拔顶成 -0.0/0.0。
     * 只有垂直精度良好且数值非 0 时才采信，否则回退到联网校准的气压海拔。
     */
    private fun acceptGpsAltitude(loc: android.location.Location) {
        if (!loc.hasAltitude()) return
        val alt = loc.altitude.toFloat()
        val vAcc = if (android.os.Build.VERSION.SDK_INT >= 26) loc.verticalAccuracyMeters else -1f
        val valid = (kotlin.math.abs(alt) > 1f) &&
            (vAcc < 0f || vAcc <= 30f)   // 无精度信息时只按数值判断
        if (valid) {
            gpsAltitude = alt
            calibrateBarometer(alt)
        } else {
            // 不可信：丢弃，保持 null 让 UI 回退到气压+联网校准
            gpsAltitude = null
        }
    }

    /**
     * 用 GPS 海拔反算气压基准偏差：
     * bias = 真实海拔 - 标准大气推算海拔。
     * 这样在失去 GPS 后，气压反演仍能给出接近真实的海拔。
     */
    private fun calibrateBarometer(gpsAlt: Float) {
        val raw = rawBaroAltitude() ?: return
        // 平滑更新，避免 GPS 抖动导致海拔跳变
        baroBias = if (baroBiasReady) baroBias * 0.9f + (gpsAlt - raw) * 0.1f else gpsAlt - raw
        baroBiasReady = true
    }

    private fun updateDeclination(loc: android.location.Location) {        val gf = android.hardware.GeomagneticField(
            loc.latitude.toFloat(), loc.longitude.toFloat(),
            loc.altitude.toFloat(), System.currentTimeMillis()
        )
        trueHeadingOffset = gf.declination
        useTrueNorth = true
    }

    /** 需已获得定位权限后调用 */
    fun startLocationUpdates() {
        try {
            locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)?.let {
                latLon = it.latitude to it.longitude
                acceptGpsAltitude(it)
                // 注意：最后已知位置可能较旧，此处不做气压校准
                updateDeclination(it)
            }
            locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)?.let {
                if (latLon == null) latLon = it.latitude to it.longitude
            }
            locationManager.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER, 2000L, 1f, locationListener,
                android.os.Looper.getMainLooper()
            )
        } catch (_: Exception) { /* 无权限或无 GPS 提供者 */ }
    }

    fun stopLocationUpdates() {
        try { locationManager.removeUpdates(locationListener) } catch (_: Exception) {}
    }

    // ---------- 传感器监听 ----------
    fun startSensors() {
        val rate = SensorManager.SENSOR_DELAY_GAME
        rotSensor?.let { sm.registerListener(this, it, rate) }
        gravitySensor?.let { sm.registerListener(this, it, rate) }
        magSensor?.let { sm.registerListener(this, it, rate) }
        pressureSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stopSensors() = sm.unregisterListener(this)

    private val lastAccel = FloatArray(3)
    private val lastMag = FloatArray(3)

    /** 归一化重力向量（供水平仪使用，Compose 可观察） */
    var gravity by mutableStateOf(floatArrayOf(0f, 0f, -9.81f)); private set

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val r = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(r, e.values)
                val o = FloatArray(3)
                SensorManager.getOrientation(r, o)
                var h = Math.toDegrees(o[0].toDouble()).toFloat()
                h = (h + 360f) % 360f
                if (useTrueNorth) h = (h + trueHeadingOffset + 360f) % 360f
                heading = h
                // 角度低通滤波，处理 0/360 环绕
                var diff = smoothHeading - h
                if (diff > 180) diff -= 360 else if (diff < -180) diff += 360
                smoothHeading = (smoothHeading - diff * 0.25f + 360f) % 360f
                pitch = Math.toDegrees(o[1].toDouble()).toFloat()
                roll = Math.toDegrees(o[2].toDouble()).toFloat()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) lastMag[i] = lastMag[i] * 0.7f + e.values[i] * 0.3f
                fieldStrength = sqrt(
                    lastMag[0] * lastMag[0] + lastMag[1] * lastMag[1] + lastMag[2] * lastMag[2]
                )
                fieldAbnormal = fieldStrength > 70f || fieldStrength < 20f
            }
            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) lastAccel[i] = lastAccel[i] * 0.8f + e.values[i] * 0.2f
                gravity = lastAccel.copyOf()
            }
            Sensor.TYPE_PRESSURE -> {
                pressure = e.values[0]
            }
            Sensor.TYPE_GRAVITY -> {
                gravity = gravity.copyOf().also { for (i in 0..2) it[i] = it[i] * 0.6f + e.values[i] * 0.4f }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            fieldAbnormal = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
        }
    }

    companion object {
        fun directionName(deg: Float): String {
            val names = arrayOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
            return names[((deg + 22.5f) / 45f).toInt() % 8]
        }

        /** 十进制度 → 度分秒字符串，如 北纬 30°55′16″ */
        fun toDms(v: Double, positive: String, negative: String): String {
            val hemi = if (v >= 0) positive else negative
            val a = kotlin.math.abs(v)
            val d = a.toInt()
            val mF = (a - d) * 60
            val m = mF.toInt()
            val s = ((mF - m) * 60).toInt()
            return "$hemi ${d}°$m′$s″"
        }

        fun tiltPercent(g: FloatArray): Pair<Float, Float> {
            val gx = g[0] / 9.81f
            val gy = g[1] / 9.81f
            return atan2(gx, -g[2]) to atan2(gy, -g[2])
        }
    }
}
