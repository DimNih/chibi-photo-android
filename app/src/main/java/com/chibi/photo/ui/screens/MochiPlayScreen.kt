package com.chibi.photo.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chibi.photo.audio.ProceduralAudioEngine
import com.chibi.photo.audio.SoundProfile
import com.chibi.photo.export.ExportManager
import com.chibi.photo.export.GifEncoder
import com.chibi.photo.haptics.HapticManager
import com.chibi.photo.physics.*
import com.chibi.photo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioTab(val label: String) {
    TOUCH("Touch"),
    MASK("Mask"),
    BOUNCE("Bounce"),
    SOUND("Sound")
}

@Composable
fun MochiPlayScreen(
    sourceBitmap: Bitmap,
    specimenTitle: String,
    initialMaterial: MaterialPreset,
    initialZone: ZoneTemplate,
    onBackToStudio: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val engine = remember(sourceBitmap) {
        MochiMeshEngine(meshWidth = 32, meshHeight = 32).apply {
            applyMaterialPreset(initialMaterial)
            applyZoneTemplate(initialZone)
        }
    }
    val audio = remember { ProceduralAudioEngine() }
    val haptics = remember { HapticManager(context) }
    val exportManager = remember { ExportManager(context) }

    var activeTab by remember { mutableStateOf(StudioTab.TOUCH) }
    var activeTool by remember { mutableStateOf(KineticTool.PULL) }
    var activeMaterial by remember { mutableStateOf(initialMaterial) }

    var isSculptEraser by remember { mutableStateOf(false) }
    var mirrorSymmetryX by remember { mutableStateOf(true) }
    var sculptBrushRadius by remember { mutableFloatStateOf(72f) }

    var freqState by remember { mutableFloatStateOf(initialMaterial.frequencyHz) }
    var zetaState by remember { mutableFloatStateOf(initialMaterial.dampingZeta) }
    var softnessState by remember { mutableFloatStateOf(initialMaterial.softness) }
    var pulseBpmState by remember { mutableFloatStateOf(116f) }

    var reactiveBlush by remember { mutableStateOf(true) }
    var wireframeLattice by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var hapticEnabled by remember { mutableStateOf(true) }
    var soundProfile by remember { mutableStateOf(SoundProfile.TACTILE_STUDIO) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableFloatStateOf(0f) }
    val recordedFrames = remember { mutableListOf<Bitmap>() }
    var isEncodingGif by remember { mutableStateOf(false) }
    var exportResultUri by remember { mutableStateOf<Uri?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) {
        engine.showMaskHeatmap = (activeTab == StudioTab.MASK)
    }

    DisposableEffect(Unit) {
        val tiltManager = TiltSensorManager(context) { gx, gy, shakeImpulse ->
            engine.tiltGravityX = gx
            engine.tiltGravityY = gy
            if (shakeImpulse > 80f) {
                engine.triggerImpulseWave(0.8f, shakeImpulse)
            }
        }
        tiltManager.start()
        onDispose { tiltManager.stop() }
    }

    var frameTicker by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastNano) / 1_000_000_000f
                lastNano = now
                engine.updatePhysics(dt)
                frameTicker = now
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordedFrames.clear()
            recordingSeconds = 0f
            val startMs = System.currentTimeMillis()
            while (isRecording && recordingSeconds < 5.0f) {
                delay(40)
                recordingSeconds = (System.currentTimeMillis() - startMs) / 1000f
            }
            if (isRecording) isRecording = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Clean Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToStudio,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = specimenTitle,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // Snapshot Button
            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .clickable {
                        scope.launch {
                            val snap = Bitmap.createBitmap(
                                sourceBitmap.width,
                                sourceBitmap.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val c = android.graphics.Canvas(snap)
                            engine.drawMesh(c, sourceBitmap, Paint(Paint.FILTER_BITMAP_FLAG))
                            val uri = exportManager.saveSnapshotToGallery(snap, "chibi_photo")
                            if (uri != null) {
                                Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
                                exportResultUri = uri
                                showExportDialog = true
                            }
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                color = BlueRaised,
                border = BorderStroke(1.dp, BlueHairline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Photo",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Photo",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Solid Blue GIF Record Button
            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .clickable {
                        if (!isRecording) {
                            isRecording = true
                        } else {
                            isRecording = false
                            scope.launch {
                                isEncodingGif = true
                                val gifEncoder = GifEncoder()
                                val targetW = 360
                                val targetH = (360f * (sourceBitmap.height.toFloat() / sourceBitmap.width)).toInt()
                                gifEncoder.start(targetW, targetH, delay = 40)
                                for (f in recordedFrames) {
                                    gifEncoder.addFrame(f)
                                }
                                val gifBytes = withContext(Dispatchers.Default) { gifEncoder.finish() }
                                val uri = exportManager.saveGifToGallery(gifBytes, "chibi_gif")
                                isEncodingGif = false
                                if (uri != null) {
                                    exportResultUri = uri
                                    showExportDialog = true
                                }
                            }
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                color = if (isRecording) SemanticRed else PrimaryBlue
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "${String.format("%.1f", recordingSeconds)}s" else "Save GIF",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        // 2. Central 1:1 Interactive Canvas Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BlueDeepest)
                .border(1.dp, BlueHairline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val meshPaint = remember {
                Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    isAntiAlias = true
                    isDither = true
                }
            }

            val aspectRatio = remember(sourceBitmap) {
                (sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat().coerceAtLeast(1f))
                    .coerceIn(0.5f, 2.0f)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .padding(16.dp)
                    .pointerInput(sourceBitmap, activeTab, activeTool, isSculptEraser, mirrorSymmetryX, sculptBrushRadius) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                for (change in event.changes) {
                                    val pId = change.id.value.toInt()
                                    val x = change.position.x
                                    val y = change.position.y
                                    val isDown = !change.previousPressed && change.pressed
                                    val isUp = change.previousPressed && !change.pressed

                                    if (activeTab == StudioTab.MASK) {
                                        if (isDown || change.pressed) {
                                            engine.paintWeightAtScreen(
                                                screenX = x,
                                                screenY = y,
                                                brushRadiusPx = sculptBrushRadius,
                                                isEraser = isSculptEraser,
                                                mirrorSymmetryX = mirrorSymmetryX,
                                                softFalloff = true
                                            )
                                            if (isDown) haptics.tickStretch()
                                            change.consume()
                                        }
                                    } else {
                                        if (isDown) {
                                            val hit = engine.onPointerDown(pId, x, y)
                                            if (hit) {
                                                audio.playGrabSound()
                                                haptics.clickGrab()
                                            }
                                            change.consume()
                                        } else if (change.pressed) {
                                            engine.onPointerMove(pId, x, y)
                                            change.consume()
                                        } else if (isUp) {
                                            val stretchDist = engine.onPointerUp(pId)
                                            if (stretchDist > 8f) {
                                                audio.playReleaseSound(stretchDist, engine.wobbleFrequency)
                                                haptics.bounceRelease()
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (frameTicker > 0L) {}
                val w = size.width
                val h = size.height

                if (activeTab == StudioTab.MASK && mirrorSymmetryX) {
                    drawLine(
                        color = PrimaryBlueLight.copy(alpha = 0.45f),
                        start = Offset(w * 0.5f, 0f),
                        end = Offset(w * 0.5f, h),
                        strokeWidth = 1.5f
                    )
                }

                engine.setupMesh(w, h, null)

                drawIntoCanvas { canvas ->
                    engine.drawMesh(canvas.nativeCanvas, sourceBitmap, meshPaint)
                }

                if (isRecording && recordedFrames.size < 120) {
                    try {
                        val frameBmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
                        val frameCanvas = android.graphics.Canvas(frameBmp)
                        engine.drawMesh(frameCanvas, sourceBitmap, meshPaint)
                        recordedFrames.add(frameBmp)
                    } catch (_: Exception) {}
                }
            }

            // Minimal top-left live waveform & reset pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueCard)
                        .border(1.dp, BlueHairline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(width = 48.dp, height = 14.dp)) {
                        if (frameTicker > 0L) {}
                        val hist = engine.waveformHistory
                        val stepX = size.width / (hist.size - 1).coerceAtLeast(1)
                        val midY = size.height * 0.85f
                        val path = Path()
                        for (i in hist.indices) {
                            val x = i * stepX
                            val y = midY - (hist[i] * size.height * 0.75f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = PrimaryBlueLight, style = Stroke(width = 2.0f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format("%.1f", freqState)} Hz",
                        style = MonoTabularStyle,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (wireframeLattice) PrimaryBlue else BlueCard)
                            .border(1.dp, BlueHairline, RoundedCornerShape(8.dp))
                            .clickable {
                                wireframeLattice = !wireframeLattice
                                engine.showWireframeLattice = wireframeLattice
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Grid",
                            style = MonoTabularStyle,
                            color = if (wireframeLattice) TextPrimary else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BlueCard)
                            .border(1.dp, BlueHairline, RoundedCornerShape(8.dp))
                            .clickable {
                                engine.resetToResting()
                                engine.triggerImpulseWave(0.4f, 280f)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = MonoTabularStyle,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (isEncodingGif) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BlueDeepest.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlueLight, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Saving GIF...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // 3. Clean Bottom Control Dock (Concise labels, zero clutter)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = BlueCard,
            border = BorderStroke(1.dp, BlueHairline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 4 Main Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (tab in StudioTab.entries) {
                        val selected = (tab == activeTab)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrimaryBlue else BlueDeepest)
                                .border(
                                    1.dp,
                                    if (selected) PrimaryBlueLight else BlueHairline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeTab) {
                    StudioTab.TOUCH -> {
                        TouchDockPanel(
                            activeTool = activeTool,
                            pulseBpm = pulseBpmState,
                            onSelectTool = { tool ->
                                activeTool = tool
                                engine.activeTool = tool
                            },
                            onBpmChange = { bpm ->
                                pulseBpmState = bpm
                                engine.pulseBpm = bpm
                            }
                        )
                    }

                    StudioTab.MASK -> {
                        MaskDockPanel(
                            isEraser = isSculptEraser,
                            mirrorSymmetryX = mirrorSymmetryX,
                            brushRadius = sculptBrushRadius,
                            onToggleEraser = { isSculptEraser = it },
                            onToggleMirror = { mirrorSymmetryX = it },
                            onBrushRadiusChange = { sculptBrushRadius = it },
                            onApplyTemplate = { template ->
                                engine.applyZoneTemplate(template)
                                engine.triggerImpulseWave(0.5f, 240f)
                            }
                        )
                    }

                    StudioTab.BOUNCE -> {
                        BounceDockPanel(
                            activeMaterial = activeMaterial,
                            freq = freqState,
                            zeta = zetaState,
                            softness = softnessState,
                            onSelectPreset = { preset ->
                                activeMaterial = preset
                                engine.applyMaterialPreset(preset)
                                freqState = preset.frequencyHz
                                zetaState = preset.dampingZeta
                                softnessState = preset.softness
                                engine.triggerImpulseWave(0.5f, 340f)
                            },
                            onFreqChange = {
                                freqState = it
                                engine.wobbleFrequency = it
                            },
                            onZetaChange = {
                                zetaState = it
                                engine.dampingZeta = it
                            },
                            onSoftnessChange = {
                                softnessState = it
                                engine.softness = it
                            }
                        )
                    }

                    StudioTab.SOUND -> {
                        SoundOpticsDockPanel(
                            reactiveBlush = reactiveBlush,
                            soundEnabled = soundEnabled,
                            hapticEnabled = hapticEnabled,
                            soundProfile = soundProfile,
                            onToggleBlush = {
                                reactiveBlush = it
                                engine.enableReactiveBlush = it
                            },
                            onToggleSound = {
                                soundEnabled = it
                                audio.setMuted(!it)
                            },
                            onToggleHaptic = {
                                hapticEnabled = it
                                haptics.isEnabled = it
                            },
                            onSelectSoundProfile = {
                                soundProfile = it
                                audio.activeProfile = it
                                audio.playGrabSound(0.9f)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog && exportResultUri != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = "Saved to gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Share your creation or keep editing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mime = if (exportResultUri.toString().endsWith(".gif")) "image/gif" else "image/png"
                        exportManager.shareFile(exportResultUri!!, mime, "Created with Chibi Photo")
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Share", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }
            },
            containerColor = BlueCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TouchDockPanel(
    activeTool: KineticTool,
    pulseBpm: Float,
    onSelectTool: (KineticTool) -> Unit,
    onBpmChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tool in KineticTool.entries) {
                val selected = (tool == activeTool)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) PrimaryBlueContainer else BlueRaised)
                        .border(
                            1.dp,
                            if (selected) PrimaryBlueLight else BlueHairline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectTool(tool) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tool.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }

        if (activeTool == KineticTool.PULSE) {
            Spacer(modifier = Modifier.height(10.dp))
            CompactSliderRow(
                label = "Tempo",
                value = pulseBpm,
                range = 60f..180f,
                formatted = "${pulseBpm.toInt()} bpm",
                onValueChange = onBpmChange
            )
        }
    }
}

@Composable
private fun MaskDockPanel(
    isEraser: Boolean,
    mirrorSymmetryX: Boolean,
    brushRadius: Float,
    onToggleEraser: (Boolean) -> Unit,
    onToggleMirror: (Boolean) -> Unit,
    onBrushRadiusChange: (Float) -> Unit,
    onApplyTemplate: (ZoneTemplate) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BlueChipButton(
                text = "Paint",
                selected = !isEraser,
                onClick = { onToggleEraser(false) },
                modifier = Modifier.weight(1f)
            )
            BlueChipButton(
                text = "Erase",
                selected = isEraser,
                onClick = { onToggleEraser(true) },
                modifier = Modifier.weight(1f)
            )
            BlueChipButton(
                text = "Mirror",
                selected = mirrorSymmetryX,
                onClick = { onToggleMirror(!mirrorSymmetryX) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CompactSliderRow(
            label = "Size",
            value = brushRadius,
            range = 28f..150f,
            formatted = "${brushRadius.toInt()} px",
            onValueChange = onBrushRadiusChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (template in ZoneTemplate.entries) {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BlueRaised)
                        .border(1.dp, BlueHairline, RoundedCornerShape(6.dp))
                        .clickable { onApplyTemplate(template) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = template.label,
                        style = MonoTabularStyle,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BounceDockPanel(
    activeMaterial: MaterialPreset,
    freq: Float,
    zeta: Float,
    softness: Float,
    onSelectPreset: (MaterialPreset) -> Unit,
    onFreqChange: (Float) -> Unit,
    onZetaChange: (Float) -> Unit,
    onSoftnessChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (preset in MaterialPreset.entries) {
                val selected = (preset == activeMaterial)
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) PrimaryBlueContainer else BlueRaised)
                        .border(
                            1.dp,
                            if (selected) PrimaryBlueLight else BlueHairline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        CompactSliderRow(
            label = "Speed",
            value = freq,
            range = 3.0f..14.0f,
            formatted = "${String.format("%.1f", freq)} Hz",
            onValueChange = onFreqChange
        )
        CompactSliderRow(
            label = "Damping",
            value = zeta,
            range = 0.06f..0.65f,
            formatted = String.format("%.2f", zeta),
            onValueChange = onZetaChange
        )
        CompactSliderRow(
            label = "Stretch",
            value = softness,
            range = 0.35f..1.00f,
            formatted = "${(softness * 100).toInt()}%",
            onValueChange = onSoftnessChange
        )
    }
}

@Composable
private fun CompactSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatted: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(68.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = TextPrimary,
                activeTrackColor = PrimaryBlueLight,
                inactiveTrackColor = BlueRaised
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatted,
            style = MonoValueStyle,
            modifier = Modifier.width(54.dp)
        )
    }
}

@Composable
private fun SoundOpticsDockPanel(
    reactiveBlush: Boolean,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    soundProfile: SoundProfile,
    onToggleBlush: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectSoundProfile: (SoundProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BlueChipButton(
                text = "Blush",
                selected = reactiveBlush,
                onClick = { onToggleBlush(!reactiveBlush) },
                modifier = Modifier.weight(1f)
            )
            BlueChipButton(
                text = "Audio",
                selected = soundEnabled,
                onClick = { onToggleSound(!soundEnabled) },
                modifier = Modifier.weight(1f)
            )
            BlueChipButton(
                text = "Haptic",
                selected = hapticEnabled,
                onClick = { onToggleHaptic(!hapticEnabled) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (profile in SoundProfile.entries) {
                val selected = (profile == soundProfile)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) BlueRaisedActive else BlueDeepest)
                        .border(
                            1.dp,
                            if (selected) PrimaryBlueLight else BlueHairline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSoundProfile(profile) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.label,
                        style = MonoTabularStyle,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BlueChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryBlueContainer else BlueRaised)
            .border(
                1.dp,
                if (selected) PrimaryBlueLight else BlueHairline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}
