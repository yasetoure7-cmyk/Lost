package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaign_profile")
data class CampaignProfileEntity(
    @PrimaryKey val id: Int = 1,
    val commanderName: String = "Commander Vex",
    val xp: Int = 0,
    val kills: Int = 0,
    val survivorsRescued: Int = 6,
    val scrap: Int = 85,
    val meds: Int = 30,
    val ammo: Int = 60,
    val fuel: Int = 25,
    val bioSamples: Int = 8,
    val equippedHelmet: String = ArmorHelmet.RECON_VISOR.name,
    val equippedVest: String = ArmorVest.LIGHT_CARRIER.name,
    val equippedCamo: String = ArmorCamo.URBAN_ASH.name,
    val heavyPauldrons: Boolean = true,
    val tacticalPack: Boolean = true,
    val unlockedHelmets: String = ArmorHelmet.RECON_VISOR.name,
    val unlockedVests: String = ArmorVest.LIGHT_CARRIER.name,
    val unlockedCamos: String = ArmorCamo.URBAN_ASH.name,
    val primaryWeapon: String = WeaponType.ASSAULT_RIFLE.name,
    val secondaryWeapon: String = WeaponType.TACTICAL_SHOTGUN.name,
    val soundEnabled: Boolean = true
)

@Entity(tableName = "village_buildings")
data class VillageBuildingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val level: Int,
    val maxLevel: Int,
    val baseScrapCost: Int,
    val baseFuelCost: Int,
    val bonusText: String
)

@Entity(tableName = "outposts")
data class OutpostEntity(
    @PrimaryKey val id: String,
    val name: String,
    val zoneName: String,
    val worldX: Float,
    val worldZ: Float,
    val captured: Boolean,
    val defenseLevel: Int,
    val loreNote: String
)

@Entity(tableName = "warriors")
data class WarriorEntity(
    @PrimaryKey val id: String,
    val callsign: String,
    val fullName: String,
    val specialty: String,
    val bio: String,
    val weaponName: String,
    val accentHex: Long,
    val recruited: Boolean,
    val deployed: Boolean,
    val level: Int,
    val recruitScrapCost: Int,
    val recruitMedCost: Int
)

@Entity(tableName = "history_chronicle")
data class HistoryEntryEntity(
    @PrimaryKey val id: String,
    val chapterTitle: String,
    val eraLabel: String,
    val narrative: String,
    val unlocked: Boolean,
    val sortOrder: Int
)
