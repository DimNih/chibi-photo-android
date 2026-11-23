package com.chibi.photo.physics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.hypot

/**
 * Hardware accelerometer listener that feeds physical tilt and shake impulses
 * into the viscoelastic mesh engine.
 */
class TiltSensorManager(
    context: Context,
    private val onTiltUpdate: (normGx: Float, normGy: Float, shakeImpulse: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f
    private var isRegistered = false

    fun start() {
        if (!isRegistered && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            isRegistered = true
        }
    }

    fun stop() {
        if (isRegistered) {
            sensorManager?.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (values.size < 3) return

        // Android accelerometer: +X is left tilt, +Y is upright gravity
        val ax = -values[0]
        val ay = values[1] - 6.5f // Subtract natural handheld viewing angle (~45 deg)
        val az = values[2]

        val normGx = (ax / 9.81f).coerceIn(-1f, 1f)
        val normGy = (ay / 9.81f).coerceIn(-1f, 1f)

        val delta = hypot(ax - lastAx, ay - lastAy) + kotlin.math.abs(az - lastAz) * 0.5f
        lastAx = ax
        lastAy = ay
        lastAz = az

        val shakeImpulse = if (delta > 6.5f) (delta * 45f).coerceAtMost(520f) else 0f
        onTiltUpdate(normGx, normGy, shakeImpulse)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

