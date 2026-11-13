package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

data class CampaignSnapshot(
    val profile: CampaignProfileEntity,
    val buildings: List<VillageBuildingEntity>,
    val outposts: List<OutpostEntity>,
    val warriors: List<WarriorEntity>,
    val chronicle: List<HistoryEntryEntity>
) {
    val armorLoadout: ArmorLoadout
        get() = ArmorLoadout(
            helmet = runCatching { ArmorHelmet.valueOf(profile.equippedHelmet) }
                .getOrDefault(ArmorHelmet.RECON_VISOR),
            vest = runCatching { ArmorVest.valueOf(profile.equippedVest) }
                .getOrDefault(ArmorVest.LIGHT_CARRIER),
            camo = runCatching { ArmorCamo.valueOf(profile.equippedCamo) }
                .getOrDefault(ArmorCamo.URBAN_ASH),
            heavyPauldrons = profile.heavyPauldrons,
            tacticalPack = profile.tacticalPack
        )

    val primaryWeapon: WeaponType
        get() = runCatching { WeaponType.valueOf(profile.primaryWeapon) }
            .getOrDefault(WeaponType.ASSAULT_RIFLE)

    val secondaryWeapon: WeaponType
        get() = runCatching { WeaponType.valueOf(profile.secondaryWeapon) }
            .getOrDefault(WeaponType.TACTICAL_SHOTGUN)

    val deployedWarriors: List<WarriorEntity>
        get() = warriors.filter { it.recruited && it.deployed }

    val villageDefenseRating: Int
        get() = buildings.sumOf { it.level * 18 } + outposts.count { it.captured } * 25

    val civilizationProgressPercent: Int
        get() {
            val outpostScore = outposts.count { it.captured } * 12
            val buildingScore = buildings.sumOf { it.level } * 3
            val survivorScore = (profile.survivorsRescued * 2).coerceAtMost(24)
            return (outpostScore + buildingScore + survivorScore).coerceIn(8, 100)
        }

    val rankTitle: String
        get() = when {
            profile.xp >= 2500 -> "Marshal of New Earth"
            profile.xp >= 1500 -> "Vanguard Commander"
            profile.xp >= 800 -> "Tactical Captain"
            profile.xp >= 300 -> "Field Sergeant"
            else -> "Pathfinder"
        }
}

class CampaignRepository(private val dao: GameDao) {

    val campaignFlow: Flow<CampaignSnapshot> = combine(
        dao.observeProfile().filterNotNull(),
        dao.observeBuildings(),
        dao.observeOutposts(),
        dao.observeWarriors(),
        dao.observeChronicle()
    ) { profile, buildings, outposts, warriors, chronicle ->
        CampaignSnapshot(profile, buildings, outposts, warriors, chronicle)
    }

