package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.TacticalGearType
import com.example.game.GameEngine
import com.example.game.HudState
import com.example.game.LostEarthGLSurfaceView
import com.example.ui.GameScreenRoute
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

@Composable
fun GameCombatScreen(
    engine: GameEngine,
    hud: HudState,
    onNavigate: (GameScreenRoute) -> Unit
) {
    var pauseOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed 60 FPS 3D OpenGL World
        AndroidView(
            factory = { ctx -> LostEarthGLSurfaceView(ctx, engine) },
            modifier = Modifier.fillMaxSize()
        )

        // Left-side Aim Touch-Drag Area (Left Side Manages Aim, Weapon Selection & Tactical Gear)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        engine.applyAimDelta(dragAmount.x, dragAmount.y)
                    }
                }
                .testTag("left_aim_pad")
        )

        // Low-health & Damage Crimson Vignette
        if (hud.damageFlash > 0f || hud.health < 35) {
            val alpha = maxOf(hud.damageFlash * 0.42f, if (hud.health < 35) 0.25f else 0f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, CrimsonThreat.copy(alpha = alpha))
                        )
                    )
            )
        }

        // Center Dynamic Tactical Crosshair & Hitmarkers
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CrosshairOverlay(
                isAds = hud.isAds,
                hitMarker = hud.hitMarkerActive,
                headshot = hud.hitMarkerHeadshot
            )
        }

        // Safe-area Tactical HUD Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // TOP BAR: Mini-Map (top-left) | Outpost Capture + Radio Comms (top-center) | Health & View Toggle & Menu (top-right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                TacticalMiniMap(
                    hud = hud,
                    worldBoxes = engine.worldBoxes,
                    modifier = Modifier.size(width = 165.dp, height = 112.dp)
                )

                // Center Comms & Outpost Capture Progress
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hud.activeOutpostName != null) {
                        Surface(
                            color = TacticalObsidian.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberTactical)
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(240.dp)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SECURING ${hud.activeOutpostName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberTactical
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { hud.activeOutpostProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = EmeraldVital,
                                    trackColor = GunmetalSurface
                                )
                            }
                        }
                    }

                    if (hud.radioMessage != null && hud.radioSpeaker != null) {
                        Surface(
                            color = GunmetalDark.copy(alpha = 0.86f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SteelCyan.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(
                                    text = "COMMS // ${hud.radioSpeaker}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SteelCyan,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = hud.radioMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Top-Right: Health & Armor Cluster + 1st/3rd Person Toggle + Menu
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    HealthArmorCluster(
                        hud = hud,
                        modifier = Modifier.width(178.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Surface(
                            onClick = { engine.togglePerspective() },
                            color = GunmetalDark.copy(alpha = 0.86f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberTactical),
                            modifier = Modifier.testTag("camera_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Cameraswitch,
                                    contentDescription = "Switch View",
                                    tint = AmberTactical,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = hud.perspective.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = { pauseOpen = true },
                            color = GunmetalDark.copy(alpha = 0.86f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            modifier = Modifier.testTag("pause_menu_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Tactical Menu",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "HQ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Contextual Interact Button (Rescue Survivor / Scavenge Crate)
            if (hud.interactPrompt != null) {
                Button(
                    onClick = { engine.triggerInteract() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVital),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .height(48.dp)
                        .testTag("interact_button")
                ) {
                    Text(
                        text = hud.interactPrompt,
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalObsidian
                    )
                }
            }

            // =========================================================================
            // LEFT SIDE: AIM, FIRE, ADS, WEAPON SELECTION & TACTICAL GEAR
            // =========================================================================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tactical Gear Bar (Frag, Med-Stim, Shock Flare)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TacticalGearChip(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "FRAG",
                        count = hud.fragCount,
                        tint = Color(0xFFF97316),
                        tag = "gear_frag_button",
                        onClick = { engine.useTacticalGear(TacticalGearType.FRAG_GRENADE) }
                    )
                    TacticalGearChip(
                        icon = Icons.Filled.MedicalServices,
                        label = "MED",
                        count = hud.medStimCount,
                        tint = EmeraldVital,
                        tag = "gear_med_button",
                        onClick = { engine.useTacticalGear(TacticalGearType.MED_STIM) }
                    )
                    TacticalGearChip(
                        icon = Icons.Filled.Bolt,
                        label = "EMP",
                        count = hud.shockFlareCount,
                        tint = SteelCyan,
                        tag = "gear_emp_button",
                        onClick = { engine.useTacticalGear(TacticalGearType.SHOCK_FLARE) }
                    )
                }

                // Weapon Selection Slots + Ammo Readout + Reload
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hud.weapons.forEachIndexed { idx, w ->
                        val active = idx == hud.activeSlot
                        Surface(
                            onClick = { engine.selectWeaponSlot(idx) },
                            color = if (active) AmberTactical.copy(alpha = 0.25f) else TacticalObsidian.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.5.dp, if (active) AmberTactical else TacticalBorder),
                            modifier = Modifier.testTag("weapon_slot_$idx")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = w.shortCode,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) AmberTactical else TextPrimary,
                                    fontSize = 11.sp
                                )
                                if (active) {
                                    Text(
                                        text = "${hud.magAmmo}/${hud.reserveAmmo}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Reload Button
                    Surface(
                        onClick = { engine.triggerReload() },
                        color = TacticalObsidian.copy(alpha = 0.8f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, if (hud.isReloading) AmberTactical else TacticalBorder),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("reload_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Reload",
                                tint = if (hud.isReloading) AmberTactical else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Fire Trigger (Hold to Shoot) + ADS Scope Toggle on LEFT side
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hold-to-Fire Tactical Trigger
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CrimsonThreat.copy(alpha = 0.72f))
                            .border(2.5.dp, AmberTactical, CircleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    engine.inputFiring = true
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                    engine.inputFiring = false
                                }
                            }
                            .testTag("left_fire_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.MyLocation,
                                contentDescription = "Fire Weapon",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "FIRE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ADS Aim Scope Toggle
                    Surface(
                        onClick = { engine.inputAds = !engine.inputAds },
                        color = if (hud.isAds) AmberTactical.copy(alpha = 0.35f) else TacticalObsidian.copy(alpha = 0.78f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, if (hud.isAds) AmberTactical else TacticalBorder),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("left_ads_button")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CenterFocusStrong,
                                contentDescription = "Aim Down Sights",
                                tint = if (hud.isAds) AmberTactical else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ADS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hud.isAds) AmberTactical else TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // RIGHT SIDE: MOVEMENT THUMBSTICK, SPRINT, CROUCH & JUMP
            // =========================================================================
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tactical Stance & Agility Buttons on the Right Flank
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        onClick = { engine.triggerJump() },
                        color = TacticalObsidian.copy(alpha = 0.78f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, TacticalBorder),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("right_jump_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Jump", tint = TextPrimary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                engine.inputCrouch = !engine.inputCrouch
                                if (engine.inputCrouch) engine.inputSprint = false
                            },
                            color = if (hud.isCrouching) AmberTactical.copy(alpha = 0.35f) else TacticalObsidian.copy(alpha = 0.78f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (hud.isCrouching) AmberTactical else TacticalBorder),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("right_crouch_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.South,
                                    contentDescription = "Crouch",
                                    tint = if (hud.isCrouching) AmberTactical else TextPrimary
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                engine.inputSprint = !engine.inputSprint
                                if (engine.inputSprint) engine.inputCrouch = false
                            },
                            color = if (hud.isSprinting) AmberTactical.copy(alpha = 0.35f) else TacticalObsidian.copy(alpha = 0.78f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (hud.isSprinting) AmberTactical else TacticalBorder),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("right_sprint_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.DirectionsRun,
                                    contentDescription = "Sprint Lock",
                                    tint = if (hud.isSprinting) AmberTactical else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Right-Side Movement Joystick
                MovementThumbstick(
                    onMoveVectorChanged = { mx, my ->
                        engine.inputMoveX = mx
                        engine.inputMoveY = my
                    }
                )
            }
        }

        // Player Downed / Emergency Evac Overlay
        if (hud.isDowned) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TacticalObsidian.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = GunmetalDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, CrimsonThreat)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "OPERATOR CRITICAL",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CrimsonThreat
                        )
                        Text(
                            text = "Sanctuary Medevac unit standing by. Your resources and progress are preserved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { engine.respawnAtSanctuary() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberTactical)
                            ) {
                                Text("EMERGENCY EVAC", color = TacticalObsidian)
                            }
                            OutlinedButton(onClick = { onNavigate(GameScreenRoute.MAIN_MENU) }) {
                                Text("RETURN TO HQ", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Pause / Tactical HQ Modal
        if (pauseOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TacticalObsidian.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = GunmetalDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, AmberTactical)
                ) {
                    Column(
                        modifier = Modifier
                            .width(440.dp)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TACTICAL COMMAND // LOST EARTH",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AmberTactical
                        )
                        Text(
                            text = "CONTROLS: Right Thumb = Move/Sprint/Crouch • Left Side = Aim/Fire/Weapons/Gear",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { pauseOpen = false },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberTactical),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RESUME", color = TacticalObsidian)
                            }
                            OutlinedButton(
                                onClick = {
                                    pauseOpen = false
                                    onNavigate(GameScreenRoute.VILLAGE_HUB)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("VILLAGE", color = TextPrimary)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pauseOpen = false
                                    onNavigate(GameScreenRoute.ARMORY)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ARMORY", color = TextPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    pauseOpen = false
                                    onNavigate(GameScreenRoute.SQUAD)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SQUAD", color = TextPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    pauseOpen = false
                                    onNavigate(GameScreenRoute.MAIN_MENU)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("LOBBY", color = CrimsonThreat)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalGearChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = TacticalObsidian.copy(alpha = 0.8f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (count > 0) tint.copy(alpha = 0.85f) else TacticalBorder),
        modifier = Modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (count > 0) tint else TextSecondary, modifier = Modifier.size(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 9.sp)
                Text("x$count", style = MaterialTheme.typography.labelLarge, color = tint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CrosshairOverlay(
    isAds: Boolean,
    hitMarker: Boolean,
    headshot: Boolean
) {
    Canvas(modifier = Modifier.size(52.dp)) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val gap = if (isAds) 4f else 10f
        val arm = if (isAds) 7f else 9f
        val retColor = if (isAds) AmberTactical else Color.White.copy(alpha = 0.88f)

        // Four crosshair ticks
        drawLine(retColor, Offset(cx - gap - arm, cy), Offset(cx - gap, cy), strokeWidth = 2.2f)
        drawLine(retColor, Offset(cx + gap, cy), Offset(cx + gap + arm, cy), strokeWidth = 2.2f)
        drawLine(retColor, Offset(cx, cy - gap - arm), Offset(cx, cy - gap), strokeWidth = 2.2f)
        drawLine(retColor, Offset(cx, cy + gap), Offset(cx, cy + gap + arm), strokeWidth = 2.2f)
        drawCircle(AmberTactical, radius = 2.4f, center = Offset(cx, cy))

        if (hitMarker) {
            val hmCol = if (headshot) CrimsonThreat else Color.White
            val d1 = 7f; val d2 = 16f
            drawLine(hmCol, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), strokeWidth = 3f)
            drawLine(hmCol, Offset(cx + d2, cy - d2), Offset(cx + d1, cy - d1), strokeWidth = 3f)
            drawLine(hmCol, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), strokeWidth = 3f)
            drawLine(hmCol, Offset(cx + d2, cy + d2), Offset(cx + d1, cy + d1), strokeWidth = 3f)
        }
    }
}
