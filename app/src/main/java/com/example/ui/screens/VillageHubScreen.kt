package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CampaignSnapshot
import com.example.data.VillageBuildingEntity
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
fun VillageHubScreen(
    campaign: CampaignSnapshot,
    onUpgradeBuilding: (String) -> Unit,
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
            title = "SANCTUARY VALLEY VILLAGE",
            subtitle = "POPULATION ${campaign.profile.survivorsRescued} CITIZENS • DEFENSE RATING ${campaign.villageDefenseRating}",
            profile = campaign.profile,
            onBack = onBack
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Village Banner & Overview
            Surface(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                color = GunmetalDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_sanctuary_village),
                            contentDescription = "Sanctuary Valley Protected Village",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(androidx.compose.ui.graphics.Color.Transparent, GunmetalDark)
                                    )
                                )
                        )
                        Text(
                            text = "PROTECTED ZONE // SECTOR 0",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldVital,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "REBUILDING CIVILIZATION",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Rescue stranded survivors from ruined city blocks and upgrade village fortifications to boost squad firepower, armor capacity, and medical supplies.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "OUTPOSTS LINKED: ${campaign.outposts.count { it.captured }} / ${campaign.outposts.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SteelCyan
                        )
                    }
                }
            }

            // Right Upgradeable Buildings Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(campaign.buildings, key = { it.id }) { building ->
                    BuildingUpgradeCard(
                        building = building,
                        scrapAvailable = campaign.profile.scrap,
                        fuelAvailable = campaign.profile.fuel,
                        onUpgrade = { onUpgradeBuilding(building.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildingUpgradeCard(
    building: VillageBuildingEntity,
    scrapAvailable: Int,
    fuelAvailable: Int,
    onUpgrade: () -> Unit
) {
    val isMax = building.level >= building.maxLevel
    val scrapCost = building.baseScrapCost * (building.level + 1)
    val fuelCost = building.baseFuelCost * (building.level + 1)
    val canAfford = !isMax && scrapAvailable >= scrapCost && fuelAvailable >= fuelCost

    Surface(
        color = GunmetalSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = building.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "LVL ${building.level}/${building.maxLevel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberTactical,
                    fontSize = 12.sp
                )
            }
            Text(
                text = building.role,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = building.bonusText,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldVital
            )
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberTactical,
                    disabledContainerColor = GunmetalDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("upgrade_${building.id}")
            ) {
                Text(
                    text = if (isMax) "MAX FORTIFIED" else "UPGRADE ($scrapCost SCRAP • $fuelCost FUEL)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canAfford) TacticalObsidian else TextSecondary
                )
            }
        }
    }
}