    suspend fun ensureSeeded() {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(CampaignProfileEntity())
        }
        if (dao.getBuildingsOnce().isEmpty()) {
            dao.upsertBuildings(defaultBuildings())
        }
        if (dao.getOutpostsOnce().isEmpty()) {
            dao.upsertOutposts(defaultOutposts())
        }
        if (dao.getWarriorsOnce().isEmpty()) {
            dao.upsertWarriors(defaultWarriors())
        }
        if (dao.getChronicleOnce().isEmpty()) {
            dao.upsertChronicle(defaultChronicle())
        }
    }

    suspend fun recordCombatRewards(
        scrapGain: Int,
        medGain: Int,
        ammoGain: Int,
        fuelGain: Int,
        bioGain: Int,
        killsGain: Int,
        xpGain: Int
    ) {
        val current = dao.getProfileOnce() ?: return
        dao.upsertProfile(
            current.copy(
                scrap = current.scrap + scrapGain,
                meds = current.meds + medGain,
                ammo = current.ammo + ammoGain,
                fuel = current.fuel + fuelGain,
                bioSamples = current.bioSamples + bioGain,
                kills = current.kills + killsGain,
                xp = current.xp + xpGain
            )
        )
        if (current.kills + killsGain >= 15) {
            dao.unlockHistoryEntry("hist_first_blood")
        }
    }

    suspend fun recordSurvivorRescued(survivorName: String) {
        val current = dao.getProfileOnce() ?: return
        val newCount = current.survivorsRescued + 1
        dao.upsertProfile(
            current.copy(
                survivorsRescued = newCount,
                xp = current.xp + 90,
                meds = current.meds + 8,
                scrap = current.scrap + 15
            )
        )
        if (newCount >= 8) {
            dao.unlockHistoryEntry("hist_village_rising")
        }
    }

    suspend fun captureOutpost(outpostId: String) {
        val outposts = dao.getOutpostsOnce()
        val target = outposts.firstOrNull { it.id == outpostId } ?: return
        if (!target.captured) {
            dao.upsertOutpost(target.copy(captured = true, defenseLevel = 1))
            val current = dao.getProfileOnce() ?: return
            dao.upsertProfile(
                current.copy(
                    xp = current.xp + 220,
                    scrap = current.scrap + 45,
                    fuel = current.fuel + 15,
                    ammo = current.ammo + 35
                )
            )
            dao.unlockHistoryEntry("hist_outpost_reclaimed")
            val allCaptured = outposts.count { it.captured || it.id == outpostId } == outposts.size
            if (allCaptured) {
                dao.unlockHistoryEntry("hist_new_dawn")
            }
            if (outpostId == "outpost_biocore") {
                dao.unlockHistoryEntry("hist_cure_recovered")
            }
        }
    }

    suspend fun upgradeBuilding(buildingId: String): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val building = dao.getBuildingsOnce().firstOrNull { it.id == buildingId } ?: return false
        if (building.level >= building.maxLevel) return false
        val scrapCost = building.baseScrapCost * (building.level + 1)
        val fuelCost = building.baseFuelCost * (building.level + 1)
        if (profile.scrap < scrapCost || profile.fuel < fuelCost) return false

        dao.upsertProfile(
            profile.copy(
                scrap = profile.scrap - scrapCost,
                fuel = profile.fuel - fuelCost,
                xp = profile.xp + 75
            )
        )
        dao.upsertBuilding(building.copy(level = building.level + 1))
        dao.unlockHistoryEntry("hist_village_rising")
        return true
    }

    suspend fun unlockOrEquipHelmet(helmet: ArmorHelmet): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val unlocked = profile.unlockedHelmets.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!unlocked.contains(helmet.name)) {
            if (profile.scrap < helmet.scrapCost) return false
            unlocked.add(helmet.name)
            dao.upsertProfile(
                profile.copy(
                    scrap = profile.scrap - helmet.scrapCost,
                    unlockedHelmets = unlocked.joinToString(","),
                    equippedHelmet = helmet.name
                )
            )
        } else {
            dao.upsertProfile(profile.copy(equippedHelmet = helmet.name))
        }
        return true
    }

    suspend fun unlockOrEquipVest(vest: ArmorVest): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val unlocked = profile.unlockedVests.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!unlocked.contains(vest.name)) {
            if (profile.scrap < vest.scrapCost) return false
            unlocked.add(vest.name)
            dao.upsertProfile(
                profile.copy(
                    scrap = profile.scrap - vest.scrapCost,
                    unlockedVests = unlocked.joinToString(","),
                    equippedVest = vest.name
                )
            )
        } else {
            dao.upsertProfile(profile.copy(equippedVest = vest.name))
        }
        return true
    }

    suspend fun unlockOrEquipCamo(camo: ArmorCamo): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val unlocked = profile.unlockedCamos.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!unlocked.contains(camo.name)) {
            if (profile.scrap < camo.scrapCost) return false
            unlocked.add(camo.name)
            dao.upsertProfile(
                profile.copy(
                    scrap = profile.scrap - camo.scrapCost,
                    unlockedCamos = unlocked.joinToString(","),
                    equippedCamo = camo.name
                )
            )
        } else {
            dao.upsertProfile(profile.copy(equippedCamo = camo.name))
        }
        return true
    }

    suspend fun toggleArmorAttachment(pauldrons: Boolean? = null, pack: Boolean? = null) {
        val profile = dao.getProfileOnce() ?: return
        dao.upsertProfile(
            profile.copy(
                heavyPauldrons = pauldrons ?: profile.heavyPauldrons,
                tacticalPack = pack ?: profile.tacticalPack
            )
        )
    }

    suspend fun setLoadoutWeapons(primary: WeaponType, secondary: WeaponType) {
        val profile = dao.getProfileOnce() ?: return
        dao.upsertProfile(
            profile.copy(
                primaryWeapon = primary.name,
                secondaryWeapon = secondary.name
            )
        )
    }

    suspend fun recruitWarrior(warriorId: String): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val warriors = dao.getWarriorsOnce()
        val target = warriors.firstOrNull { it.id == warriorId } ?: return false
        if (target.recruited) return true
        if (profile.scrap < target.recruitScrapCost || profile.meds < target.recruitMedCost) {
            return false
        }
        val activeCount = warriors.count { it.recruited && it.deployed }
        dao.upsertProfile(
            profile.copy(
                scrap = profile.scrap - target.recruitScrapCost,
                meds = profile.meds - target.recruitMedCost,
                xp = profile.xp + 110
            )
        )
        dao.upsertWarrior(target.copy(recruited = true, deployed = activeCount < 3))
        dao.unlockHistoryEntry("hist_brotherhood")
        return true
    }

    suspend fun toggleWarriorDeploy(warriorId: String) {
        val warriors = dao.getWarriorsOnce()
        val target = warriors.firstOrNull { it.id == warriorId } ?: return
        if (!target.recruited) return
        if (!target.deployed && warriors.count { it.recruited && it.deployed } >= 3) return
        dao.upsertWarrior(target.copy(deployed = !target.deployed))
    }

    suspend fun upgradeWarrior(warriorId: String): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val target = dao.getWarriorsOnce().firstOrNull { it.id == warriorId } ?: return false
        if (!target.recruited || target.level >= 5) return false
        val cost = 35 * target.level
        if (profile.scrap < cost) return false
        dao.upsertProfile(profile.copy(scrap = profile.scrap - cost, xp = profile.xp + 60))
        dao.upsertWarrior(target.copy(level = target.level + 1))
        return true
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        val profile = dao.getProfileOnce() ?: return
        dao.upsertProfile(profile.copy(soundEnabled = enabled))
    }

    private fun defaultBuildings() = listOf(
        VillageBuildingEntity(
            id = "b_wall",
            name = "Reinforced Perimeter Wall",
            role = "Blocks infected breaches around Sanctuary Valley",
            level = 1,
            maxLevel = 5,
            baseScrapCost = 30,
            baseFuelCost = 8,
            bonusText = "+18 Village Defense / lvl"
        ),
        VillageBuildingEntity(
            id = "b_watchtower",
            name = "Sentry Watchtowers",
            role = "Elevated sniper overwatch covering valley roads",
            level = 1,
            maxLevel = 5,
            baseScrapCost = 35,
            baseFuelCost = 10,
            bonusText = "+10% Squad Firepower / lvl"
        ),
        VillageBuildingEntity(
            id = "b_medbay",
            name = "Field Medbay Clinic",
            role = "Synthesizes combat stims & heals rescued citizens",
            level = 1,
            maxLevel = 5,
            baseScrapCost = 28,
            baseFuelCost = 12,
            bonusText = "+1 Med-Stim Capacity / lvl"
        ),
        VillageBuildingEntity(
            id = "b_armory",
            name = "Tactical Armory Forge",
            role = "Refits ballistic armor plates and weapon barrels",
            level = 1,
            maxLevel = 5,
            baseScrapCost = 40,
            baseFuelCost = 10,
            bonusText = "+15 Max Armor / lvl"
        ),
        VillageBuildingEntity(
            id = "b_solar",
            name = "Solar Micro-Grid",
            role = "Powers beacon relays, water purifiers & floodlights",
            level = 1,
            maxLevel = 5,
            baseScrapCost = 25,
            baseFuelCost = 15,
            bonusText = "+5 Frag & Flare Supply"
        ),
        VillageBuildingEntity(
            id = "b_barracks",
            name = "Warrior Command Barracks",
            role = "Trains recruited survivors into combat specialists",
            level = 1,
            maxLevel = 5,
            baseScrapCost = 32,
            baseFuelCost = 8,
            bonusText = "Boosts Squad HP & Accuracy"
        )
    )

    private fun defaultOutposts() = listOf(
        OutpostEntity(
            id = "outpost_ashfall",
            name = "Ashfall Metro Relay",
            zoneName = WorldZone.ASHFALL_DOWNTOWN.title,
            worldX = -48f,
            worldZ = -12f,
            captured = false,
            defenseLevel = 0,
            loreNote = "Former emergency broadcast plaza overlooking collapsed skyscrapers."
        ),
        OutpostEntity(
            id = "outpost_harbor",
            name = "Harbor Pier Bravo",
            zoneName = WorldZone.HARBOR_QUARANTINE.title,
            worldX = 50f,
            worldZ = -14f,
            captured = false,
            defenseLevel = 0,
            loreNote = "Abandoned UN cargo terminal stocked with fuel drums and steel."
        ),
        OutpostEntity(
            id = "outpost_overpass",
            name = "North Highway Checkpoint",
            zoneName = WorldZone.ASHFALL_DOWNTOWN.title,
            worldX = -18f,
            worldZ = -44f,
            captured = false,
            defenseLevel = 0,
            loreNote = "Fortified military blockade guarding the approach to Ground Zero."
        ),
        OutpostEntity(
            id = "outpost_biocore",
            name = "Bio-Core Research Spire",
            zoneName = WorldZone.BIOCORE_SPIRE.title,
            worldX = 0f,
            worldZ = -74f,
            captured = false,
            defenseLevel = 0,
            loreNote = "Original outbreak laboratory holding intact vaccine synthesis data."
        )
    )

    private fun defaultWarriors() = listOf(
        WarriorEntity(
            id = "w_elena",
            callsign = "SPECTER",
            fullName = "Sgt. Elena Vance",
            specialty = "Recon Marksman",
            bio = "Former airborne sniper who held Ashfall bridge alone for four days.",
            weaponName = "SR-25 Sentinel",
            accentHex = 0xFF38BDF8,
            recruited = true,
            deployed = true,
            level = 1,
            recruitScrapCost = 0,
            recruitMedCost = 0
        ),
        WarriorEntity(
            id = "w_marcus",
            callsign = "ANVIL",
            fullName = "Marcus Cole",
            specialty = "Heavy Support Gunner",
            bio = "Combat engineer clad in salvaged riot plating. Unmatched suppressive fire.",
            weaponName = "M4A1 Vanguard",
            accentHex = 0xFFF59E0B,
            recruited = false,
            deployed = false,
            level = 1,
            recruitScrapCost = 45,
            recruitMedCost = 10
        ),
        WarriorEntity(
            id = "w_maya",
            callsign = "PULSE",
            fullName = "Dr. Maya Lin",
            specialty = "Field Combat Medic",
            bio = "Virologist & trauma surgeon keeping the squad alive during protracted sieges.",
            weaponName = "M9A3 Sidearm",
            accentHex = 0xFF22C55E,
            recruited = false,
            deployed = false,
            level = 1,
            recruitScrapCost = 40,
            recruitMedCost = 15
        ),
        WarriorEntity(
            id = "w_kai",
            callsign = "BREACH",
            fullName = "Kai Mercer",
            specialty = "CQB Assault Breacher",
            bio = "Close-quarters specialist trained for clearing dense infected city blocks.",
            weaponName = "SPAS-12 Breacher",
            accentHex = 0xFFEF4444,
            recruited = false,
            deployed = false,
            level = 1,
            recruitScrapCost = 65,
            recruitMedCost = 12
        )
    )

    private fun defaultChronicle() = listOf(
        HistoryEntryEntity(
            id = "hist_awakening",
            chapterTitle = "Chapter I: Ashes of Old Earth",
            eraLabel = "DAY 01 // SANCTUARY VALLEY",
            narrative = "After the global pathogen collapse, Commander Vex established the first timber-and-concrete wall in Sanctuary Valley, swearing to unite scattered survivors and retake the ruined cities.",
            unlocked = true,
            sortOrder = 1
        ),
        HistoryEntryEntity(
            id = "hist_first_blood",
            chapterTitle = "Chapter II: Line in the Dust",
            eraLabel = "COMBAT MILESTONE",
            narrative = "Fifteen infected hostiles neutralized beyond the perimeter. Word spreads across emergency radio channels that an organized tactical unit is fighting back.",
            unlocked = false,
            sortOrder = 2
        ),
        HistoryEntryEntity(
            id = "hist_brotherhood",
            chapterTitle = "Chapter III: The Vanguard Squad",
            eraLabel = "SQUADRON FORMED",
            narrative = "Warriors from fallen regiments answered the call, forging a disciplined fireteam capable of pushing deep into quarantined metropolitan zones.",
            unlocked = false,
            sortOrder = 3
        ),
        HistoryEntryEntity(
            id = "hist_village_rising",
            chapterTitle = "Chapter IV: Sanctuary Rising",
            eraLabel = "CIVILIZATION REBUILT",
            narrative = "Rescued families and engineers fortified Sanctuary Village with solar relays, watchtowers, and a working clinic—proving humanity can thrive again.",
            unlocked = false,
            sortOrder = 4
        ),
        HistoryEntryEntity(
            id = "hist_outpost_reclaimed",
            chapterTitle = "Chapter V: Reclaiming the Ruins",
            eraLabel = "OUTPOST SECURED",
            narrative = "An abandoned urban relay was purged of infected hordes and brought back online, extending Sanctuary's safe corridor across the dead city.",
            unlocked = false,
            sortOrder = 5
        ),
        HistoryEntryEntity(
            id = "hist_cure_recovered",
            chapterTitle = "Chapter VI: Echoes of Bio-Core",
            eraLabel = "GROUND ZERO SECURED",
            narrative = "Inside the Bio-Core Spire ruins, Vex's squad secured intact research servers—unlocking the blueprint to end the plague forever.",
            unlocked = false,
            sortOrder = 6
        ),
        HistoryEntryEntity(
            id = "hist_new_dawn",
            chapterTitle = "Chapter VII: History Written in Steel",
            eraLabel = "WORLD RESTORED",
            narrative = "Every major sector outpost flies the Sanctuary banner. From ruined skyscrapers to harbor piers, humanity's new civilization stands unbroken.",
            unlocked = false,
            sortOrder = 7
        )
    )
}
