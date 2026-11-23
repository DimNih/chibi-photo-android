package com.chibi.photo.ui.components

import android.graphics.*
import com.chibi.photo.physics.MaterialPreset
import com.chibi.photo.physics.ZoneTemplate

data class PresetCharacter(
    val id: String,
    val name: String,
    val tag: String,
    val defaultMaterial: MaterialPreset,
    val defaultZone: ZoneTemplate,
    val generator: (size: Int) -> Bitmap
)

/**
 * Pure Flat Cel-Shaded Chibi Characters (Zero gradients, crisp solid colors)
 */
object CharacterPresets {

    val all: List<PresetCharacter> = listOf(
        PresetCharacter(
            id = "astrid_cadet",
            name = "Astrid",
            tag = "Soft gel",
            defaultMaterial = MaterialPreset.SOFT_GEL,
            defaultZone = ZoneTemplate.DUAL_CHEEKS,
            generator = { size -> drawAstridCadet(size) }
        ),
        PresetCharacter(
            id = "kuro_slime",
            name = "Kuro",
            tag = "Water drop",
            defaultMaterial = MaterialPreset.WATER_DROP,
            defaultZone = ZoneTemplate.CENTER_CORE,
            generator = { size -> drawKuroSlime(size) }
        ),
        PresetCharacter(
            id = "bao_spirit",
            name = "Bao",
            tag = "Warm dough",
            defaultMaterial = MaterialPreset.WARM_DOUGH,
            defaultZone = ZoneTemplate.DUAL_CHEEKS,
            generator = { size -> drawBaoSpirit(size) }
        ),
        PresetCharacter(
            id = "noctis_owl",
            name = "Noctis",
            tag = "Latex snap",
            defaultMaterial = MaterialPreset.LATEX_SNAP,
            defaultZone = ZoneTemplate.UPPER_CREST,
            generator = { size -> drawNoctisOwl(size) }
        )
    )

