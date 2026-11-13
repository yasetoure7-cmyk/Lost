package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TacticalSoundEngine
import com.example.data.ArmorCamo
import com.example.data.ArmorHelmet
import com.example.data.ArmorVest
import com.example.data.CampaignRepository
import com.example.data.CampaignSnapshot
import com.example.data.LostEarthDatabase
import com.example.data.WeaponType
import com.example.game.GameEngine
import com.example.game.GameEventCallbacks
import com.example.game.HudState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GameScreenRoute {
    MAIN_MENU,
    PLAYING,
    VILLAGE_HUB,
    ARMORY,
    SQUAD,
    CHRONICLE
}

class LostEarthViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LostEarthDatabase.getInstance(application).gameDao()
    private val repository = CampaignRepository(dao)
    val soundEngine = TacticalSoundEngine()

    private val _currentScreen = MutableStateFlow(GameScreenRoute.MAIN_MENU)
    val currentScreen: StateFlow<GameScreenRoute> = _currentScreen.asStateFlow()

    private val _campaign = MutableStateFlow<CampaignSnapshot?>(null)
    val campaign: StateFlow<CampaignSnapshot?> = _campaign.asStateFlow()

    private val _hudState = MutableStateFlow(HudState())
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var worldInitialized = false

    val gameEngine: GameEngine = GameEngine(
        soundEngine = soundEngine,
        callbacks = object : GameEventCallbacks {
            override fun onRewardsEarned(
                scrap: Int,
                meds: Int,
                ammo: Int,
                fuel: Int,
                bio: Int,
                kills: Int,
                xp: Int
            ) {
                viewModelScope.launch {
                    repository.recordCombatRewards(scrap, meds, ammo, fuel, bio, kills, xp)
                }
            }

            override fun onSurvivorRescued(survivorName: String) {
                viewModelScope.launch {
                    repository.recordSurvivorRescued(survivorName)
                }
            }

            override fun onOutpostCaptured(outpostId: String) {
                viewModelScope.launch {
                    repository.captureOutpost(outpostId)
                }
            }
        }
    )

    init {
        gameEngine.onHudUpdated = { snapshot ->
            _hudState.value = snapshot
        }
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.campaignFlow.collect { snap ->
                _campaign.value = snap
                soundEngine.enabled = snap.profile.soundEnabled
                if (worldInitialized) {
                    gameEngine.syncArmoryAndSquad(
                        loadout = snap.armorLoadout,
                        primary = snap.primaryWeapon,
                        secondary = snap.secondaryWeapon,
                        deployedWarriors = snap.deployedWarriors
                    )
                }
            }
        }
    }

    fun navigateTo(route: GameScreenRoute) {
        if (route == GameScreenRoute.PLAYING && !worldInitialized) {
            deployToWorld()
            return
        }
        _currentScreen.value = route
    }

    fun deployToWorld() {
        val snap = _campaign.value ?: return
        val armoryLvl = snap.buildings.firstOrNull { it.id == "b_armory" }?.level ?: 1
        val medbayLvl = snap.buildings.firstOrNull { it.id == "b_medbay" }?.level ?: 1
        val watchtowerLvl = snap.buildings.firstOrNull { it.id == "b_watchtower" }?.level ?: 1
        val solarLvl = snap.buildings.firstOrNull { it.id == "b_solar" }?.level ?: 1

        gameEngine.initializeWorld(
            outpostEntities = snap.outposts,
            deployedWarriors = snap.deployedWarriors,
            loadout = snap.armorLoadout,
            primary = snap.primaryWeapon,
            secondary = snap.secondaryWeapon,
            armoryLevel = armoryLvl,
            medbayLevel = medbayLvl,
            watchtowerLevel = watchtowerLvl,
            solarLevel = solarLvl
        )
        worldInitialized = true
        _currentScreen.value = GameScreenRoute.PLAYING
    }

    fun upgradeBuilding(id: String) {
        viewModelScope.launch {
            val ok = repository.upgradeBuilding(id)
            if (ok) {
                soundEngine.playSuccessChime()
                showBanner("Structure upgraded! Sanctuary defenses reinforced.")
            } else {
                showBanner("Insufficient Scrap or Fuel resources.")
            }
        }
    }

    fun selectHelmet(helmet: ArmorHelmet) {
        viewModelScope.launch {
            val ok = repository.unlockOrEquipHelmet(helmet)
            if (ok) soundEngine.playReload() else showBanner("Need ${helmet.scrapCost} Scrap to forge.")
        }
    }

    fun selectVest(vest: ArmorVest) {
        viewModelScope.launch {
            val ok = repository.unlockOrEquipVest(vest)
            if (ok) soundEngine.playReload() else showBanner("Need ${vest.scrapCost} Scrap to forge.")
        }
    }

    fun selectCamo(camo: ArmorCamo) {
        viewModelScope.launch {
            val ok = repository.unlockOrEquipCamo(camo)
            if (ok) soundEngine.playReload() else showBanner("Need ${camo.scrapCost} Scrap to unlock.")
        }
    }

    fun togglePauldrons(enabled: Boolean) {
        viewModelScope.launch { repository.toggleArmorAttachment(pauldrons = enabled) }
    }

    fun togglePack(enabled: Boolean) {
        viewModelScope.launch { repository.toggleArmorAttachment(pack = enabled) }
    }

    fun selectWeapons(primary: WeaponType, secondary: WeaponType) {
        viewModelScope.launch {
            repository.setLoadoutWeapons(primary, secondary)
            soundEngine.playReload()
        }
    }

    fun recruitWarrior(warriorId: String) {
        viewModelScope.launch {
            val ok = repository.recruitWarrior(warriorId)
            if (ok) {
                soundEngine.playSuccessChime()
                showBanner("Warrior recruited to the Vanguard Squad!")
            } else {
                showBanner("Insufficient Scrap or Med-Supplies to recruit.")
            }
        }
    }

    fun toggleWarriorDeployed(warriorId: String) {
        viewModelScope.launch {
            repository.toggleWarriorDeploy(warriorId)
            soundEngine.playReload()
        }
    }

    fun upgradeWarrior(warriorId: String) {
        viewModelScope.launch {
            val ok = repository.upgradeWarrior(warriorId)
            if (ok) {
                soundEngine.playSuccessChime()
                showBanner("Squad warrior combat training upgraded!")
            } else {
                showBanner("Not enough Scrap or already max level.")
            }
        }
    }

    fun toggleSound() {
        val current = _campaign.value?.profile?.soundEnabled ?: true
        viewModelScope.launch { repository.setSoundEnabled(!current) }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    private fun showBanner(msg: String) {
        _statusBanner.value = msg
    }
}
