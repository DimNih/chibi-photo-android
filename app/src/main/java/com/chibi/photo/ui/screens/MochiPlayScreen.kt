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
import com.chibi.photo.ui.AppLanguage
import com.chibi.photo.ui.AppStrings
import com.chibi.photo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioTab {
    TOUCH,
    MASK,
    BOUNCE,
    SOUND
}

@Composable
fun MochiPlayScreen(
    sourceBitmap: Bitmap,
    specimenTitle: String,
    initialMaterial: MaterialPreset,
    initialZone: ZoneTemplate,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onBackToStudio: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = AppStrings.forLang(currentLanguage)

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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar: Back + Title + Language Toggle + Dark Mode Toggle + Photo + GIF
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToStudio,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = strings.back,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = specimenTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Language Toggle Pill
            Surface(
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onToggleLanguage() },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.ID) "🇮🇩 ID" else "🇬🇧 EN",
                        style = MonoTabularStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Dark/Light Mode Toggle
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onToggleDarkMode() },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (isDarkMode) strings.lightMode else strings.darkMode,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Snapshot Button
            Surface(
                modifier = Modifier
                    .height(32.dp)
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
                                Toast.makeText(context, strings.exportSuccessMessage, Toast.LENGTH_SHORT).show()
                                exportResultUri = uri
                                showExportDialog = true
                            }
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = strings.exportSnapshot,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.exportSnapshot,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Solid Flat Blue GIF Record Button
            Surface(
                modifier = Modifier
                    .height(32.dp)
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
                color = if (isRecording) SemanticRed else MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isRecording) "${String.format("%.1f", recordingSeconds)}s" else strings.exportGif,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Central Squishy Canvas Stage (Zero gradients, flat surface)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioTheme.extended.bgBackdrop)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
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

            val primaryColor = MaterialTheme.colorScheme.primary
            val symmetryLineColor = primaryColor.copy(alpha = 0.45f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .padding(14.dp)
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
                        color = symmetryLineColor,
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

            // Waveform and Controls Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Waveform Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.size(width = 44.dp, height = 12.dp)) {
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
                        drawPath(path = path, color = primaryColor, style = Stroke(width = 1.8f))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.1f", freqState)} Hz",
                        style = MonoTabularStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Grid & Reset Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (wireframeLattice)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                wireframeLattice = !wireframeLattice
                                engine.showWireframeLattice = wireframeLattice
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ID) "Kisi" else "Grid",
                            style = MonoTabularStyle,
                            color = if (wireframeLattice)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                engine.resetToResting()
                                engine.triggerImpulseWave(0.4f, 280f)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = strings.reset,
                            style = MonoTabularStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isEncodingGif) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.encodingMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Bottom Control Dock (Clean flat card, zero gradients)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 4 Studio Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (tab in StudioTab.entries) {
                        val selected = (tab == activeTab)
                        val tabLabel = when (tab) {
                            StudioTab.TOUCH -> strings.tabTouch
                            StudioTab.MASK -> strings.tabMask
                            StudioTab.BOUNCE -> strings.tabBounce
                            StudioTab.SOUND -> strings.tabSound
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (activeTab) {
                    StudioTab.TOUCH -> {
                        TouchDockPanel(
                            activeTool = activeTool,
                            pulseBpm = pulseBpmState,
                            currentLanguage = currentLanguage,
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
                            currentLanguage = currentLanguage,
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
                            currentLanguage = currentLanguage,
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
                            currentLanguage = currentLanguage,
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
                    text = strings.exportSuccessTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = strings.exportSuccessMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(strings.actionShare, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(strings.actionDone, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TouchDockPanel(
    activeTool: KineticTool,
    pulseBpm: Float,
    currentLanguage: AppLanguage,
    onSelectTool: (KineticTool) -> Unit,
    onBpmChange: (Float) -> Unit
) {
    val strings = AppStrings.forLang(currentLanguage)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tool in KineticTool.entries) {
                val selected = (tool == activeTool)
                val toolLabel = when (tool) {
                    KineticTool.PULL -> strings.toolPinch
                    KineticTool.POKE -> strings.toolPoke
                    KineticTool.SWIRL -> strings.toolSwirl
                    KineticTool.PULSE -> strings.toolPulse
                    KineticTool.GYRO -> strings.toolGravity
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectTool(tool) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = toolLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeTool == KineticTool.PULSE) {
            Spacer(modifier = Modifier.height(10.dp))
            CompactSliderRow(
                label = if (currentLanguage == AppLanguage.ID) "Tempo" else "Tempo",
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
    currentLanguage: AppLanguage,
    onToggleEraser: (Boolean) -> Unit,
    onToggleMirror: (Boolean) -> Unit,
    onBrushRadiusChange: (Float) -> Unit,
    onApplyTemplate: (ZoneTemplate) -> Unit
) {
    val strings = AppStrings.forLang(currentLanguage)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatChipButton(
                text = strings.sculptDraw,
                selected = !isEraser,
                onClick = { onToggleEraser(false) },
                modifier = Modifier.weight(1f)
            )
            FlatChipButton(
                text = strings.sculptErase,
                selected = isEraser,
                onClick = { onToggleEraser(true) },
                modifier = Modifier.weight(1f)
            )
            FlatChipButton(
                text = strings.sculptSymmetry,
                selected = mirrorSymmetryX,
                onClick = { onToggleMirror(!mirrorSymmetryX) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CompactSliderRow(
            label = strings.sculptBrushSize,
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
                val tLabel = when (template) {
                    ZoneTemplate.DUAL_CHEEKS -> strings.zoneDualCheeks
                    ZoneTemplate.CENTER_CORE -> strings.zoneCenterCore
                    ZoneTemplate.UPPER_CREST -> strings.zoneUpperCrest
                    ZoneTemplate.FULL_BODY -> strings.zoneFullCanvas
                    ZoneTemplate.CLEAR -> strings.zoneClear
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                        .clickable { onApplyTemplate(template) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tLabel,
                        style = MonoTabularStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    currentLanguage: AppLanguage,
    onSelectPreset: (MaterialPreset) -> Unit,
    onFreqChange: (Float) -> Unit,
    onZetaChange: (Float) -> Unit,
    onSoftnessChange: (Float) -> Unit
) {
    val strings = AppStrings.forLang(currentLanguage)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (preset in MaterialPreset.entries) {
                val selected = (preset == activeMaterial)
                val presetTitle = when (preset) {
                    MaterialPreset.SOFT_GEL -> strings.materialSoftGel
                    MaterialPreset.WATER_DROP -> strings.materialWaterDrop
                    MaterialPreset.LATEX_SNAP -> strings.materialLatexSnap
                    MaterialPreset.WARM_DOUGH -> strings.materialWarmDough
                    MaterialPreset.MEMORY_FOAM -> strings.materialMemoryFoam
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = presetTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        CompactSliderRow(
            label = strings.paramFrequency,
            value = freq,
            range = 3.0f..14.0f,
            formatted = "${String.format("%.1f", freq)} Hz",
            onValueChange = onFreqChange
        )
        CompactSliderRow(
            label = strings.paramDamping,
            value = zeta,
            range = 0.06f..0.65f,
            formatted = String.format("%.2f", zeta),
            onValueChange = onZetaChange
        )
        CompactSliderRow(
            label = strings.paramSoftness,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatted,
            style = MonoValueStyle,
            color = MaterialTheme.colorScheme.onSurface,
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
    currentLanguage: AppLanguage,
    onToggleBlush: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectSoundProfile: (SoundProfile) -> Unit
) {
    val strings = AppStrings.forLang(currentLanguage)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatChipButton(
                text = strings.toggleBlush,
                selected = reactiveBlush,
                onClick = { onToggleBlush(!reactiveBlush) },
                modifier = Modifier.weight(1f)
            )
            FlatChipButton(
                text = strings.soundFxToggle,
                selected = soundEnabled,
                onClick = { onToggleSound(!soundEnabled) },
                modifier = Modifier.weight(1f)
            )
            FlatChipButton(
                text = strings.hapticToggle,
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
                val profileName = when (profile) {
                    SoundProfile.TACTILE_STUDIO -> strings.soundProfileTactile
                    SoundProfile.HYDRO_GEL -> strings.soundProfileHydro
                    SoundProfile.FM_CHIME -> strings.soundProfileChime
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSoundProfile(profile) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileName,
                        style = MonoTabularStyle,
                        color = if (selected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FlatChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
