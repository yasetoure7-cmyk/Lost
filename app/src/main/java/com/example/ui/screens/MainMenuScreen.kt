package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CampaignSnapshot
import com.example.ui.GameScreenRoute
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
fun MainMenuScreen(
    campaign: CampaignSnapshot?,
    onDeploy: () -> Unit,
    onNavigate: (GameScreenRoute) -> Unit,
    onToggleSound: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_lost_earth),
            contentDescription = "Lost Earth ruined city backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TacticalObsidian.copy(alpha = 0.92f),
                            TacticalObsidian.copy(alpha = 0.65f),
                            TacticalObsidian.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT COLUMN: Game Identity, Civilization Meter & Primary Deploy Action
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = AmberTactical.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, AmberTactical)
                        ) {
                            Text(
                                text = "OFFLINE TACTICAL SURVIVAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberTactical,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (campaign != null) {
                            IconButton(
                                onClick = onToggleSound,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (campaign.profile.soundEnabled) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = "Toggle Sound",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "LOST EARTH",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontSize = 42.sp
                    )
                    Text(
                        text = "RECLAIM THE RUINED CITIES. REBUILD CIVILIZATION.",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberTactical,
                        fontSize = 13.sp
                    )
                }

                if (campaign != null) {
                    Surface(
                        color = GunmetalDark.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${campaign.rankTitle.uppercase()} // ${campaign.profile.commanderName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SteelCyan
                                )
                                Text(
                                    text = "REBUILD ${campaign.civilizationProgressPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldVital,
                                    fontSize = 12.sp
                                )
                            }
                            LinearProgressIndicator(
                                progress = { campaign.civilizationProgressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldVital,
                                trackColor = GunmetalSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBadge("SURVIVORS", "${campaign.profile.survivorsRescued}")
                                StatBadge("OUTPOSTS", "${campaign.outposts.count { it.captured }}/${campaign.outposts.size}")
                                StatBadge("SQUAD", "${campaign.deployedWarriors.size}/3")
                                StatBadge("HOSTILES KIA", "${campaign.profile.kills}")
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onDeploy,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberTactical),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("deploy_button")
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Deploy",
                            tint = TacticalObsidian,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "DEPLOY TO OPEN WORLD",
                            style = MaterialTheme.typography.titleMedium,
                            color = TacticalObsidian,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "CONTROLS: Right Screen = Movement • Left Screen = Aim, Weapons & Tactical Gear",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // RIGHT COLUMN: Command Navigation Tiles + Resource Summary
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (campaign != null) {
                    ResourceTickerRow(campaign.profile)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LobbyTile(
                            title = "SANCTUARY VILLAGE",
                            subtitle = "Build walls, clinics & shelter",
                            icon = Icons.Filled.Apartment,
                            accent = EmeraldVital,
                            tag = "menu_village_button",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(GameScreenRoute.VILLAGE_HUB) }
                        )
                        LobbyTile(
                            title = "ARMORY & ARMOR",
                            subtitle = "Customize 3D suit & rifles",
                            icon = Icons.Filled.Shield,
                            accent = AmberTactical,
                            tag = "menu_armory_button",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(GameScreenRoute.ARMORY) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LobbyTile(
                            title = "WARRIOR SQUAD",
                            subtitle = "Recruit & deploy fighters",
                            icon = Icons.Filled.Groups,
                            accent = SteelCyan,
                            tag = "menu_squad_button",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(GameScreenRoute.SQUAD) }
                        )
                        LobbyTile(
                            title = "HISTORY CHRONICLE",
                            subtitle = "Story, outposts & legacy",
                            icon = Icons.Filled.HistoryEdu,
                            accent = Color(0xFFF97316),
                            tag = "menu_chronicle_button",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(GameScreenRoute.CHRONICLE) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun LobbyTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(104.dp)
            .testTag(tag),
        color = GunmetalDark.copy(alpha = 0.88f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
