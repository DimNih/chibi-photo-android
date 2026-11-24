package com.chibi.photo.physics

import android.graphics.*
import kotlin.math.*

enum class KineticTool(val label: String, val hint: String) {
    PULL("Pinch", "Grab and stretch elastic zones with multi-touch recoil"),
    POKE("Poke", "Press to indent and ripple soft surfaces outward"),
    SWIRL("Swirl", "Twist vertices around touch anchor with rotational torque"),
    PULSE("Pulse", "Auto-oscillate active zones to a steady studio tempo"),
    GYRO("Gravity", "Tilt or shake your device to sway elastic regions")
}

enum class MaterialPreset(
    val title: String,
    val subtitle: String,
    val frequencyHz: Float,
    val dampingZeta: Float,
    val softness: Float,
    val coupling: Float,
    val bulge: Float
) {
    SOFT_GEL("Soft gel", "6.8 Hz · low damping", 6.8f, 0.13f, 0.95f, 0.08f, 0.22f),
    WATER_DROP("Water drop", "5.2 Hz · wave ripple", 5.2f, 0.08f, 0.94f, 0.16f, 0.26f),
    LATEX_SNAP("Latex snap", "11.2 Hz · high tension", 11.2f, 0.11f, 0.84f, 0.10f, 0.15f),
    WARM_DOUGH("Warm dough", "4.6 Hz · heavy rebound", 4.6f, 0.28f, 0.98f, 0.12f, 0.20f),
    MEMORY_FOAM("Memory foam", "3.2 Hz · slow recovery", 3.2f, 0.62f, 0.82f, 0.05f, 0.10f)
}

enum class ZoneTemplate(val label: String) {
    DUAL_CHEEKS("Dual cheeks"),
    CENTER_CORE("Center core"),
    UPPER_CREST("Upper crests"),
    FULL_BODY("Full canvas"),
    CLEAR("Clear all")
}

/**
 * 2D Viscoelastic Spring-Damper Mesh Engine with 5 kinetic tools,
 * volume-preserving bulge, reactive strain blush, and live telemetry waveform.
 */
