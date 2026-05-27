package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel

class ScaffoldHeightViewModel : ViewModel() {

    // Calibration settings state
    var cameraFov by mutableStateOf(54.0) // Camera Vertical FOV in degrees (Standard is around 54.0)
    var manualDistance by mutableStateOf(5.0) // Estimated distance to scaffold in meters
    var eyeHeight by mutableStateOf(1.6) // Eye level / phone height in meters for ground estimation
    var distanceMethod by mutableStateOf("MANUAL") // "MANUAL" or "AUTO" (derived from eyeHeight and base angle)
    var measuringMode by mutableStateOf("TOUCH") // "TOUCH" (Tapping screen) or "AIM" (Sights crosshair in center)

    // Touch positions (Coordinates on screen)
    var basePoint by mutableStateOf<Offset?>(null)
    var topPoint by mutableStateOf<Offset?>(null)

    // Sighted angles (Captured vertical tilt in radians for AIM mode)
    var baseAngle by mutableStateOf<Double?>(null)
    var topAngle by mutableStateOf<Double?>(null)

    // Calculation outputs
    var measuredHeight by mutableStateOf(0.0)
    var measuredDistance by mutableStateOf(0.0)
    var measuredPrecision by mutableStateOf(0.0)
    var calculationPerformed by mutableStateOf(false)

    // Set points methods
    fun setBasePosition(offset: Offset) {
        basePoint = offset
        calculationPerformed = false
    }

    fun setTopPosition(offset: Offset) {
        topPoint = offset
        calculationPerformed = false
    }

    fun captureBaseAngle(currentPitchDegrees: Double) {
        baseAngle = Math.toRadians(currentPitchDegrees)
        calculationPerformed = false
    }

    fun captureTopAngle(currentPitchDegrees: Double) {
        topAngle = Math.toRadians(currentPitchDegrees)
        calculationPerformed = false
    }

    // Reset everything
    fun reset() {
        basePoint = null
        topPoint = null
        baseAngle = null
        topAngle = null
        measuredHeight = 0.0
        measuredDistance = 0.0
        measuredPrecision = 0.0
        calculationPerformed = false
    }

    // Main calculation trigger
    fun computeHeight(devicePitchDegrees: Double, screenHeightPx: Double?) {
        val fovRad = Math.toRadians(cameraFov)
        val finalBaseAngleRad: Double
        val finalTopAngleRad: Double

        if (measuringMode == "TOUCH") {
            val base = basePoint ?: return
            val top = topPoint ?: return
            val heightPx = screenHeightPx ?: 1920.0 // Default fallback if not layout measured

            // Normalized Y coord from center: Top of screen y=0 is -0.5, bottom y=H is 0.5
            val baseNormY = (base.y / heightPx) - 0.5
            val topNormY = (top.y / heightPx) - 0.5

            val livePitchRad = Math.toRadians(devicePitchDegrees)

            // Absolute angle relative to horizon plane: pitch - vertical angle off-center
            // (Note: we subtract normY because top of screen has negative normY which means positive view elevation)
            finalBaseAngleRad = livePitchRad - (baseNormY * fovRad)
            finalTopAngleRad = livePitchRad - (topNormY * fovRad)
        } else {
            // "AIM" Mode: Using captured angles
            finalBaseAngleRad = baseAngle ?: return
            finalTopAngleRad = topAngle ?: return
        }

        // Determine estimated horizontal distance to target
        val d: Double = if (distanceMethod == "AUTO") {
            // Calculate distance based on ground-angle: eyeHeight = d * -tan(baseAngle)
            // Works only if base point is on ground (looking down) which makes finalBaseAngleRad negative
            if (finalBaseAngleRad < -0.05) {
                val calculatedDistance = eyeHeight / -Math.tan(finalBaseAngleRad)
                // Cap it at a realistic range [1m to 50m]
                calculatedDistance.coerceIn(1.0, 50.0)
            } else {
                // If aiming horizon or up, fallback to manual input and alert
                manualDistance
            }
        } else {
            manualDistance
        }

        // Calculate heights above visual center projection
        // height = d * (tan(topAngle) - tan(baseAngle))
        val h = d * (Math.sin(finalTopAngleRad - finalBaseAngleRad) / (Math.cos(finalTopAngleRad) * Math.cos(finalBaseAngleRad)))
        // Note: d * (tan(top) - tan(base)) is equivalent to above trig identity which is extremely robust. Let's use it.
        val heightResult = d * (Math.tan(finalTopAngleRad) - Math.tan(finalBaseAngleRad))

        measuredDistance = d
        measuredHeight = if (heightResult > 0.0) heightResult else 0.0
        measuredPrecision = calculateTheoreticalPrecision(d, finalTopAngleRad, finalBaseAngleRad)
        calculationPerformed = true
    }

    private fun calculateTheoreticalPrecision(d: Double, topRad: Double, baseRad: Double): Double {
        // Sensor uncertainty: ~0.8 degrees of tilt deviation
        val delta = Math.toRadians(0.8)
        val h1 = d * (Math.tan(topRad + delta) - Math.tan(baseRad - delta))
        val h2 = d * (Math.tan(topRad - delta) - Math.tan(baseRad + delta))
        val error = Math.abs(h1 - h2) / 2.0
        // Cap precision floor to 0.1m for UI realism
        return Math.max(0.1, error)
    }
}
