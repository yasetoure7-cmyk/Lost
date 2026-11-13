package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ArmorCamo
import com.example.data.ArmorHelmet
import com.example.data.ArmorVest
import com.example.data.CampaignSnapshot
import com.example.data.WeaponType
import com.example.game.ArmoryPreviewGLSurfaceView
import com.example.ui.theme.AmberTactical
import com.example.ui.theme.EmeraldVital
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.SteelCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ArmoryScreen(
    campaign: CampaignSnapshot,
    onSelectHelmet: (ArmorHelmet) -> Unit,
    onSelectVest: (ArmorVest) -> Unit,
    onSelectCamo: (ArmorCamo) -> Unit,
    onTogglePauldrons: (Boolean) -> Unit,
    onTogglePack: (Boolean) -> Unit,
    onSelectWeapons: (WeaponType, WeaponType) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val loadout = campaign.armorLoadout
    val unlockedHelmets = campaign.profile.unlockedHelmets.split(",").toSet()
    val unlockedVests = campaign.profile.unlockedVests.split(",").toSet()
    val unlockedCamos = campaign.profile.unlockedCamos.split(",").toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TacticalScreenHeader(
            title = "TACTICAL ARMORY & SUIT",
            subtitle = "TOTAL ARMOR ${loadout.maxArmor} • CAMO: ${loadout.camo.displayName.uppercase()}",
            profile = campaign.profile,
            onBack = onBack
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Live 3D Rotating Soldier Preview
            Surface(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                color = GunmetalDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx -> ArmoryPreviewGLSurfaceView(ctx) },
                        update = { view ->
                            view.renderer.loadout = loadout
                            view.renderer.weapon = campaign.primaryWeapon
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Surface(
                        color = TacticalObsidian.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${loadout.helmet.displayName} • ${loadout.vest.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberTactical,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Right Customization Tabs
            Surface(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                color = GunmetalDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = GunmetalSurface,
                        contentColor = AmberTactical
                    ) {
                        listOf("HELMETS", "VESTS", "CAMO & RIG", "WEAPONS").forEachIndexed { i, label ->
                            Tab(
                                selected = selectedTab == i,
                                onClick = { selectedTab = i },
                                text = {
                                    Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                                }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> items(ArmorHelmet.entries) { helm ->
                                val owned = unlockedHelmets.contains(helm.name)
                                val equipped = loadout.helmet == helm
                                GearOptionRow(
                                    title = helm.displayName,
                                    detail = "${helm.description} (+${helm.armorBonus} Armor)",
                                    owned = owned,
                                    equipped = equipped,
                                    costLabel = "${helm.scrapCost} SCRAP",
                                    onClick = { onSelectHelmet(helm) }
                                )
                            }
                            1 -> items(ArmorVest.entries) { vest ->
                                val owned = unlockedVests.contains(vest.name)
                                val equipped = loadout.vest == vest
                                GearOptionRow(
                                    title = vest.displayName,
                                    detail = "${vest.description} (+${vest.armorBonus} Armor)",
                                    owned = owned,
                                    equipped = equipped,
                                    costLabel = "${vest.scrapCost} SCRAP",
                                    onClick = { onSelectVest(vest) }
                                )
                            }
                            2 -> {
                                item {
                                    Surface(
                                        color = GunmetalSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, TacticalBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Heavy Shoulder Pauldrons (+15 Armor)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                                Spacer(Modifier.width(8.dp))
                                                Switch(checked = loadout.heavyPauldrons, onCheckedChange = onTogglePauldrons)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Tactical Pack", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                                Spacer(Modifier.width(8.dp))
                                                Switch(checked = loadout.tacticalPack, onCheckedChange = onTogglePack)
                                            }
                                        }
                                    }
                                }
                                items(ArmorCamo.entries) { camo ->
                                    val owned = unlockedCamos.contains(camo.name)
                                    val equipped = loadout.camo == camo
                                    GearOptionRow(
                                        title = camo.displayName,
                                        detail = "Ballistic weave finish",
                                        owned = owned,
                                        equipped = equipped,
                                        costLabel = "${camo.scrapCost} SCRAP",
                                        swatchColor = Color(camo.primaryHex),
                                        onClick = { onSelectCamo(camo) }
                                    )
                                }
                            }
                            else -> {
                                val primaries = listOf(
                                    WeaponType.ASSAULT_RIFLE,
                                    WeaponType.TACTICAL_SHOTGUN,
                                    WeaponType.MARKSMAN_SNIPER
                                )
                                items(primaries) { weapon ->
                                    val isPrimary = campaign.primaryWeapon == weapon
                                    val isSecondary = campaign.secondaryWeapon == weapon
                                    Surface(
                                        color = GunmetalSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPrimary) AmberTactical else if (isSecondary) SteelCyan else TacticalBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(weapon.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                                Text(
                                                    "DMG ${weapon.damage * weapon.pellets} • MAG ${weapon.magSize} • RANGE ${weapon.rangeMeters.toInt()}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { onSelectWeapons(weapon, campaign.secondaryWeapon) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isPrimary) AmberTactical else GunmetalDark
                                                    )
                                                ) {
                                                    Text(
                                                        if (isPrimary) "PRIMARY" else "SET SLOT 1",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isPrimary) TacticalObsidian else TextPrimary
                                                    )
                                                }
                                                Button(
                                                    onClick = { onSelectWeapons(campaign.primaryWeapon, weapon) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSecondary) SteelCyan else GunmetalDark
                                                    )
                                                ) {
                                                    Text(
                                                        if (isSecondary) "SLOT 2" else "SET SLOT 2",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSecondary) TacticalObsidian else TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GearOptionRow(
    title: String,
    detail: String,
    owned: Boolean,
    equipped: Boolean,
    costLabel: String,
    swatchColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        color = GunmetalSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.2.dp, if (equipped) AmberTactical else TacticalBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (swatchColor != null) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(swatchColor)
                    )
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontSize = 14.sp)
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontSize = 11.sp)
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        equipped -> EmeraldVital
                        owned -> SteelCyan
                        else -> AmberTactical
                    }
                ),
                modifier = Modifier.height(42.dp)
            ) {
                Text(
                    text = when {
                        equipped -> "EQUIPPED"
                        owned -> "EQUIP"
                        else -> "FORGE ($costLabel)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalObsidian
                )
            }
        }
    }
}
