package com.chibi.photo.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chibi.photo.audio.ProceduralAudioEngine
import com.chibi.photo.physics.MaterialPreset
import com.chibi.photo.physics.MochiMeshEngine
import com.chibi.photo.physics.ZoneTemplate
import com.chibi.photo.ui.AppLanguage
import com.chibi.photo.ui.AppStrings
import com.chibi.photo.ui.components.CharacterPresets
import com.chibi.photo.ui.components.PresetCharacter
import com.chibi.photo.ui.theme.MonoTabularStyle
import com.chibi.photo.ui.theme.StudioTheme

@Composable
fun InputScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onOpenWorkbench: (
        bitmap: Bitmap,
        title: String,
        presetMaterial: MaterialPreset,
        presetZone: ZoneTemplate
    ) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val strings = AppStrings.forLang(currentLanguage)

    var activePreset by remember { mutableStateOf(CharacterPresets.all.first()) }
    val heroBitmap = remember(activePreset.id) { activePreset.generator(560) }
    val previewEngine = remember(activePreset.id) {
        MochiMeshEngine(meshWidth = 28, meshHeight = 28).apply {
            applyMaterialPreset(activePreset.defaultMaterial)
            applyZoneTemplate(activePreset.defaultZone)
        }
    }
    val previewAudio = remember { ProceduralAudioEngine() }

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activePreset.id) {
        previewEngine.triggerImpulseWave(0.55f, 360f)
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastNano) / 1_000_000_000f
                lastNano = now
                previewEngine.updatePhysics(dt)
                frameTick = now
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                onOpenWorkbench(
                    bitmap,
                    if (currentLanguage == AppLanguage.ID) "Foto Pribadi" else "Custom Photo",
                    MaterialPreset.SOFT_GEL,
                    ZoneTemplate.DUAL_CHEEKS
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // App Header: Chibi Logo + Title + Language Toggle + Dark Mode Toggle + Import
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Chibi Logo & Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                val logoThumb = remember { CharacterPresets.all.first().generator(96) }
                Image(
                    bitmap = logoThumb.asImageBitmap(),
                    contentDescription = "Chibi Photo Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Control Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

                // Dark/Light Mode Toggle Button
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

                // Import Photo Button
                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.ID) "Foto" else "Import",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Hero Preview Card (Flat Light-Blue or Dark surface, zero gradient)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activePreset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.tapToSquishHint,
                        style = MonoTabularStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioTheme.extended.bgBackdrop)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val meshPaint = remember {
                        Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }
                    }

                    Canvas(
                        modifier = Modifier
                            .size(240.dp)
                            .pointerInput(activePreset.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        for (change in event.changes) {
                                            val pid = change.id.value.toInt()
                                            val x = change.position.x
                                            val y = change.position.y
                                            val isDown = !change.previousPressed && change.pressed
                                            val isUp = change.previousPressed && !change.pressed

                                            if (isDown) {
                                                if (previewEngine.onPointerDown(pid, x, y)) {
                                                    previewAudio.playGrabSound(0.7f)
                                                }
                                                change.consume()
                                            } else if (change.pressed) {
                                                previewEngine.onPointerMove(pid, x, y)
                                                change.consume()
                                            } else if (isUp) {
                                                val dist = previewEngine.onPointerUp(pid)
                                                if (dist > 8f) {
                                                    previewAudio.playReleaseSound(dist, previewEngine.wobbleFrequency)
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        if (frameTick > 0L) {}
                        previewEngine.setupMesh(size.width, size.height, null)
                        drawIntoCanvas { canvas ->
                            previewEngine.drawMesh(canvas.nativeCanvas, heroBitmap, meshPaint)
                        }
                    }

                    // Wireframe Grid Toggle Chip
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (previewEngine.showWireframeLattice)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                            .clickable {
                                previewEngine.showWireframeLattice = !previewEngine.showWireframeLattice
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.ID) "Kisi" else "Grid",
                            style = MonoTabularStyle,
                            color = if (previewEngine.showWireframeLattice)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Solid Flat Primary Action Button (Buka Studio / Open Studio)
                Button(
                    onClick = {
                        onOpenWorkbench(
                            heroBitmap,
                            activePreset.name,
                            activePreset.defaultMaterial,
                            activePreset.defaultZone
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.openStudio,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preset Characters Section
        Text(
            text = strings.choosePreset,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Clean 2x2 Grid of Presets
        val rows = CharacterPresets.all.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (rowItems in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (preset in rowItems) {
                        CharacterGridCard(
                            preset = preset,
                            isSelected = preset.id == activePreset.id,
                            modifier = Modifier.weight(1f),
                            onSelectPreview = { activePreset = preset },
                            onOpenDirect = {
                                val bmp = preset.generator(600)
                                onOpenWorkbench(
                                    bmp,
                                    preset.name,
                                    preset.defaultMaterial,
                                    preset.defaultZone
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CharacterGridCard(
    preset: PresetCharacter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelectPreview: () -> Unit,
    onOpenDirect: () -> Unit
) {
    val thumb = remember(preset.id) { preset.generator(220) }

    Surface(
        modifier = modifier.clickable { onSelectPreview() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioTheme.extended.bgBackdrop),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = preset.name,
                    modifier = Modifier.size(102.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = preset.tag,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { onOpenDirect() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