class MochiMeshEngine(
    val meshWidth: Int = 32,
    val meshHeight: Int = 32
) {
    val totalVerts: Int = (meshWidth + 1) * (meshHeight + 1)

    val origX = FloatArray(totalVerts)
    val origY = FloatArray(totalVerts)
    val currX = FloatArray(totalVerts)
    val currY = FloatArray(totalVerts)
    val vx = FloatArray(totalVerts)
    val vy = FloatArray(totalVerts)
    val weights = FloatArray(totalVerts)
    val meshVerts = FloatArray(totalVerts * 2)
    val vertexColors = IntArray(totalVerts)

    data class TouchGrab(
        val pointerId: Int,
        var screenX: Float,
        var screenY: Float,
        var startX: Float,
        var startY: Float,
        var prevX: Float,
        var prevY: Float,
        var holdTime: Float = 0f
    )

    private val activeTouches = mutableMapOf<Int, TouchGrab>()

    // Active tool & material parameters
    var activeTool: KineticTool = KineticTool.PULL
    var activePreset: MaterialPreset = MaterialPreset.SOFT_GEL

    var softness: Float = MaterialPreset.SOFT_GEL.softness
    var grabRadius: Float = 175f
    var wobbleFrequency: Float = MaterialPreset.SOFT_GEL.frequencyHz
    var dampingZeta: Float = MaterialPreset.SOFT_GEL.dampingZeta
    var couplingStrength: Float = MaterialPreset.SOFT_GEL.coupling
    var bulgeFactor: Float = MaterialPreset.SOFT_GEL.bulge
    var pulseBpm: Float = 116f

    // Visual overlays & reactive shading
    var showWireframeLattice: Boolean = false
    var showMaskHeatmap: Boolean = false
    var enableReactiveBlush: Boolean = true
    var enable3dSpecular: Boolean = true

    // External gravity tilt from accelerometer (-1..1 range)
    var tiltGravityX: Float = 0f
    var tiltGravityY: Float = 0f

    // Live telemetry
    var peakDisplacementPx: Float = 0f
        private set
    var averageStrain: Float = 0f
        private set
    val waveformHistory = FloatArray(40)
    private var waveformWriteIdx = 0
    private var simulationTimeSec = 0f

    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f
    private var isInitialized: Boolean = false

    private val latticePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun applyMaterialPreset(preset: MaterialPreset) {
        activePreset = preset
        wobbleFrequency = preset.frequencyHz
        dampingZeta = preset.dampingZeta
        softness = preset.softness
        couplingStrength = preset.coupling
        bulgeFactor = preset.bulge
    }

    fun setupMesh(width: Float, height: Float, maskBitmap: Bitmap? = null) {
        if (width <= 0f || height <= 0f) return
        if (isInitialized && abs(canvasWidth - width) < 1f && abs(canvasHeight - height) < 1f) {
            return
        }

        val hadWeights = isInitialized
        canvasWidth = width
        canvasHeight = height

        var index = 0
        for (y in 0..meshHeight) {
            val fy = y.toFloat() / meshHeight
            val py = fy * height
            for (x in 0..meshWidth) {
                val fx = x.toFloat() / meshWidth
                val px = fx * width

                origX[index] = px
                origY[index] = py
                currX[index] = px
                currY[index] = py
                vx[index] = 0f
                vy[index] = 0f
                vertexColors[index] = Color.WHITE

                if (!hadWeights) {
                    if (maskBitmap != null && !maskBitmap.isRecycled) {
                        val mx = (fx * (maskBitmap.width - 1)).toInt().coerceIn(0, maskBitmap.width - 1)
                        val my = (fy * (maskBitmap.height - 1)).toInt().coerceIn(0, maskBitmap.height - 1)
                        val pixel = maskBitmap.getPixel(mx, my)
                        val alpha = (pixel ushr 24) and 0xFF
                        val green = (pixel ushr 8) and 0xFF
                        weights[index] = if (alpha > 20) (green / 255f).coerceAtLeast(alpha / 255f) else 0f
                    } else {
                        weights[index] = computeTemplateWeight(fx, fy, ZoneTemplate.DUAL_CHEEKS)
                    }
                }

                meshVerts[index * 2] = px
                meshVerts[index * 2 + 1] = py
                index++
            }
        }
        isInitialized = true
    }

    fun loadWeightsFromMask(maskBitmap: Bitmap?) {
        var index = 0
        for (y in 0..meshHeight) {
            val fy = y.toFloat() / meshHeight
            for (x in 0..meshWidth) {
                val fx = x.toFloat() / meshWidth
                if (maskBitmap != null && !maskBitmap.isRecycled) {
                    val mx = (fx * (maskBitmap.width - 1)).toInt().coerceIn(0, maskBitmap.width - 1)
                    val my = (fy * (maskBitmap.height - 1)).toInt().coerceIn(0, maskBitmap.height - 1)
                    val pixel = maskBitmap.getPixel(mx, my)
                    val alpha = (pixel ushr 24) and 0xFF
                    val green = (pixel ushr 8) and 0xFF
                    weights[index] = if (alpha > 20) (green / 255f).coerceAtLeast(alpha / 255f) else 0f
                } else {
                    weights[index] = computeTemplateWeight(fx, fy, ZoneTemplate.DUAL_CHEEKS)
                }
                index++
            }
        }
    }

    fun applyZoneTemplate(template: ZoneTemplate) {
        var index = 0
        for (y in 0..meshHeight) {
            val fy = y.toFloat() / meshHeight
            for (x in 0..meshWidth) {
                val fx = x.toFloat() / meshWidth
                weights[index] = computeTemplateWeight(fx, fy, template)
                index++
            }
        }
    }

    private fun computeTemplateWeight(fx: Float, fy: Float, template: ZoneTemplate): Float {
        // Keep outermost boundary gently anchored
        val edgeMargin = min(min(fx, 1f - fx), min(fy, 1f - fy))
        val anchorScale = (edgeMargin / 0.08f).coerceIn(0.15f, 1f)

        val raw = when (template) {
            ZoneTemplate.DUAL_CHEEKS -> {
                val dL = hypot(fx - 0.31f, fy - 0.57f)
                val dR = hypot(fx - 0.69f, fy - 0.57f)
                val wL = (1f - (dL / 0.21f).pow(2)).coerceIn(0f, 1f)
                val wR = (1f - (dR / 0.21f).pow(2)).coerceIn(0f, 1f)
                max(wL, wR)
            }
            ZoneTemplate.CENTER_CORE -> {
                val d = hypot(fx - 0.50f, fy - 0.52f)
                (1f - (d / 0.34f).pow(2)).coerceIn(0f, 1f)
            }
            ZoneTemplate.UPPER_CREST -> {
                val dL = hypot(fx - 0.28f, fy - 0.26f)
                val dR = hypot(fx - 0.72f, fy - 0.26f)
                val wL = (1f - (dL / 0.22f).pow(2)).coerceIn(0f, 1f)
                val wR = (1f - (dR / 0.22f).pow(2)).coerceIn(0f, 1f)
                max(wL, wR)
            }
            ZoneTemplate.FULL_BODY -> 1.0f
            ZoneTemplate.CLEAR -> 0.0f
        }
        return (raw * anchorScale).coerceIn(0f, 1f)
    }

    /**
     * Direct in-place sculpting on the mesh weights with optional X-axis symmetry.
     */
    fun paintWeightAtScreen(
        screenX: Float,
        screenY: Float,
        brushRadiusPx: Float,
        isEraser: Boolean,
        mirrorSymmetryX: Boolean,
        softFalloff: Boolean
    ) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return
        applyBrushStroke(screenX, screenY, brushRadiusPx, isEraser, softFalloff)
        if (mirrorSymmetryX) {
            val mirroredX = canvasWidth - screenX
            applyBrushStroke(mirroredX, screenY, brushRadiusPx, isEraser, softFalloff)
        }
    }

    private fun applyBrushStroke(
        cx: Float,
        cy: Float,
        radiusPx: Float,
        isEraser: Boolean,
        softFalloff: Boolean
    ) {
        val r = radiusPx.coerceAtLeast(12f)
        for (i in 0 until totalVerts) {
            val dist = hypot(origX[i] - cx, origY[i] - cy)
            if (dist <= r) {
                val norm = dist / r
                val delta = if (softFalloff) (1f - norm * norm).pow(1.5f) * 0.45f else 0.85f
                if (isEraser) {
                    weights[i] = (weights[i] - delta).coerceIn(0f, 1f)
                } else {
                    weights[i] = (weights[i] + delta).coerceIn(0f, 1f)
                }
            }
        }
    }

    fun onPointerDown(pointerId: Int, x: Float, y: Float): Boolean {
        activeTouches[pointerId] = TouchGrab(pointerId, x, y, x, y, x, y, 0f)
        val currentRadius = grabRadius * 1.35f

        if (activeTool == KineticTool.POKE) {
            // Instant radial poke impulse outward from tap point
            for (i in 0 until totalVerts) {
                val w = weights[i]
                if (w <= 0.02f) continue
                val dx = origX[i] - x
                val dy = origY[i] - y
                val dist = hypot(dx, dy)
                if (dist < currentRadius && dist > 0.5f) {
                    val falloff = (1f - dist / currentRadius).pow(1.6f) * w * softness
                    val pokeImpulse = 580f * falloff
                    vx[i] += (dx / dist) * pokeImpulse
                    vy[i] += (dy / dist) * pokeImpulse
                }
            }
        }

        for (i in 0 until totalVerts) {
            if (weights[i] > 0.04f && hypot(origX[i] - x, origY[i] - y) < currentRadius) {
                return true
            }
        }
        return false
    }

    fun onPointerMove(pointerId: Int, x: Float, y: Float) {
        activeTouches[pointerId]?.let {
            it.prevX = it.screenX
            it.prevY = it.screenY
            it.screenX = x
            it.screenY = y
        }
    }

    fun onPointerUp(pointerId: Int): Float {
        val grab = activeTouches.remove(pointerId) ?: return 0f
        val dist = hypot(grab.screenX - grab.startX, grab.screenY - grab.startY)
        val currentRadius = grabRadius * (0.85f + softness * 0.4f)

        // Convert elastic potential energy into high-velocity spring snap recoil
        for (i in 0 until totalVerts) {
            val w = weights[i]
            if (w <= 0.02f) continue
            val d = hypot(origX[i] - grab.startX, origY[i] - grab.startY)
            if (d < currentRadius * 1.15f) {
                val falloff = (1f - (d / (currentRadius * 1.15f))).coerceIn(0f, 1f) * w
                val dirX = origX[i] - currX[i]
                val dirY = origY[i] - currY[i]
                val mag = hypot(dirX, dirY)
                if (mag > 0.5f) {
                    val recoilSpeed = 480f * (mag / currentRadius).coerceIn(0.15f, 1.8f) * falloff
                    vx[i] += (dirX / mag) * recoilSpeed
                    vy[i] += (dirY / mag) * recoilSpeed
                }
            }
        }
        return if (activeTool == KineticTool.POKE) max(dist, 45f) else dist
    }

    /**
     * Triggers an immediate kinetic jiggle across all masked regions (e.g. for preview or test tap).
     */
    fun triggerImpulseWave(angleRad: Float = 0.6f, strength: Float = 420f) {
        val ix = cos(angleRad) * strength
        val iy = sin(angleRad) * strength
        for (i in 0 until totalVerts) {
            val w = weights[i]
            if (w > 0.02f) {
                vx[i] += ix * w * softness
                vy[i] += iy * w * softness
            }
        }
    }

    fun resetToResting() {
        activeTouches.clear()
        for (i in 0 until totalVerts) {
            currX[i] = origX[i]
            currY[i] = origY[i]
            vx[i] = 0f
            vy[i] = 0f
        }
    }

    fun updatePhysics(deltaSeconds: Float) {
        if (!isInitialized) return

        val dt = deltaSeconds.coerceIn(0.001f, 0.04f)
        simulationTimeSec += dt
        val subSteps = 4
        val subDt = dt / subSteps

        val omega = 2f * Math.PI.toFloat() * wobbleFrequency
        val omegaSq = omega * omega
        val twoZetaOmega = 2f * dampingZeta * omega
        val currentRadius = grabRadius * (0.85f + softness * 0.45f)

        // Pulse tool rhythmic force
        val pulseFreqRad = (pulseBpm / 60f) * 2f * Math.PI.toFloat()
        val pulseEnvelope = if (activeTool == KineticTool.PULSE) {
            sin(simulationTimeSec * pulseFreqRad)
        } else 0f

        for (step in 0 until subSteps) {
            // 1. Apply active touch tool deformations
            for ((_, grab) in activeTouches) {
                grab.holdTime += subDt
                val dragDx = grab.screenX - grab.startX
                val dragDy = grab.screenY - grab.startY
                val dragMag = hypot(dragDx, dragDy)
                val maxStretch = currentRadius * 2.5f * softness
                val clampedDx = if (dragMag > maxStretch && dragMag > 0.001f) (dragDx / dragMag) * maxStretch else dragDx
                val clampedDy = if (dragMag > maxStretch && dragMag > 0.001f) (dragDy / dragMag) * maxStretch else dragDy

                // Normalized drag axis for perpendicular squash-and-stretch bulge
                val axisX = if (dragMag > 1f) dragDx / dragMag else 1f
                val axisY = if (dragMag > 1f) dragDy / dragMag else 0f
                val perpX = -axisY
                val perpY = axisX

                for (i in 0 until totalVerts) {
                    val w = weights[i]
                    if (w <= 0.01f) continue

                    val relX = origX[i] - grab.startX
                    val relY = origY[i] - grab.startY
                    val dist = hypot(relX, relY)
                    if (dist >= currentRadius) continue

                    val normDist = dist / currentRadius
                    val falloff = (1f - normDist * normDist).pow(2) * w * softness

                    when (activeTool) {
                        KineticTool.PULL, KineticTool.PULSE, KineticTool.GYRO -> {
                            // Squash & stretch bulge perpendicular to pull vector
                            val perpProj = relX * perpX + relY * perpY
                            val stretchRatio = (dragMag / currentRadius).coerceIn(0f, 1.4f)
                            val bulgeOffset = perpProj * bulgeFactor * stretchRatio * falloff

                            val targetX = origX[i] + clampedDx * falloff + perpX * bulgeOffset
                            val targetY = origY[i] + clampedDy * falloff + perpY * bulgeOffset

                            val prevX = currX[i]
                            val prevY = currY[i]
                            val pullStiffness = 0.62f
                            currX[i] += (targetX - currX[i]) * pullStiffness
                            currY[i] += (targetY - currY[i]) * pullStiffness
                            vx[i] = (currX[i] - prevX) / subDt * 0.45f
                            vy[i] = (currY[i] - prevY) / subDt * 0.45f
                        }

                        KineticTool.POKE -> {
                            // Push vertices radially outward from current finger position
                            val dxFromFinger = origX[i] - grab.screenX
                            val dyFromFinger = origY[i] - grab.screenY
                            val dFinger = hypot(dxFromFinger, dyFromFinger)
                            if (dFinger < currentRadius && dFinger > 0.5f) {
                                val pokeFalloff = (1f - dFinger / currentRadius).pow(1.5f) * w * softness
                                val pushDist = currentRadius * 0.48f * pokeFalloff
                                val targetX = origX[i] + (dxFromFinger / dFinger) * pushDist
                                val targetY = origY[i] + (dyFromFinger / dFinger) * pushDist
                                currX[i] += (targetX - currX[i]) * 0.5f
                                currY[i] += (targetY - currY[i]) * 0.5f
                            }
                        }

                        KineticTool.SWIRL -> {
                            // Rotational twist around grab start anchor
                            val swirlAngle = ((dragDx - dragDy) / currentRadius).coerceIn(-2.2f, 2.2f) * falloff * 1.4f
                            val cosA = cos(swirlAngle)
                            val sinA = sin(swirlAngle)
                            val rotX = relX * cosA - relY * sinA
                            val rotY = relX * sinA + relY * cosA
                            val targetX = grab.startX + rotX
                            val targetY = grab.startY + rotY

                            val prevX = currX[i]
                            val prevY = currY[i]
                            currX[i] += (targetX - currX[i]) * 0.55f
                            currY[i] += (targetY - currY[i]) * 0.55f
                            vx[i] = (currX[i] - prevX) / subDt * 0.4f
                            vy[i] = (currY[i] - prevY) / subDt * 0.4f
                        }
                    }
                }
            }

            // 2. Damped harmonic oscillator for all free vertices
            val cxCenter = canvasWidth * 0.5f
            val cyCenter = canvasHeight * 0.55f

            for (i in 0 until totalVerts) {
                val w = weights[i]
                if (w <= 0.001f) {
                    currX[i] = origX[i]
                    currY[i] = origY[i]
                    vx[i] = 0f
                    vy[i] = 0f
                    continue
                }

                var isPinnedByFinger = false
                if (activeTool != KineticTool.POKE) {
                    for ((_, grab) in activeTouches) {
                        if (hypot(origX[i] - grab.startX, origY[i] - grab.startY) < currentRadius * 0.68f) {
                            isPinnedByFinger = true
                            break
                        }
                    }
                }
                if (isPinnedByFinger) continue

                val dispX = currX[i] - origX[i]
                val dispY = currY[i] - origY[i]

                var ax = -omegaSq * dispX - twoZetaOmega * vx[i]
                var ay = -omegaSq * dispY - twoZetaOmega * vy[i]

                // Apply continuous rhythmic pulse force in PULSE mode
                if (activeTool == KineticTool.PULSE) {
                    val rx = origX[i] - cxCenter
                    val ry = origY[i] - cyCenter
                    val rMag = hypot(rx, ry).coerceAtLeast(1f)
                    val pulseForce = pulseEnvelope * 1450f * w * softness
                    ax += (rx / rMag) * pulseForce
                    ay += (ry / rMag) * pulseForce + pulseEnvelope * 650f * w
                }

                // Apply device tilt gravity force
                if (activeTool == KineticTool.GYRO || abs(tiltGravityX) > 0.05f || abs(tiltGravityY) > 0.05f) {
                    val gravScale = if (activeTool == KineticTool.GYRO) 2400f else 850f
                    ax += tiltGravityX * gravScale * w * softness
                    ay += tiltGravityY * gravScale * w * softness
                }

                vx[i] += ax * subDt
                vy[i] += ay * subDt

                currX[i] += vx[i] * subDt
                currY[i] += vy[i] * subDt
            }

            // 3. Viscoelastic neighbor coupling
            applyNeighborCoupling()
        }

        // 4. Pack mesh vertices & compute live telemetry
        var maxDisp = 0f
        var strainSum = 0f
        var activeWeightCount = 0
        var vIdx = 0
        for (i in 0 until totalVerts) {
            val cx = currX[i]
            val cy = currY[i]
            meshVerts[vIdx++] = cx
            meshVerts[vIdx++] = cy

            val d = hypot(cx - origX[i], cy - origY[i])
            if (d > maxDisp) maxDisp = d
            if (weights[i] > 0.05f) {
                strainSum += d
                activeWeightCount++
            }
        }

        peakDisplacementPx = maxDisp
        averageStrain = if (activeWeightCount > 0) strainSum / activeWeightCount else 0f
        waveformHistory[waveformWriteIdx] = (maxDisp / (grabRadius * 0.9f)).coerceIn(0f, 1f)
        waveformWriteIdx = (waveformWriteIdx + 1) % waveformHistory.size
    }

    private fun applyNeighborCoupling() {
        val stride = meshWidth + 1
        val c = couplingStrength * softness

        for (y in 1 until meshHeight) {
            for (x in 1 until meshWidth) {
                val idx = y * stride + x
                val w = weights[idx]
                if (w <= 0.02f) continue

                val left = idx - 1
                val right = idx + 1
                val up = idx - stride
                val down = idx + stride

                val avgDispX = ((currX[left] - origX[left]) +
                        (currX[right] - origX[right]) +
                        (currX[up] - origX[up]) +
                        (currX[down] - origX[down])) * 0.25f

                val avgDispY = ((currY[left] - origY[left]) +
                        (currY[right] - origY[right]) +
                        (currY[up] - origY[up]) +
                        (currY[down] - origY[down])) * 0.25f

                val myDispX = currX[idx] - origX[idx]
                val myDispY = currY[idx] - origY[idx]

                currX[idx] += (avgDispX - myDispX) * c * w
                currY[idx] += (avgDispY - myDispY) * c * w
            }
        }
    }

    /**
     * Renders the deformed bitmap mesh + reactive strain blush + optional technical lattice.
     */
    fun drawMesh(canvas: Canvas, bitmap: Bitmap, paint: Paint) {
        if (!isInitialized || bitmap.isRecycled) return

        // 1. Hardware-accelerated deformable bitmap mesh
        canvas.drawBitmapMesh(
            bitmap,
            meshWidth,
            meshHeight,
            meshVerts,
            0,
            null,
            0,
            paint
        )

        // 2. Reactive Strain Blush & 3D Specular Highlight on stretched regions
        if (enableReactiveBlush || enable3dSpecular) {
            drawReactiveSurfaceShading(canvas)
        }

        // 3. Sculpt Mask Heatmap overlay (when in Sculpt Zone mode)
        if (showMaskHeatmap) {
            drawMaskHeatmapOverlay(canvas)
        }

        // 4. Precision Tension Wireframe Lattice (when toggled)
        if (showWireframeLattice) {
            drawTensionLatticeOverlay(canvas)
        }
    }

    private fun drawReactiveSurfaceShading(canvas: Canvas) {
        val strainRatio = (peakDisplacementPx / (grabRadius * 0.75f)).coerceIn(0f, 1f)
        if (strainRatio < 0.04f) return

        // Find centroid of maximum displacement
        var weightedX = 0f
        var weightedY = 0f
        var totalDisp = 0f
        for (i in 0 until totalVerts) {
            val d = hypot(currX[i] - origX[i], currY[i] - origY[i])
            if (d > 4f) {
                weightedX += currX[i] * d
                weightedY += currY[i] * d
                totalDisp += d
            }
        }
        if (totalDisp <= 1f) return

        val centerX = weightedX / totalDisp
        val centerY = weightedY / totalDisp
        val radius = grabRadius * 0.65f

        if (enableReactiveBlush) {
            val alpha = (strainRatio * 80).toInt().coerceIn(0, 90)
            blushPaint.shader = null
            blushPaint.color = Color.argb(alpha, 251, 113, 133)
            canvas.drawCircle(centerX, centerY, radius * 0.65f, blushPaint)
        }

        if (enable3dSpecular) {
            val specAlpha = (strainRatio * 65).toInt().coerceIn(0, 75)
            val specRadius = radius * 0.32f
            blushPaint.shader = null
            blushPaint.color = Color.argb(specAlpha, 255, 255, 255)
            canvas.drawCircle(centerX - specRadius * 0.35f, centerY - specRadius * 0.35f, specRadius, blushPaint)
        }
    }

    private fun drawMaskHeatmapOverlay(canvas: Canvas) {
        val cellRadius = (min(canvasWidth, canvasHeight) / meshWidth) * 0.52f
        for (i in 0 until totalVerts) {
            val w = weights[i]
            if (w <= 0.03f) continue
            val alpha = (w * 155).toInt().coerceIn(20, 165)
            // Studio Cobalt (#38BDF8) heatmap nodes
            nodePaint.color = Color.argb(alpha, 56, 189, 248)
            canvas.drawCircle(currX[i], currY[i], cellRadius * (0.5f + w * 0.5f), nodePaint)
        }
    }

    private fun drawTensionLatticeOverlay(canvas: Canvas) {
        val stride = meshWidth + 1
        // Draw every 2nd grid line for crisp technical readability
        for (y in 0..meshHeight step 2) {
            for (x in 0..meshWidth step 2) {
                val idx = y * stride + x
                val cx = currX[idx]
                val cy = currY[idx]
                val disp = hypot(cx - origX[idx], cy - origY[idx])
                val strain = (disp / 60f).coerceIn(0f, 1f)

                // Interpolate from muted slate (#7A8296) to bright cobalt (#38BDF8)
                val r = (122 + (56 - 122) * strain).toInt()
                val g = (130 + (189 - 130) * strain).toInt()
                val b = (150 + (248 - 150) * strain).toInt()
                val alpha = (75 + 165 * max(strain, weights[idx] * 0.5f)).toInt().coerceIn(60, 240)
                latticePaint.color = Color.argb(alpha, r, g, b)

                if (x + 2 <= meshWidth) {
                    val rightIdx = y * stride + (x + 2)
                    canvas.drawLine(cx, cy, currX[rightIdx], currY[rightIdx], latticePaint)
                }
                if (y + 2 <= meshHeight) {
                    val downIdx = (y + 2) * stride + x
                    canvas.drawLine(cx, cy, currX[downIdx], currY[downIdx], latticePaint)
                }

                // Draw displacement vector tail if displaced
                if (disp > 3f) {
                    nodePaint.color = Color.argb(220, 56, 189, 248)
                    canvas.drawCircle(cx, cy, 3.2f, nodePaint)
                }
            }
        }
    }
}
