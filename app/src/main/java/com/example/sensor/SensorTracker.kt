package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Output states
    private val _pitch = MutableStateFlow(0f) // Pitch in degrees
    val pitch: StateFlow<Float> = _pitch

    private val _roll = MutableStateFlow(0f) // Roll in degrees
    val roll: StateFlow<Float> = _roll

    private val _isRotationalSensorActive = MutableStateFlow(false)
    val isRotationalSensorActive: StateFlow<Boolean> = _isRotationalSensorActive

    // Sensors
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Intermediate reading buffers for low-pass filtering
    private var gravityValues = FloatArray(3)
    private var magneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false

    // Low-pass filter coefficient for accelerometer fallback (0.1f means smooth, less jitter)
    private val filterAlpha = 0.12f

    fun start() {
        // Try high-accuracy Rotational Vector Sensor first
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            _isRotationalSensorActive.value = true
        } else {
            // Fallback to Accel + Mag
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometerSensor != null) {
                sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_UI)
            }
            _isRotationalSensorActive.value = false
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationValues)

                // Convert orientation components from radians to degrees
                // orientationValues[1] is Pitch (tilt front/back)
                // orientationValues[2] is Roll (tilt left/right)
                val rawPitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                val rawRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                // Apply mild smoothing to rotation vector too for pixel-perfect stability
                _pitch.value = _pitch.value * (1f - filterAlpha) + rawPitch * filterAlpha
                _roll.value = _roll.value * (1f - filterAlpha) + rawRoll * filterAlpha
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // Apply low-pass filter to raw accelerations
                for (i in 0..2) {
                    gravityValues[i] = gravityValues[i] * (1f - filterAlpha) + event.values[i] * filterAlpha
                }
                hasGravity = true
                processOrientationFallback()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) {
                    magneticValues[i] = magneticValues[i] * (1f - filterAlpha) + event.values[i] * filterAlpha
                }
                hasMagnetic = true
                processOrientationFallback()
            }
        }
    }

    private fun processOrientationFallback() {
        // If rotational vector was active, do not shadow with fallback
        if (rotationVectorSensor != null) return

        if (hasGravity && hasMagnetic) {
            val rMatrix = FloatArray(9)
            val iMatrix = FloatArray(9)
            if (SensorManager.getRotationMatrix(rMatrix, iMatrix, gravityValues, magneticValues)) {
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(rMatrix, orientationValues)
                
                _pitch.value = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                _roll.value = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
                return
            }
        }

        // Ultimate fallback: Raw Accelerometer gravity-vector based calculations
        // This gets the orientation even with absolutely no magnetometer or rotation vector (essential for some emulators)
        if (hasGravity) {
            val ax = gravityValues[0]
            val ay = gravityValues[1]
            val az = gravityValues[2]

            // Calculate tilt relative to gravity vector
            // Ay is primary vertical axis in portrait
            val gSum = sqrt(ax * ax + ay * ay + az * az)
            if (gSum > 0.1f) {
                val rawPitchRad = atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble()))
                val rawRollRad = atan2(ax.toDouble(), ay.toDouble())

                _pitch.value = Math.toDegrees(rawPitchRad).toFloat()
                _roll.value = Math.toDegrees(rawRollRad).toFloat()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
