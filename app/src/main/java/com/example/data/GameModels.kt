package com.example.data

enum class CameraPerspective(val label: String) {
    THIRD_PERSON("3RD PERSON"),
    FIRST_PERSON("1ST PERSON")
}

enum class PlayerStance {
    IDLE,
    WALK,
    SPRINT,
    CROUCH,
    AIM
}

enum class ArmorHelmet(
    val displayName: String,
    val armorBonus: Int,
    val scrapCost: Int,
    val description: String
) {
    RECON_VISOR("Recon Tactical Visor", 15, 0, "Lightweight HUD-integrated ballistic visor."),
    OPS_BALLISTIC("Ops-Core Ballistic", 30, 40, "High-cut Kevlar helmet with night-optic mount."),
    JUGGERNAUT_HELM("Vanguard Heavy Helm", 55, 95, "Full-face reinforced titanium assault helmet."),
    GHILLIE_HOOD("Ghost Recon Hood", 25, 65, "Camouflaged ballistic cowl for urban ruins.")
}

enum class ArmorVest(
    val displayName: String,
    val armorBonus: Int,
    val speedFactor: Float,
    val scrapCost: Int,
    val description: String
) {
    LIGHT_CARRIER("Light Plate Carrier", 25, 1.05f, 0, "Agile tactical rig built for rapid urban traversal."),
    ASSAULT_KEVLAR("Assault Kevlar Rig", 50, 1.0f, 55, "Balanced trauma plates with modular ammo pouches."),
    EXO_REINFORCED("Titan Exo-Armor", 85, 0.94f, 120, "Heavy mechanized chest plating against Brute impacts.")
}

enum class ArmorCamo(
    val displayName: String,
    val primaryHex: Long,
    val accentHex: Long,
    val scrapCost: Int
) {
    URBAN_ASH("Urban Ash", 0xFF475569, 0xFF94A3B8, 0),
    OLIVE_DRAB("Ranger Olive", 0xFF4D5D3B, 0xFF849B68, 25),
    NIGHT_CARBON("Night Ops Carbon", 0xFF1E242B, 0xFF38BDF8, 45),
    DESERT_SAND("Dune Tactical", 0xFF8A7557, 0xFFD6B98C, 45),
    CRIMSON_VANGUARD("Crimson Vanguard", 0xFF5C1D24, 0xFFF59E0B, 80)
}

data class ArmorLoadout(
    val helmet: ArmorHelmet = ArmorHelmet.RECON_VISOR,
    val vest: ArmorVest = ArmorVest.LIGHT_CARRIER,
    val camo: ArmorCamo = ArmorCamo.URBAN_ASH,
    val heavyPauldrons: Boolean = true,
    val tacticalPack: Boolean = true
) {
    val maxArmor: Int
        get() = helmet.armorBonus + vest.armorBonus + if (heavyPauldrons) 15 else 0
}

enum class WeaponType(
    val displayName: String,
    val shortCode: String,
    val damage: Int,
    val fireRateMs: Long,
    val magSize: Int,
    val startReserve: Int,
    val maxReserve: Int,
    val rangeMeters: Float,
    val spreadDeg: Float,
    val pellets: Int,
    val reloadMs: Long,
    val adsFov: Float,
    val lengthScale: Float,
    val barrelColorHex: Long
) {
    ASSAULT_RIFLE(
        displayName = "M4A1 Vanguard",
        shortCode = "AR-M4",
        damage = 34,
        fireRateMs = 115L,
        magSize = 30,
        startReserve = 180,
        maxReserve = 240,
        rangeMeters = 68f,
        spreadDeg = 1.6f,
        pellets = 1,
        reloadMs = 1500L,
        adsFov = 42f,
        lengthScale = 1.0f,
        barrelColorHex = 0xFF2B3138
    ),
    TACTICAL_SHOTGUN(
        displayName = "SPAS-12 Breacher",
        shortCode = "SG-12",
        damage = 22,
        fireRateMs = 680L,
        magSize = 8,
        startReserve = 48,
        maxReserve = 72,
        rangeMeters = 26f,
        spreadDeg = 7.5f,
        pellets = 6,
        reloadMs = 1850L,
        adsFov = 48f,
        lengthScale = 0.92f,
        barrelColorHex = 0xFF3A3F47
    ),
    MARKSMAN_SNIPER(
        displayName = "SR-25 Sentinel",
        shortCode = "DMR-25",
        damage = 98,
        fireRateMs = 580L,
        magSize = 10,
        startReserve = 50,
        maxReserve = 80,
        rangeMeters = 110f,
        spreadDeg = 0.35f,
        pellets = 1,
        reloadMs = 1950L,
        adsFov = 24f,
        lengthScale = 1.28f,
        barrelColorHex = 0xFF232A24
    ),
    COMBAT_PISTOL(
        displayName = "M9A3 Sidearm",
        shortCode = "HG-9",
        damage = 28,
        fireRateMs = 230L,
        magSize = 15,
        startReserve = 90,
        maxReserve = 135,
        rangeMeters = 42f,
        spreadDeg = 2.0f,
        pellets = 1,
        reloadMs = 1100L,
        adsFov = 48f,
        lengthScale = 0.55f,
        barrelColorHex = 0xFF343A42
    )
}

enum class TacticalGearType(
    val displayName: String,
    val shortCode: String,
    val cooldownMs: Long
) {
    FRAG_GRENADE("M67 Frag", "FRAG", 2200L),
    MED_STIM("Combat Med-Stim", "MED", 3000L),
    SHOCK_FLARE("EMP Shock Flare", "EMP", 4000L)
}

enum class ZombieType(
    val displayName: String,
    val maxHp: Int,
    val speed: Float,
    val damage: Int,
    val scale: Float,
    val xpReward: Int,
    val scrapReward: Int
) {
    WALKER("Infected Walker", 95, 2.35f, 11, 1.0f, 20, 4),
    RUNNER("Feral Sprinter", 70, 5.1f, 14, 0.94f, 30, 6),
    BRUTE("Armored Juggernaut", 290, 1.85f, 25, 1.32f, 75, 16),
    TOXIC("Bio-Spitter", 115, 2.8f, 16, 1.05f, 45, 9)
}

enum class WorldZone(
    val title: String,
    val subtitle: String,
    val centerX: Float,
    val centerZ: Float,
    val radius: Float
) {
    SANCTUARY_VALLEY(
        title = "Sanctuary Valley",
        subtitle = "Protected Survivor Village",
        centerX = 0f,
        centerZ = 54f,
        radius = 36f
    ),
    ASHFALL_DOWNTOWN(
        title = "Ashfall Downtown",
        subtitle = "Collapsed Financial Towers",
        centerX = -46f,
        centerZ = -8f,
        radius = 45f
    ),
    HARBOR_QUARANTINE(
        title = "Harbor Quarantine",
        subtitle = "Abandoned Military Cargo Port",
        centerX = 48f,
        centerZ = -10f,
        radius = 44f
    ),
    BIOCORE_SPIRE(
        title = "Bio-Core Ground Zero",
        subtitle = "Infected Research Nexus",
        centerX = 0f,
        centerZ = -68f,
        radius = 42f
    )
}

data class ResourceInventory(
    val scrap: Int = 60,
    val meds: Int = 25,
    val ammo: Int = 50,
    val fuel: Int = 20,
    val bioSamples: Int = 5
)
