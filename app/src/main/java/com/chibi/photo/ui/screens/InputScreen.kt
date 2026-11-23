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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
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
import com.chibi.photo.ui.components.CharacterPresets
import com.chibi.photo.ui.components.PresetCharacter
import com.chibi.photo.ui.theme.*

@Composable
fun InputScreen(
    onOpenWorkbench: (
        bitmap: Bitmap,
        title: String,
        presetMaterial: MaterialPreset,
        presetZone: ZoneTemplate
    ) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                    "Photo",
                    MaterialPreset.SOFT_GEL,
                    ZoneTemplate.DUAL_CHEEKS
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Clean Header: Chibi Logo + App Name + Import Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val logoThumb = remember { CharacterPresets.all.first().generator(96) }
                Image(
                    bitmap = logoThumb.asImageBitmap(),
                    contentDescription = "Chibi Photo Logo",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, BlueHairlineStrong, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Chibi Photo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
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
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Interactive Hero Card (Clean, minimal text, direct touch interaction)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BlueCard,
            border = BorderStroke(1.dp, BlueHairline)
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
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryBlueLight)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activePreset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = activePreset.tag,
                        style = MonoTabularStyle,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(256.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueDeepest)
                        .border(1.dp, BlueHairline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val meshPaint = remember {
                        Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }
                    }

                    Canvas(
                        modifier = Modifier
                            .size(236.dp)
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

                    // Simple Mesh toggle chip
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (previewEngine.showWireframeLattice) PrimaryBlue else BlueCard)
                            .border(1.dp, BlueHairline, RoundedCornerShape(6.dp))
                            .clickable {
                                previewEngine.showWireframeLattice = !previewEngine.showWireframeLattice
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Grid",
                            style = MonoTabularStyle,
                            color = if (previewEngine.showWireframeLattice) TextPrimary else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Solid Royal Blue Primary Action Button
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
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open studio",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Characters",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Clean 2x2 Character Grid
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

        Spacer(modifier = Modifier.height(16.dp))
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
        color = if (isSelected) BlueRaised else BlueCard,
        border = BorderStroke(1.5.dp, if (isSelected) PrimaryBlueLight else BlueHairline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BlueDeepest),
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
                        color = TextPrimary
                    )
                    Text(
                        text = preset.tag,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryBlue else BlueDeepest)
                        .border(1.dp, BlueHairline, RoundedCornerShape(8.dp))
                        .clickable { onOpenDirect() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
