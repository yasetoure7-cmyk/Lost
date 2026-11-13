package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CampaignSnapshot
import com.example.data.WarriorEntity
import com.example.ui.theme.AmberTactical
import com.example.ui.theme.EmeraldVital
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.SteelCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SquadScreen(
    campaign: CampaignSnapshot,
    onRecruit: (String) -> Unit,
    onToggleDeploy: (String) -> Unit,
    onUpgrade: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TacticalScreenHeader(
            title = "WARRIOR SQUAD COMMAND",
            subtitle = "ACTIVE FIELD TEAM: ${campaign.deployedWarriors.size} / 3 DEPLOYED",
            profile = campaign.profile,
            onBack = onBack
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(campaign.warriors, key = { it.id }) { warrior ->
                WarriorCard(
                    warrior = warrior,
                    scrapAvailable = campaign.profile.scrap,
                    medsAvailable = campaign.profile.meds,
                    onRecruit = { onRecruit(warrior.id) },
                    onToggleDeploy = { onToggleDeploy(warrior.id) },
                    onUpgrade = { onUpgrade(warrior.id) }
                )
            }
        }
    }
}

@Composable
private fun WarriorCard(
    warrior: WarriorEntity,
    scrapAvailable: Int,
    medsAvailable: Int,
    onRecruit: () -> Unit,
    onToggleDeploy: () -> Unit,
    onUpgrade: () -> Unit
) {
    val accent = Color(warrior.accentHex)
    Surface(
        color = GunmetalDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, if (warrior.deployed) accent else TacticalBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${warrior.callsign} // ${warrior.fullName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                    Text(
                        text = "${warrior.specialty} • WEAPON: ${warrior.weaponName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                if (warrior.recruited) {
                    Surface(
                        color = GunmetalSurface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LVL ${warrior.level}/5",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberTactical,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text(
                text = warrior.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!warrior.recruited) {
                    val canRecruit = scrapAvailable >= warrior.recruitScrapCost && medsAvailable >= warrior.recruitMedCost
                    Button(
                        onClick = onRecruit,
                        enabled = canRecruit,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberTactical),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "RECRUIT (${warrior.recruitScrapCost} SCRAP • ${warrior.recruitMedCost} MEDS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalObsidian
                        )
                    }
                } else {
                    Button(
                        onClick = onToggleDeploy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (warrior.deployed) EmeraldVital else GunmetalSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (warrior.deployed) "DEPLOYED IN SQUAD" else "ASSIGN TO SQUAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (warrior.deployed) TacticalObsidian else TextPrimary
                        )
                    }
                    val upCost = 35 * warrior.level
                    OutlinedButton(
                        onClick = onUpgrade,
                        enabled = warrior.level < 5 && scrapAvailable >= upCost,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (warrior.level >= 5) "MAX RANK" else "TRAIN ($upCost SCRAP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberTactical
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChronicleScreen(
    campaign: CampaignSnapshot,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TacticalScreenHeader(
            title = "HISTORY CHRONICLE & OUTPOSTS",
            subtitle = "CIVILIZATION RESTORED: ${campaign.civilizationProgressPercent}% • SECURED OUTPOSTS: ${campaign.outposts.count { it.captured }}/${campaign.outposts.size}",
            profile = campaign.profile,
            onBack = onBack
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Outpost Liberation Tracker
            LazyColumn(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(campaign.outposts, key = { it.id }) { op ->
                    Surface(
                        color = GunmetalDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (op.captured) EmeraldVital else AmberTactical.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(op.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontSize = 14.sp)
                                Text(
                                    text = if (op.captured) "SECURED" else "HOSTILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (op.captured) EmeraldVital else AmberTactical
                                )
                            }
                            Text(
                                text = "${op.zoneName} • COORDS (${op.worldX.toInt()}, ${op.worldZ.toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelCyan
                            )
                            Text(op.loreNote, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Right: Story History Chapters
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(campaign.chronicle, key = { it.id }) { entry ->
                    Surface(
                        color = if (entry.unlocked) GunmetalSurface else GunmetalDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (entry.unlocked) AmberTactical.copy(alpha = 0.7f) else TacticalBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = entry.eraLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.unlocked) AmberTactical else TextMuted
                            )
                            Text(
                                text = entry.chapterTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (entry.unlocked) TextPrimary else TextMuted
                            )
                            Text(
                                text = if (entry.unlocked) entry.narrative else "LOCKED — Continue rescuing survivors, upgrading Sanctuary Village, and securing city outposts to write this chapter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (entry.unlocked) TextSecondary else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
