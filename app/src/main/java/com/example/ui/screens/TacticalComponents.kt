package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CampaignProfileEntity
import com.example.game.BlipKind
import com.example.game.HudState
import com.example.game.WorldBox
import com.example.ui.theme.AmberTactical
import com.example.ui.theme.CrimsonThreat
import com.example.ui.theme.EmeraldVital
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.SteelCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun TacticalScreenHeader(
    title: String,
    subtitle: String,
    profile: CampaignProfileEntity?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GunmetalDark.copy(alpha = 0.94f))
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmberTactical
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontSize = 20.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberTactical
                )
            }
        }
        if (profile != null) {
            ResourceTickerRow(profile)
        }
    }
}

@Composable
fun ResourceTickerRow(profile: CampaignProfileEntity) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResourceChip(Icons.Filled.Build, "${profile.scrap}", "SCRAP", AmberTactical)
        ResourceChip(Icons.Filled.MedicalServices, "${profile.meds}", "MEDS", EmeraldVital)
        ResourceChip(Icons.Filled.ViewInAr, "${profile.ammo}", "AMMO", SteelCyan)
        ResourceChip(Icons.Filled.LocalGasStation, "${profile.fuel}", "FUEL", Color(0xFFF97316))
        ResourceChip(Icons.Filled.Science, "${profile.bioSamples}", "BIO", Color(0xFFA855F7))
    }
}

@Composable
private fun ResourceChip(icon: ImageVector, value: String, label: String, tint: Color) {
    Surface(
        color = GunmetalSurface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontSize = 12.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun TacticalMiniMap(
    hud: HudState,
    worldBoxes: List<WorldBox>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = TacticalObsidian.copy(alpha = 0.78f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, AmberTactical.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hud.currentZone.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberTactical,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${hud.playerX.toInt()},${hud.playerZ.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0A1017))
            ) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val radarRange = 55f
                val scale = (minOf(size.width, size.height) * 0.48f) / radarRange

                // Helper transforms world offset into player-rotated radar space
                val cosY = cos(-hud.playerYaw)
                val sinY = sin(-hud.playerYaw)
                fun toMap(wx: Float, wz: Float): Offset {
                    val dx = wx - hud.playerX
                    val dz = wz - hud.playerZ
                    val rx = dx * cosY - dz * sinY
                    val rz = dx * sinY + dz * cosY
                    return Offset(cx + rx * scale, cy + rz * scale)
                }

                // Radar range rings
                drawCircle(
                    color = TacticalBorder.copy(alpha = 0.45f),
                    radius = radarRange * 0.5f * scale,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = TacticalBorder.copy(alpha = 0.45f),
                    radius = radarRange * 0.92f * scale,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f)
                )

                // World buildings near player
                for (b in worldBoxes) {
                    if (!b.collidable) continue
                    if (hypot(b.x - hud.playerX, b.z - hud.playerZ) > radarRange + 15f) continue
                    val pt = toMap(b.x, b.z)
                    val rw = (b.sx * scale).coerceAtLeast(3f)
                    val rh = (b.sz * scale).coerceAtLeast(3f)
                    drawRect(
                        color = Color(0xFF334155).copy(alpha = 0.8f),
                        topLeft = Offset(pt.x - rw * 0.5f, pt.y - rh * 0.5f),
                        size = Size(rw, rh)
                    )
                }

                // Blips (Outposts, Crates, Survivors, Squad, Zombies)
                for (blip in hud.mapBlips) {
                    val rawDist = hypot(blip.x - hud.playerX, blip.z - hud.playerZ)
                    val pt = if (rawDist > radarRange * 0.92f && (blip.kind == BlipKind.OUTPOST_HOSTILE || blip.kind == BlipKind.SURVIVOR)) {
                        // Clamp important objective icons to radar edge
                        val dx = blip.x - hud.playerX
                        val dz = blip.z - hud.playerZ
                        val rx = dx * cosY - dz * sinY
                        val rz = dx * sinY + dz * cosY
                        val len = hypot(rx, rz).coerceAtLeast(1f)
                        val clamped = radarRange * 0.88f * scale
                        Offset(cx + (rx / len) * clamped, cy + (rz / len) * clamped)
                    } else if (rawDist <= radarRange) {
                        toMap(blip.x, blip.z)
                    } else {
                        continue
                    }

                    when (blip.kind) {
                        BlipKind.ZOMBIE -> drawCircle(CrimsonThreat, radius = 3.2f, center = pt)
                        BlipKind.SQUAD -> drawCircle(SteelCyan, radius = 4.2f, center = pt)
                        BlipKind.SURVIVOR -> drawCircle(EmeraldVital, radius = 4.8f, center = pt)
                        BlipKind.CRATE -> drawRect(
                            AmberTactical,
                            topLeft = Offset(pt.x - 2.5f, pt.y - 2.5f),
                            size = Size(5f, 5f)
                        )
                        BlipKind.OUTPOST_HOSTILE -> drawCircle(
                            AmberTactical,
                            radius = 6f,
                            center = pt,
                            style = Stroke(width = 2.2f)
                        )
                        BlipKind.OUTPOST_SECURED -> drawCircle(
                            EmeraldVital,
                            radius = 6f,
                            center = pt,
                            style = Stroke(width = 2.2f)
                        )
                    }
                }

                // Player FOV cone & arrow at center
                val cone = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - 18f, cy - 32f)
                    lineTo(cx + 18f, cy - 32f)
                    close()
                }
                drawPath(
                    cone,
                    brush = Brush.verticalGradient(
                        listOf(AmberTactical.copy(alpha = 0.04f), AmberTactical.copy(alpha = 0.25f))
                    )
                )
                drawCircle(TextPrimary, radius = 4.5f, center = Offset(cx, cy))
            }
        }
    }
}

