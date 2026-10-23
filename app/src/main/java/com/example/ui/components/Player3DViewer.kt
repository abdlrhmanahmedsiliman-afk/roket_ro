package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PciStatus
import com.example.model.PlayerCondition
import com.example.model.PlayerLineup
import com.example.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

enum class Render3DMode(val labelAr: String) {
    FACE_3D("وجه اللاعب 3D"),
    REALISTIC("مجسم 3D كامل"),
    HOLO_CARD("بطاقة 3D تكتيكية"),
    WIREFRAME("شبكة تكتيكية")
}

/**
 * 3D Interactive Player & Face Hologram Viewer
 * Features:
 * - True 3D perspective projection with Yaw (horizontal) and Pitch (vertical)
 * - 360° Drag & pinch-to-zoom interactive controls
 * - 3D Spherical Face Mesh Hologram with sweeping biometric scanner
 * - 3D Facial Landmark Tracking nodes and live EKG heartbeat telemetry
 * - 3D Realistic kit mannequin with head avatar texture
 * - 3D Tilting Holographic trading card
 */
@Composable
fun InteractivePlayer3DView(
    playerNumber: Int,
    playerName: String,
    teamColor: Color,
    pciStatus: PciStatus,
    modifier: Modifier = Modifier,
    photoUrl: String = "",
    position: String = "FWD",
    rating: Float = 8.5f,
    renderMode: Render3DMode = Render3DMode.FACE_3D,
    autoRotate: Boolean = false,
    onResetAngle: (() -> Unit)? = null
) {
    var yawAngle by remember { mutableFloatStateOf(0.35f) }
    var pitchAngle by remember { mutableFloatStateOf(0.10f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "player_auto_spin")
    val animatedYaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yaw_spin"
    )

    val currentYaw = if (autoRotate) animatedYaw + yawAngle else yawAngle

    val pciPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pci_aura"
    )

    val scanSweep by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_sweep"
    )

    val ekgPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ekg_phase"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.SurfaceSoft,
                        Color(0xFF070B12),
                        Color(0xFF030508)
                    )
                )
            )
            .border(1.2.dp, pciStatus.color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.75f, 2.2f)
                    yawAngle += pan.x * 0.015f
                    pitchAngle = (pitchAngle - pan.y * 0.012f).coerceIn(-0.45f, 0.45f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.52f
            val scale = (size.minDimension * 0.40f) * zoomScale

            fun project3D(x: Float, y: Float, z: Float): Offset {
                val cosP = cos(pitchAngle.toDouble()).toFloat()
                val sinP = sin(pitchAngle.toDouble()).toFloat()
                val y1 = y * cosP - z * sinP
                val z1 = y * sinP + z * cosP

                val cosY = cos(currentYaw.toDouble()).toFloat()
                val sinY = sin(currentYaw.toDouble()).toFloat()
                val x2 = x * cosY + z1 * sinY
                val z2 = -x * sinY + z1 * cosY

                val distance = 4.2f
                val perspective = distance / (distance - z2)
                return Offset(
                    centerX + x2 * scale * perspective,
                    centerY - y1 * scale * perspective
                )
            }

            // 1. Draw 3D Tactical Holographic Base Ring
            val baseRadius = 0.85f * pciPulse
            val basePoints = 28
            val basePolygon = Path()
            for (i in 0..basePoints) {
                val theta = (i * 2 * Math.PI / basePoints).toFloat()
                val bx = cos(theta.toDouble()).toFloat() * baseRadius
                val bz = sin(theta.toDouble()).toFloat() * baseRadius
                val pt = project3D(bx, -1.35f, bz)
                if (i == 0) basePolygon.moveTo(pt.x, pt.y) else basePolygon.lineTo(pt.x, pt.y)
            }
            drawPath(path = basePolygon, color = pciStatus.color.copy(alpha = 0.18f))
            drawPath(path = basePolygon, color = pciStatus.color.copy(alpha = 0.70f), style = Stroke(width = 2.dp.toPx()))

            // Inner pedestal ring
            val innerBasePolygon = Path()
            for (i in 0..basePoints) {
                val theta = (i * 2 * Math.PI / basePoints).toFloat()
                val bx = cos(theta.toDouble()).toFloat() * (baseRadius * 0.55f)
                val bz = sin(theta.toDouble()).toFloat() * (baseRadius * 0.55f)
                val pt = project3D(bx, -1.35f, bz)
                if (i == 0) innerBasePolygon.moveTo(pt.x, pt.y) else innerBasePolygon.lineTo(pt.x, pt.y)
            }
            drawPath(path = innerBasePolygon, color = AppColors.NeonCyan.copy(alpha = 0.40f), style = Stroke(width = 1.2.dp.toPx()))

            // 2. Render based on selected 3D mode
            when (renderMode) {
                Render3DMode.FACE_3D -> {
                    // Draw 3D Spherical Coordinate Wireframe Sphere around Head
                    val sphereRadius = 0.72f
                    val ringSteps = 16

                    // Latitude rings
                    for (lat in -2..2) {
                        val latY = lat * 0.22f
                        val rLat = cos((lat * 0.35).toDouble()).toFloat() * sphereRadius
                        val ringPath = Path()
                        for (j in 0..ringSteps) {
                            val phi = (j * 2 * Math.PI / ringSteps).toFloat()
                            val px = cos(phi.toDouble()).toFloat() * rLat
                            val pz = sin(phi.toDouble()).toFloat() * rLat
                            val pt = project3D(px, latY, pz)
                            if (j == 0) ringPath.moveTo(pt.x, pt.y) else ringPath.lineTo(pt.x, pt.y)
                        }
                        drawPath(ringPath, AppColors.NeonCyan.copy(alpha = 0.25f), style = Stroke(width = 1.dp.toPx()))
                    }

                    // Longitude rings
                    for (lon in 0..3) {
                        val lonAngle = lon * (Math.PI / 4)
                        val lonPath = Path()
                        for (k in 0..ringSteps) {
                            val theta = (k * 2 * Math.PI / ringSteps).toFloat()
                            val py = sin(theta.toDouble()).toFloat() * sphereRadius
                            val rad = cos(theta.toDouble()).toFloat() * sphereRadius
                            val px = cos(lonAngle).toFloat() * rad
                            val pz = sin(lonAngle).toFloat() * rad
                            val pt = project3D(px, py, pz)
                            if (k == 0) lonPath.moveTo(pt.x, pt.y) else lonPath.lineTo(pt.x, pt.y)
                        }
                        drawPath(lonPath, teamColor.copy(alpha = 0.30f), style = Stroke(width = 1.dp.toPx()))
                    }

                    // 3D Facial Landmarks (Forehead, Eyes, Nose, Cheeks, Chin)
                    val landmarks = listOf(
                        Triple(0f, 0.42f, 0.48f),   // Forehead
                        Triple(-0.20f, 0.22f, 0.46f), // Left Eye
                        Triple(0.20f, 0.22f, 0.46f),  // Right Eye
                        Triple(0f, 0.04f, 0.54f),    // Nose Bridge
                        Triple(-0.24f, -0.12f, 0.40f), // Left Cheek
                        Triple(0.24f, -0.12f, 0.40f),  // Right Cheek
                        Triple(0f, -0.32f, 0.45f)    // Chin
                    )

                    val projectedLandmarks = landmarks.map { (lx, ly, lz) -> project3D(lx, ly, lz) }

                    // Draw connecting biometric mesh lines
                    for (idx in 0 until projectedLandmarks.size - 1) {
                        drawLine(
                            color = AppColors.NeonCyan.copy(alpha = 0.45f),
                            start = projectedLandmarks[idx],
                            end = projectedLandmarks[idx + 1],
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw target nodes
                    projectedLandmarks.forEach { pt ->
                        drawCircle(color = AppColors.NeonCyan, radius = 3.5.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = pt)
                    }

                    // Sweeping Laser Scan Line
                    val scanY = scanSweep * 0.45f
                    val scanLeft = project3D(-0.55f, scanY, 0.35f)
                    val scanRight = project3D(0.55f, scanY, 0.35f)
                    drawLine(
                        color = AppColors.NeonCyan.copy(alpha = 0.85f),
                        start = scanLeft,
                        end = scanRight,
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Render3DMode.REALISTIC, Render3DMode.WIREFRAME -> {
                    val isWireframe = renderMode == Render3DMode.WIREFRAME
                    val bodyColor = if (isWireframe) Color.Transparent else teamColor
                    val strokeColor = if (isWireframe) AppColors.NeonCyan else Color.White.copy(alpha = 0.8f)
                    val wireStroke = Stroke(width = 1.5.dp.toPx())

                    fun draw3DBox(
                        minX: Float, maxX: Float,
                        minY: Float, maxY: Float,
                        minZ: Float, maxZ: Float,
                        fillColor: Color,
                        outlineColor: Color
                    ) {
                        val p000 = project3D(minX, minY, minZ)
                        val p001 = project3D(minX, minY, maxZ)
                        val p010 = project3D(minX, maxY, minZ)
                        val p011 = project3D(minX, maxY, maxZ)
                        val p100 = project3D(maxX, minY, minZ)
                        val p101 = project3D(maxX, minY, maxZ)
                        val p110 = project3D(maxX, maxY, minZ)
                        val p111 = project3D(maxX, maxY, maxZ)

                        val faces = listOf(
                            Path().apply { moveTo(p001.x, p001.y); lineTo(p101.x, p101.y); lineTo(p111.x, p111.y); lineTo(p011.x, p011.y); close() },
                            Path().apply { moveTo(p000.x, p000.y); lineTo(p100.x, p100.y); lineTo(p110.x, p110.y); lineTo(p010.x, p010.y); close() },
                            Path().apply { moveTo(p100.x, p100.y); lineTo(p101.x, p101.y); lineTo(p111.x, p111.y); lineTo(p110.x, p110.y); close() },
                            Path().apply { moveTo(p000.x, p000.y); lineTo(p001.x, p001.y); lineTo(p011.x, p011.y); lineTo(p010.x, p010.y); close() },
                            Path().apply { moveTo(p010.x, p010.y); lineTo(p110.x, p110.y); lineTo(p111.x, p111.y); lineTo(p011.x, p011.y); close() }
                        )
                        faces.forEach { face ->
                            if (!isWireframe) drawPath(face, fillColor)
                            drawPath(face, outlineColor, style = wireStroke)
                        }
                    }

                    // Torso
                    val torsoTop = 0.65f
                    val torsoBottom = -0.15f
                    val torsoWidth = 0.42f
                    val torsoDepth = 0.22f
                    draw3DBox(-torsoWidth / 2, torsoWidth / 2, torsoBottom, torsoTop, -torsoDepth / 2, torsoDepth / 2, bodyColor, strokeColor)

                    // Shorts
                    val shortsBottom = -0.55f
                    val shortsColor = if (isWireframe) Color.Transparent else Color(0xFF151922)
                    draw3DBox(-0.38f / 2, 0.38f / 2, shortsBottom, torsoBottom, -0.20f / 2, 0.20f / 2, shortsColor, strokeColor)

                    // Legs
                    val legWidth = 0.12f
                    val legDepth = 0.14f
                    val kneesY = -0.92f
                    val feetY = -1.30f
                    val legColor = if (isWireframe) Color.Transparent else Color(0xFFD4A373)
                    val socksColor = if (isWireframe) Color.Transparent else teamColor

                    draw3DBox(-0.16f - legWidth / 2, -0.16f + legWidth / 2, kneesY, shortsBottom, -legDepth / 2, legDepth / 2, legColor, strokeColor)
                    draw3DBox(-0.16f - legWidth / 2, -0.16f + legWidth / 2, feetY, kneesY, -legDepth / 2, legDepth / 2, socksColor, strokeColor)
                    draw3DBox(0.16f - legWidth / 2, 0.16f + legWidth / 2, kneesY, shortsBottom, -legDepth / 2, legDepth / 2, legColor, strokeColor)
                    draw3DBox(0.16f - legWidth / 2, 0.16f + legWidth / 2, feetY, kneesY, -legDepth / 2, legDepth / 2, socksColor, strokeColor)

                    // Boots
                    val bootColor = if (isWireframe) Color.Transparent else AppColors.NeonCyan
                    draw3DBox(-0.16f - legWidth / 2, -0.16f + legWidth / 2, -1.35f, feetY, -0.05f, 0.16f, bootColor, strokeColor)
                    draw3DBox(0.16f - legWidth / 2, 0.16f + legWidth / 2, -1.35f, feetY, -0.05f, 0.16f, bootColor, strokeColor)

                    // Arms
                    val armWidth = 0.09f
                    draw3DBox(-torsoWidth / 2 - armWidth, -torsoWidth / 2, 0.10f, torsoTop, -0.10f, 0.10f, bodyColor, strokeColor)
                    draw3DBox(torsoWidth / 2, torsoWidth / 2 + armWidth, 0.10f, torsoTop, -0.10f, 0.10f, bodyColor, strokeColor)

                    // Head Node
                    val headCenter = project3D(0f, 0.95f, 0f)
                    val headRadius = 0.18f * scale
                    val skinTone = if (isWireframe) Color.Transparent else Color(0xFFE0AC69)
                    if (!isWireframe) drawCircle(color = skinTone, radius = headRadius, center = headCenter)
                    drawCircle(color = strokeColor, radius = headRadius, center = headCenter, style = wireStroke)

                    // Chest jersey number badge in 3D
                    val chestPos = project3D(0f, 0.35f, torsoDepth / 2 + 0.02f)
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 13.dp.toPx() * zoomScale, center = chestPos)
                    drawCircle(color = Color.Black, radius = 11.dp.toPx() * zoomScale, center = chestPos)
                }

                Render3DMode.HOLO_CARD -> {
                    // 3D Tilting Card Polygon
                    val cardW = 0.65f
                    val cardH = 1.05f
                    val cp1 = project3D(-cardW, -cardH, 0f)
                    val cp2 = project3D(cardW, -cardH, 0f)
                    val cp3 = project3D(cardW, cardH, 0f)
                    val cp4 = project3D(-cardW, cardH, 0f)

                    val cardPath = Path().apply {
                        moveTo(cp1.x, cp1.y)
                        lineTo(cp2.x, cp2.y)
                        lineTo(cp3.x, cp3.y)
                        lineTo(cp4.x, cp4.y)
                        close()
                    }
                    drawPath(
                        path = cardPath,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF0F172A), teamColor.copy(alpha = 0.4f), Color(0xFF020617))
                        )
                    )
                    drawPath(
                        path = cardPath,
                        color = AppColors.NeonGold.copy(alpha = 0.85f),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // Draw Live EKG Waveform in bottom-left
            val ekgW = 90.dp.toPx()
            val ekgH = 26.dp.toPx()
            val ekgStart = Offset(20.dp.toPx(), size.height - 38.dp.toPx())

            val ekgPath = Path().apply {
                moveTo(ekgStart.x, ekgStart.y + ekgH / 2)
                lineTo(ekgStart.x + ekgW * 0.25f, ekgStart.y + ekgH / 2)
                lineTo(ekgStart.x + ekgW * 0.35f, ekgStart.y + ekgH * 0.1f)
                lineTo(ekgStart.x + ekgW * 0.45f, ekgStart.y + ekgH * 0.9f)
                lineTo(ekgStart.x + ekgW * 0.55f, ekgStart.y + ekgH / 2)
                lineTo(ekgStart.x + ekgW, ekgStart.y + ekgH / 2)
            }
            drawPath(path = ekgPath, color = AppColors.NeonGreen.copy(alpha = 0.75f), style = Stroke(width = 1.5.dp.toPx()))
        }

        // Central Floating 3D Face Portrait Avatar with Parallactic Tilt
        if (renderMode == Render3DMode.FACE_3D || renderMode == Render3DMode.HOLO_CARD) {
            val tiltOffsetX = (sin(currentYaw.toDouble()) * 14).dp
            val tiltOffsetY = (-sin(pitchAngle.toDouble()) * 10).dp

            Box(
                modifier = Modifier
                    .offset(x = tiltOffsetX, y = tiltOffsetY)
                    .size((130 * zoomScale).dp),
                contentAlignment = Alignment.Center
            ) {
                // Gyroscope outer glowing rings
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, AppColors.NeonCyan.copy(alpha = 0.5f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .border(1.2.dp, teamColor.copy(alpha = 0.6f), CircleShape)
                )

                // Face Orb with real player headshot photo
                Box(
                    modifier = Modifier.fillMaxSize(0.72f),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerAvatarView(
                        photoUrl = photoUrl,
                        playerName = playerName,
                        playerNumber = playerNumber,
                        teamColor = teamColor,
                        size = 180.dp,
                        fontSize = 32.sp,
                        borderColor = AppColors.NeonCyan,
                        borderWidth = 2.5.dp
                    )

                    // Biometric scanning horizon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, AppColors.NeonCyan, Color.Transparent)
                                )
                            )
                    )
                }
            }
        }

        // Overlay: Top Player Bio Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatarView(
                photoUrl = photoUrl,
                playerName = playerName,
                playerNumber = playerNumber,
                teamColor = teamColor,
                size = 42.dp,
                fontSize = 14.sp,
                borderColor = Color.White,
                borderWidth = 1.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = playerName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "#$playerNumber • $position • تقييم: $rating/10", color = AppColors.NeonCyan, fontSize = 10.5.sp)
            }
        }

        // Overlay: Bottom-Right Biometric Telemetry
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(0.8.dp, AppColors.NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("🫀 نبض: 74 BPM", color = AppColors.NeonGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(0.8.dp, AppColors.NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("🎯 تركيز: 96%", color = AppColors.NeonCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(0.8.dp, AppColors.Warning.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("⚡ سرعة: 34.6 كم/س", color = AppColors.Warning, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Gesture Hints
        Text(
            text = "اسحب للتدوير 360° • قرّب للتكبير والتصغير",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

/**
 * Full Detailed Bottom Sheet for 3D Player View
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Player3DBottomSheet(
    player: PlayerLineup,
    teamName: String,
    teamColor: Color,
    playerCondition: PlayerCondition? = null,
    pci: PlayerCondition? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRenderMode by remember { mutableStateOf(Render3DMode.FACE_3D) }
    var autoSpin by remember { mutableStateOf(true) }

    val activePci = playerCondition ?: pci ?: remember(player.name) {
        val score = (player.rating * 10).toInt().coerceIn(65, 96)
        PlayerCondition(
            playerId = player.athleteId.ifBlank { "${player.number}_${player.name}" },
            playerName = player.name,
            playerNumber = player.number,
            teamName = teamName,
            position = player.position,
            factors = com.example.model.PciFactors(
                physicalFatigue = (score - 5).toFloat().coerceIn(40f, 98f),
                mentalPsychological = score.toFloat(),
                recentForm = (score + 3).toFloat().coerceIn(50f, 99f),
                injuryRisk = (100 - (100 - score) / 2).toFloat().coerceIn(60f, 98f),
                travelScheduleLoad = 85f,
                tacticalFit = score.toFloat(),
                weatherPitchAdaptation = 90f
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF070B12),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            // Header: Player Name, Position, Team
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatarView(
                        photoUrl = player.photoUrl,
                        playerName = player.name,
                        playerNumber = player.number,
                        teamColor = teamColor,
                        size = 54.dp,
                        fontSize = 18.sp,
                        borderColor = teamColor,
                        borderWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (player.isCaptain) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.NeonGold.copy(alpha = 0.2f))
                                        .border(1.dp, AppColors.NeonGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("C قائد", color = AppColors.NeonGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "$teamName • مركز: ${player.position} • تقييم: ${player.rating}/10",
                            color = AppColors.TextSoft,
                            fontSize = 11.5.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = AppColors.TextSoft
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive 3D Player View
            InteractivePlayer3DView(
                playerNumber = player.number,
                playerName = player.name,
                teamColor = teamColor,
                pciStatus = activePci.status,
                photoUrl = player.photoUrl,
                position = player.position,
                rating = player.rating,
                renderMode = selectedRenderMode,
                autoRotate = autoSpin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Render Mode Chips & Auto-Spin Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Render3DMode.entries.forEach { mode ->
                        val isSelected = selectedRenderMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppColors.NeonBlue else AppColors.Surface)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedRenderMode = mode }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mode.labelAr,
                                color = if (isSelected) Color.Black else AppColors.TextSoft,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Auto-spin toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (autoSpin) AppColors.NeonCyan.copy(alpha = 0.2f) else AppColors.Surface)
                        .border(
                            1.dp,
                            if (autoSpin) AppColors.NeonCyan else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { autoSpin = !autoSpin }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Rotate",
                            tint = if (autoSpin) AppColors.NeonCyan else AppColors.TextSoft,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (autoSpin) "دوران نشط" else "دوران",
                            color = if (autoSpin) AppColors.NeonCyan else AppColors.TextSoft,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PCI Score Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = Brush.horizontalGradient(
                    colors = listOf(activePci.status.color.copy(alpha = 0.8f), AppColors.NeonBlue.copy(alpha = 0.4f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مؤشر الجاهزية البدنية والفنية (PCI)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحليل خوارزمي متقدم لـ 7 عوامل جاهزية متكاملة",
                                color = AppColors.TextSoft,
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(activePci.status.color.copy(alpha = 0.2f))
                                .border(1.dp, activePci.status.color, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${activePci.overallScore}/100",
                                color = activePci.status.color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7 Factors Breakdown
                    PciBarRow("الإجهاد البدني والحمل التراكمي", activePci.factors.physicalFatigue.toInt(), activePci.status.color)
                    PciBarRow("الاستشفاء والجاهزية الأخيرة", activePci.factors.recentForm.toInt(), AppColors.NeonCyan)
                    PciBarRow("مؤشر الاستقرار النفسي والتركيز", activePci.factors.mentalPsychological.toInt(), AppColors.NeonBlue)
                    PciBarRow("خطر الإصابات والإجهاد المتكرر", (100 - activePci.factors.injuryRisk.toInt()).coerceIn(5, 55), if (activePci.factors.injuryRisk < 60) AppColors.NeonRed else AppColors.NeonGreen)
                    PciBarRow("التوافق التكتيكي وانسجام الخطة", activePci.factors.tacticalFit.toInt(), AppColors.NeonGold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PciBarRow(title: String, value: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = AppColors.TextSoft, fontSize = 11.sp)
            Text(text = "$value%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value / 100f).coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(color)
            )
        }
    }
}