    private fun drawFlatBackdrop(canvas: Canvas, s: Float, fillColor: Int) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.46f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF263554.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.46f, borderPaint)
    }

    private fun drawAstridCadet(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        drawFlatBackdrop(canvas, s, 0xFF16223B.toInt())

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A101D.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.020f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Flat back hair
        val backHairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF475569.toInt() }
        canvas.drawOval(RectF(s * 0.18f, s * 0.20f, s * 0.82f, s * 0.84f), backHairPaint)

        // Flat blue tactical collar
        val collarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1D4ED8.toInt() }
        val collarPath = Path().apply {
            moveTo(s * 0.28f, s * 0.86f)
            lineTo(s * 0.36f, s * 0.72f)
            lineTo(s * 0.64f, s * 0.72f)
            lineTo(s * 0.72f, s * 0.86f)
            close()
        }
        canvas.drawPath(collarPath, collarPaint)
        canvas.drawPath(collarPath, linePaint)

        // Flat cel-shaded peach face
        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE9DC.toInt() }
        val facePath = Path().apply {
            moveTo(s * 0.24f, s * 0.45f)
            cubicTo(s * 0.22f, s * 0.30f, s * 0.78f, s * 0.30f, s * 0.76f, s * 0.45f)
            cubicTo(s * 0.83f, s * 0.64f, s * 0.72f, s * 0.78f, s * 0.50f, s * 0.78f)
            cubicTo(s * 0.28f, s * 0.78f, s * 0.17f, s * 0.64f, s * 0.24f, s * 0.45f)
            close()
        }
        canvas.drawPath(facePath, skinPaint)
        canvas.drawPath(facePath, linePaint)

        // Flat solid blush circles
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8FB7185.toInt() }
        canvas.drawOval(RectF(s * 0.22f, s * 0.57f, s * 0.38f, s * 0.67f), blushPaint)
        canvas.drawOval(RectF(s * 0.62f, s * 0.57f, s * 0.78f, s * 0.67f), blushPaint)

        // Flat chibi eyes
        val eyeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A101D.toInt() }
        val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() }
        val irisLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF60A5FA.toInt() }
        val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        canvas.drawOval(RectF(s * 0.33f, s * 0.47f, s * 0.43f, s * 0.59f), eyeBg)
        canvas.drawOval(RectF(s * 0.57f, s * 0.47f, s * 0.67f, s * 0.59f), eyeBg)

        canvas.drawOval(RectF(s * 0.345f, s * 0.50f, s * 0.415f, s * 0.58f), irisPaint)
        canvas.drawOval(RectF(s * 0.585f, s * 0.50f, s * 0.655f, s * 0.58f), irisPaint)

        canvas.drawOval(RectF(s * 0.355f, s * 0.535f, s * 0.405f, s * 0.575f), irisLight)
        canvas.drawOval(RectF(s * 0.595f, s * 0.535f, s * 0.645f, s * 0.575f), irisLight)

        canvas.drawCircle(s * 0.36f, s * 0.50f, s * 0.022f, specPaint)
        canvas.drawCircle(s * 0.60f, s * 0.50f, s * 0.022f, specPaint)

        // Cat smile
        val mouthPath = Path().apply {
            moveTo(s * 0.455f, s * 0.62f)
            quadTo(s * 0.48f, s * 0.65f, s * 0.50f, s * 0.625f)
            quadTo(s * 0.52f, s * 0.65f, s * 0.545f, s * 0.62f)
        }
        canvas.drawPath(mouthPath, linePaint)

        // Flat silver-blue bangs
        val frontHairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt() }
        val bangsPath = Path().apply {
            moveTo(s * 0.21f, s * 0.48f)
            lineTo(s * 0.31f, s * 0.41f)
            lineTo(s * 0.37f, s * 0.48f)
            lineTo(s * 0.48f, s * 0.39f)
            lineTo(s * 0.56f, s * 0.48f)
            lineTo(s * 0.67f, s * 0.40f)
            lineTo(s * 0.79f, s * 0.48f)
            cubicTo(s * 0.80f, s * 0.22f, s * 0.20f, s * 0.22f, s * 0.21f, s * 0.48f)
            close()
        }
        canvas.drawPath(bangsPath, frontHairPaint)
        canvas.drawPath(bangsPath, linePaint)

        // Solid Royal Blue Visor
        val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() }
        val visorRect = RectF(s * 0.27f, s * 0.25f, s * 0.73f, s * 0.37f)
        canvas.drawRoundRect(visorRect, s * 0.05f, s * 0.05f, visorPaint)
        canvas.drawRoundRect(visorRect, s * 0.05f, s * 0.05f, linePaint)

        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.015f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(s * 0.33f, s * 0.29f, s * 0.50f, s * 0.29f, glossPaint)

        return bmp
    }

    private fun drawKuroSlime(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        drawFlatBackdrop(canvas, s, 0xFF132038.toInt())

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() }
        val innerHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B82F6.toInt() }
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A101D.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.020f
        }

        val slimePath = Path().apply {
            moveTo(s * 0.50f, s * 0.21f)
            cubicTo(s * 0.72f, s * 0.21f, s * 0.84f, s * 0.46f, s * 0.82f, s * 0.64f)
            cubicTo(s * 0.80f, s * 0.77f, s * 0.66f, s * 0.79f, s * 0.50f, s * 0.79f)
            cubicTo(s * 0.34f, s * 0.79f, s * 0.20f, s * 0.77f, s * 0.18f, s * 0.64f)
            cubicTo(s * 0.16f, s * 0.46f, s * 0.28f, s * 0.21f, s * 0.50f, s * 0.21f)
            close()
        }
        canvas.drawPath(slimePath, bodyPaint)
        canvas.drawOval(RectF(s * 0.24f, s * 0.27f, s * 0.76f, s * 0.72f), innerHighlight)
        canvas.drawPath(slimePath, outlinePaint)

        val jellyGloss = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawOval(RectF(s * 0.29f, s * 0.31f, s * 0.41f, s * 0.37f), jellyGloss)

        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFACC15.toInt() }
        canvas.drawOval(RectF(s * 0.36f, s * 0.50f, s * 0.43f, s * 0.59f), eyePaint)
        canvas.drawOval(RectF(s * 0.57f, s * 0.50f, s * 0.64f, s * 0.59f), eyePaint)

        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF93C5FD.toInt() }
        canvas.drawOval(RectF(s * 0.25f, s * 0.58f, s * 0.35f, s * 0.65f), cheekPaint)
        canvas.drawOval(RectF(s * 0.65f, s * 0.58f, s * 0.75f, s * 0.65f), cheekPaint)

        return bmp
    }

    private fun drawBaoSpirit(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        drawFlatBackdrop(canvas, s, 0xFF16223B.toInt())

        val doughPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFEF3C7.toInt() }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A101D.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.020f
            strokeCap = Paint.Cap.ROUND
        }

        val bunPath = Path().apply {
            moveTo(s * 0.50f, s * 0.24f)
            cubicTo(s * 0.76f, s * 0.26f, s * 0.86f, s * 0.50f, s * 0.82f, s * 0.68f)
            cubicTo(s * 0.78f, s * 0.78f, s * 0.64f, s * 0.78f, s * 0.50f, s * 0.78f)
            cubicTo(s * 0.36f, s * 0.78f, s * 0.22f, s * 0.78f, s * 0.18f, s * 0.68f)
            cubicTo(s * 0.14f, s * 0.50f, s * 0.24f, s * 0.26f, s * 0.50f, s * 0.24f)
            close()
        }
        canvas.drawPath(bunPath, doughPaint)
        canvas.drawPath(bunPath, linePaint)

        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() }
        canvas.drawCircle(s * 0.50f, s * 0.40f, s * 0.025f, sealPaint)

        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8FB7185.toInt() }
        canvas.drawCircle(s * 0.29f, s * 0.60f, s * 0.082f, cheekPaint)
        canvas.drawCircle(s * 0.71f, s * 0.60f, s * 0.082f, cheekPaint)

        val eyeL = Path().apply {
            moveTo(s * 0.34f, s * 0.53f)
            quadTo(s * 0.39f, s * 0.47f, s * 0.44f, s * 0.53f)
        }
        val eyeR = Path().apply {
            moveTo(s * 0.56f, s * 0.53f)
            quadTo(s * 0.61f, s * 0.47f, s * 0.66f, s * 0.53f)
        }
        canvas.drawPath(eyeL, linePaint)
        canvas.drawPath(eyeR, linePaint)

        val mouth = Path().apply {
            moveTo(s * 0.47f, s * 0.58f)
            quadTo(s * 0.50f, s * 0.62f, s * 0.53f, s * 0.58f)
        }
        canvas.drawPath(mouth, linePaint)

        return bmp
    }

    private fun drawNoctisOwl(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        drawFlatBackdrop(canvas, s, 0xFF132038.toInt())

        val plumagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334155.toInt() }
        val bellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE2E8F0.toInt() }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A101D.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.020f
            strokeCap = Paint.Cap.ROUND
        }

        val crestL = Path().apply {
            moveTo(s * 0.25f, s * 0.38f)
            lineTo(s * 0.17f, s * 0.18f)
            lineTo(s * 0.39f, s * 0.28f)
            close()
        }
        val crestR = Path().apply {
            moveTo(s * 0.61f, s * 0.28f)
            lineTo(s * 0.83f, s * 0.18f)
            lineTo(s * 0.75f, s * 0.38f)
            close()
        }
        canvas.drawPath(crestL, plumagePaint); canvas.drawPath(crestL, linePaint)
        canvas.drawPath(crestR, plumagePaint); canvas.drawPath(crestR, linePaint)

        val bodyRect = RectF(s * 0.20f, s * 0.26f, s * 0.80f, s * 0.80f)
        canvas.drawOval(bodyRect, plumagePaint)
        canvas.drawOval(bodyRect, linePaint)

        canvas.drawCircle(s * 0.38f, s * 0.48f, s * 0.13f, bellyPaint)
        canvas.drawCircle(s * 0.62f, s * 0.48f, s * 0.13f, bellyPaint)
        canvas.drawOval(RectF(s * 0.28f, s * 0.56f, s * 0.72f, s * 0.79f), bellyPaint)

        val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B82F6.toInt() }
        val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A101D.toInt() }

        canvas.drawCircle(s * 0.38f, s * 0.48f, s * 0.075f, irisPaint)
        canvas.drawCircle(s * 0.62f, s * 0.48f, s * 0.075f, irisPaint)
        canvas.drawCircle(s * 0.38f, s * 0.48f, s * 0.042f, pupilPaint)
        canvas.drawCircle(s * 0.62f, s * 0.48f, s * 0.042f, pupilPaint)

        val beakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        val beak = Path().apply {
            moveTo(s * 0.50f, s * 0.50f)
            lineTo(s * 0.535f, s * 0.55f)
            lineTo(s * 0.50f, s * 0.60f)
            lineTo(s * 0.465f, s * 0.55f)
            close()
        }
        canvas.drawPath(beak, beakPaint)

        return bmp
    }
}