@Composable
fun HealthArmorCluster(
    hud: HudState,
    modifier: Modifier = Modifier
) {
    val hpFrac = (hud.health.toFloat() / hud.maxHealth.coerceAtLeast(1)).coerceIn(0f, 1f)
    val armorFrac = (hud.armor.toFloat() / hud.maxArmor.coerceAtLeast(1)).coerceIn(0f, 1f)
    val hpColor = when {
        hpFrac > 0.55f -> EmeraldVital
        hpFrac > 0.28f -> AmberTactical
        else -> CrimsonThreat
    }

    Surface(
        modifier = modifier,
        color = TacticalObsidian.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.MedicalServices, contentDescription = "Health", tint = hpColor, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${hud.health}",
                        style = MaterialTheme.typography.labelLarge,
                        color = hpColor,
                        fontSize = 14.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Shield, contentDescription = "Armor", tint = SteelCyan, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${hud.armor}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SteelCyan,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "KILLS ${hud.killCountSession}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberTactical,
                    fontSize = 10.sp
                )
            }

            // Health Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1F2937))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hpFrac)
                        .height(7.dp)
                        .background(hpColor)
                )
            }
            // Armor Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1F2937))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(armorFrac)
                        .height(4.dp)
                        .background(SteelCyan)
                )
            }
            // Stamina Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1F2937))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hud.stamina.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(AmberTactical)
                )
            }
        }
    }
}

@Composable
fun MovementThumbstick(
    onMoveVectorChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 95f

    Box(
        modifier = modifier
            .size(132.dp)
            .clip(CircleShape)
            .background(TacticalObsidian.copy(alpha = 0.45f))
            .border(2.dp, AmberTactical.copy(alpha = 0.55f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMoveVectorChanged(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMoveVectorChanged(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val len = hypot(raw.x, raw.y)
                        val clamped = if (len > maxRadiusPx) {
                            Offset(raw.x / len * maxRadiusPx, raw.y / len * maxRadiusPx)
                        } else raw
                        knobOffset = clamped
                        // Forward on screen is -Y, so invert Y for forward movement (+1)
                        onMoveVectorChanged(clamped.x / maxRadiusPx, -clamped.y / maxRadiusPx)
                    }
                )
            }
            .testTag("right_movement_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(
                color = TacticalBorder.copy(alpha = 0.6f),
                radius = size.minDimension * 0.36f,
                center = center,
                style = Stroke(width = 2f)
            )
            val knobPos = Offset(center.x + knobOffset.x * 0.5f, center.y + knobOffset.y * 0.5f)
            drawCircle(
                color = AmberTactical.copy(alpha = 0.85f),
                radius = 24.dp.toPx(),
                center = knobPos
            )
            drawCircle(
                color = TacticalObsidian,
                radius = 12.dp.toPx(),
                center = knobPos
            )
        }
        Text(
            text = "MOVE",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )
    }
}
