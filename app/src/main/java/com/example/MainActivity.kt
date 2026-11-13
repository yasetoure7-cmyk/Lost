package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GameScreenRoute
import com.example.ui.LostEarthViewModel
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.ChronicleScreen
import com.example.ui.screens.GameCombatScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SquadScreen
import com.example.ui.screens.VillageHubScreen
import com.example.ui.theme.AmberTactical
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.LostEarthTheme
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveLandscape()

        setContent {
            LostEarthTheme {
                val vm: LostEarthViewModel = viewModel()
                val route by vm.currentScreen.collectAsStateWithLifecycle()
                val campaign by vm.campaign.collectAsStateWithLifecycle()
                val hud by vm.hudState.collectAsStateWithLifecycle()
                val banner by vm.statusBanner.collectAsStateWithLifecycle()

                LaunchedEffect(banner) {
                    if (banner != null) {
                        delay(2600L)
                        vm.dismissBanner()
                    }
                }

                if (route != GameScreenRoute.MAIN_MENU) {
                    BackHandler {
                        vm.navigateTo(GameScreenRoute.MAIN_MENU)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (route) {
                        GameScreenRoute.MAIN_MENU -> {
                            MainMenuScreen(
                                campaign = campaign,
                                onDeploy = { vm.deployToWorld() },
                                onNavigate = { vm.navigateTo(it) },
                                onToggleSound = { vm.toggleSound() }
                            )
                        }
                        GameScreenRoute.PLAYING -> {
                            GameCombatScreen(
                                engine = vm.gameEngine,
                                hud = hud,
                                onNavigate = { vm.navigateTo(it) }
                            )
                        }
                        GameScreenRoute.VILLAGE_HUB -> {
                            campaign?.let { snap ->
                                VillageHubScreen(
                                    campaign = snap,
                                    onUpgradeBuilding = { vm.upgradeBuilding(it) },
                                    onBack = { vm.navigateTo(GameScreenRoute.MAIN_MENU) }
                                )
                            }
                        }
                        GameScreenRoute.ARMORY -> {
                            campaign?.let { snap ->
                                ArmoryScreen(
                                    campaign = snap,
                                    onSelectHelmet = { vm.selectHelmet(it) },
                                    onSelectVest = { vm.selectVest(it) },
                                    onSelectCamo = { vm.selectCamo(it) },
                                    onTogglePauldrons = { vm.togglePauldrons(it) },
                                    onTogglePack = { vm.togglePack(it) },
                                    onSelectWeapons = { p, s -> vm.selectWeapons(p, s) },
                                    onBack = { vm.navigateTo(GameScreenRoute.MAIN_MENU) }
                                )
                            }
                        }
                        GameScreenRoute.SQUAD -> {
                            campaign?.let { snap ->
                                SquadScreen(
                                    campaign = snap,
                                    onRecruit = { vm.recruitWarrior(it) },
                                    onToggleDeploy = { vm.toggleWarriorDeployed(it) },
                                    onUpgrade = { vm.upgradeWarrior(it) },
                                    onBack = { vm.navigateTo(GameScreenRoute.MAIN_MENU) }
                                )
                            }
                        }
                        GameScreenRoute.CHRONICLE -> {
                            campaign?.let { snap ->
                                ChronicleScreen(
                                    campaign = snap,
                                    onBack = { vm.navigateTo(GameScreenRoute.MAIN_MENU) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = banner != null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    ) {
                        Surface(
                            color = GunmetalDark.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberTactical)
                        ) {
                            Text(
                                text = banner.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureImmersiveLandscape() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
