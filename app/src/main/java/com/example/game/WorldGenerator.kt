package com.example.game

import com.example.data.OutpostEntity
import com.example.data.WorldZone

enum class MaterialKind {
    ASPHALT,
    CONCRETE_RUIN,
    SKYSCRAPER_WINDOW,
    RUST_METAL,
    CONTAINER_BLUE,
    CONTAINER_RED,
    WOOD_TIMBER,
    FOLIAGE,
    GLOW_AMBER,
    GLOW_CYAN,
    GLOW_CRIMSON,
    SANDBAG
}

data class WorldBox(
    val id: String,
    val x: Float,
    val y: Float, // center Y
    val z: Float,
    val sx: Float, // full width X
    val sy: Float, // full height Y
    val sz: Float, // full depth Z
    val material: MaterialKind,
    val collidable: Boolean = true,
    val colorTint: Long = 0xFFFFFFFF
)

data class WorldSurvivor(
    val id: String,
    val name: String,
    val role: String,
    val message: String,
    val x: Float,
    val z: Float,
    val yaw: Float,
    var rescued: Boolean = false
)

data class WorldCrate(
    val id: String,
    val x: Float,
    val z: Float,
    val label: String,
    val scrapAmount: Int,
    val ammoAmount: Int,
    val medAmount: Int,
    val fuelAmount: Int,
    var collected: Boolean = false
)

data class WorldOutpost(
    val id: String,
    val name: String,
    val zoneName: String,
    val x: Float,
    val z: Float,
    var captured: Boolean,
    var progress: Float = if (captured) 1f else 0f,
    var waveSpawned: Boolean = false
)

object WorldGenerator {
    const val WORLD_HALF_EXTENT = 104f

    fun buildWorldGeometry(outposts: List<OutpostEntity>): List<WorldBox> {
        val boxes = mutableListOf<WorldBox>()
        var idx = 0
        fun add(
            x: Float,
            yBottom: Float,
            z: Float,
            sx: Float,
            sy: Float,
            sz: Float,
            mat: MaterialKind,
            collidable: Boolean = true,
            tint: Long = 0xFFFFFFFF
        ) {
            boxes.add(
                WorldBox(
                    id = "wb_${idx++}",
                    x = x,
                    y = yBottom + sy * 0.5f,
                    z = z,
                    sx = sx,
                    sy = sy,
                    sz = sz,
                    material = mat,
                    collidable = collidable,
                    colorTint = tint
                )
            )
        }

        // 1. Main Asphalt Road Network (non-collidable flat strips slightly above ground)
        add(0f, 0.01f, -18f, 12f, 0.04f, 170f, MaterialKind.ASPHALT, false)
        add(0f, 0.015f, -12f, 168f, 0.04f, 11f, MaterialKind.ASPHALT, false)
        add(-46f, 0.015f, -28f, 9f, 0.04f, 68f, MaterialKind.ASPHALT, false)
        add(48f, 0.015f, -26f, 9f, 0.04f, 68f, MaterialKind.ASPHALT, false)

        // 2. Sanctuary Valley (Protected Village Hub: z = +34..+86, x = -28..+28)
        // Perimeter walls with North Gate (x in -6..6) & Side Gates
        add(-18f, 0f, 34f, 22f, 3.8f, 1.8f, MaterialKind.WOOD_TIMBER)
        add(18f, 0f, 34f, 22f, 3.8f, 1.8f, MaterialKind.WOOD_TIMBER)
        add(-29f, 0f, 58f, 1.8f, 3.8f, 48f, MaterialKind.CONCRETE_RUIN)
        add(29f, 0f, 58f, 1.8f, 3.8f, 48f, MaterialKind.CONCRETE_RUIN)
        add(0f, 0f, 82f, 58f, 4.0f, 2.0f, MaterialKind.CONCRETE_RUIN)

        // Gate Watchtowers
        add(-9.5f, 0f, 34f, 3.2f, 7.2f, 3.2f, MaterialKind.WOOD_TIMBER)
        add(-9.5f, 7.2f, 34f, 4.0f, 0.4f, 4.0f, MaterialKind.RUST_METAL)
        add(-9.5f, 7.6f, 34f, 0.9f, 0.9f, 0.9f, MaterialKind.GLOW_AMBER, false)

        add(9.5f, 0f, 34f, 3.2f, 7.2f, 3.2f, MaterialKind.WOOD_TIMBER)
        add(9.5f, 7.2f, 34f, 4.0f, 0.4f, 4.0f, MaterialKind.RUST_METAL)
        add(9.5f, 7.6f, 34f, 0.9f, 0.9f, 0.9f, MaterialKind.GLOW_AMBER, false)

        // Village Buildings: Medbay, Armory, Barracks, Solar Array, Campfire
        add(-18f, 0f, 52f, 8f, 4.2f, 7f, MaterialKind.CONCRETE_RUIN)
        add(-18f, 4.2f, 52f, 8.6f, 0.5f, 7.6f, MaterialKind.CONTAINER_BLUE)
        add(18f, 0f, 52f, 8f, 4.2f, 7f, MaterialKind.RUST_METAL)
        add(18f, 4.2f, 52f, 8.6f, 0.5f, 7.6f, MaterialKind.WOOD_TIMBER)
        add(-17f, 0f, 69f, 9f, 4.5f, 7f, MaterialKind.WOOD_TIMBER)
        add(17f, 0f, 69f, 9f, 4.5f, 7f, MaterialKind.CONCRETE_RUIN)

        // Village Central Hearth / Beacon
        add(0f, 0f, 60f, 2.2f, 0.6f, 2.2f, MaterialKind.SANDBAG)
        add(0f, 0.6f, 60f, 0.9f, 0.8f, 0.9f, MaterialKind.GLOW_AMBER, false)
        add(0f, 0f, 66f, 0.4f, 8.0f, 0.4f, MaterialKind.RUST_METAL)
        add(0f, 7.6f, 66f, 1.4f, 0.8f, 0.4f, MaterialKind.GLOW_CYAN, false)

        // Sandbag barricades along main gate road
        add(-5.5f, 0f, 31f, 3.2f, 1.1f, 1.1f, MaterialKind.SANDBAG)
        add(5.5f, 0f, 31f, 3.2f, 1.1f, 1.1f, MaterialKind.SANDBAG)

        // 3. Ashfall Downtown (Ruined City Skyscrapers & Vehicles: x = -92..-16, z = -56..+24)
        add(-32f, 0f, 4f, 11f, 16f, 11f, MaterialKind.SKYSCRAPER_WINDOW)
        add(-32f, 16f, 4f, 7f, 5f, 8f, MaterialKind.CONCRETE_RUIN)
        add(-64f, 0f, 6f, 13f, 22f, 12f, MaterialKind.SKYSCRAPER_WINDOW)
        add(-64f, 0f, -28f, 12f, 18f, 12f, MaterialKind.SKYSCRAPER_WINDOW)
        add(-30f, 0f, -30f, 10f, 14f, 10f, MaterialKind.CONCRETE_RUIN)
        add(-78f, 0f, -12f, 10f, 12f, 14f, MaterialKind.SKYSCRAPER_WINDOW)
        add(-54f, 0f, -46f, 11f, 15f, 10f, MaterialKind.CONCRETE_RUIN)
        add(-34f, 0f, 20f, 9f, 8f, 8f, MaterialKind.CONCRETE_RUIN)

        // Ruined road vehicles & concrete highway dividers in Ashfall & Center Ave
        add(-3.5f, 0f, 10f, 2.2f, 1.6f, 4.4f, MaterialKind.RUST_METAL)
        add(3.8f, 0f, -4f, 2.4f, 1.7f, 4.6f, MaterialKind.CONTAINER_RED)
        add(-22f, 0f, -12f, 4.5f, 1.6f, 2.1f, MaterialKind.RUST_METAL)
        add(-56f, 0f, -11f, 4.6f, 1.8f, 2.2f, MaterialKind.CONTAINER_BLUE)
        add(-41f, 0f, -20f, 1.4f, 1.1f, 4.0f, MaterialKind.SANDBAG)

        // 4. Harbor Quarantine (Shipping Containers, Fuel Tanks, Crane Pillars: x = +16..+92)
        add(28f, 0f, 4f, 3.2f, 3.0f, 7.6f, MaterialKind.CONTAINER_RED)
        add(33f, 0f, 4f, 3.2f, 3.0f, 7.6f, MaterialKind.CONTAINER_BLUE)
        add(30.5f, 3.0f, 4f, 3.2f, 3.0f, 7.6f, MaterialKind.RUST_METAL)

        add(64f, 0f, 2f, 3.4f, 3.1f, 8.2f, MaterialKind.CONTAINER_BLUE)
        add(69f, 0f, 2f, 3.4f, 3.1f, 8.2f, MaterialKind.CONTAINER_RED)
        add(66.5f, 3.1f, 2f, 3.4f, 3.1f, 8.2f, MaterialKind.CONTAINER_BLUE)

        add(30f, 0f, -28f, 8.0f, 3.4f, 3.4f, MaterialKind.CONTAINER_RED)
        add(30f, 0f, -34f, 8.0f, 3.4f, 3.4f, MaterialKind.RUST_METAL)
        add(66f, 0f, -30f, 11f, 10f, 12f, MaterialKind.CONCRETE_RUIN)
        add(78f, 0f, -12f, 6f, 14f, 6f, MaterialKind.RUST_METAL)
        add(22f, 0f, 18f, 7f, 5f, 7f, MaterialKind.CONCRETE_RUIN)
        add(58f, 0f, 20f, 9f, 4.5f, 6f, MaterialKind.RUST_METAL)

        // Quarantine checkpoints & fuel silos
        add(15f, 0f, -12f, 1.2f, 1.2f, 4.5f, MaterialKind.SANDBAG)
        add(42f, 0f, -22f, 4.2f, 5.2f, 4.2f, MaterialKind.RUST_METAL)
        add(42f, 5.2f, -22f, 1.2f, 0.8f, 1.2f, MaterialKind.GLOW_AMBER, false)

        // 5. North Highway Checkpoint & Bio-Core Ground Zero (z = -38..-96)
        add(-14f, 0f, -44f, 7f, 4.0f, 3.5f, MaterialKind.CONCRETE_RUIN)
        add(12f, 0f, -44f, 7f, 4.0f, 3.5f, MaterialKind.CONCRETE_RUIN)
        add(-4f, 0f, -38f, 3.0f, 1.2f, 1.2f, MaterialKind.SANDBAG)
        add(4f, 0f, -38f, 3.0f, 1.2f, 1.2f, MaterialKind.SANDBAG)

        // Bio-Core Research Spire Complex
        add(-24f, 0f, -72f, 12f, 18f, 14f, MaterialKind.SKYSCRAPER_WINDOW)
        add(24f, 0f, -72f, 12f, 18f, 14f, MaterialKind.SKYSCRAPER_WINDOW)
        add(0f, 0f, -92f, 22f, 26f, 12f, MaterialKind.SKYSCRAPER_WINDOW)
        add(0f, 26f, -92f, 8f, 8f, 8f, MaterialKind.CONCRETE_RUIN)
        add(0f, 34f, -92f, 2.2f, 3.0f, 2.2f, MaterialKind.GLOW_CRIMSON, false)

        // Bio-containment pylons around Ground Zero
        add(-12f, 0f, -66f, 2.0f, 5.5f, 2.0f, MaterialKind.RUST_METAL)
        add(-12f, 5.5f, -66f, 1.2f, 1.2f, 1.2f, MaterialKind.GLOW_CRIMSON, false)
        add(12f, 0f, -66f, 2.0f, 5.5f, 2.0f, MaterialKind.RUST_METAL)
        add(12f, 5.5f, -66f, 1.2f, 1.2f, 1.2f, MaterialKind.GLOW_CRIMSON, false)

        // Outpost Beacon Towers in the 3D world
        outposts.forEach { outpost ->
            add(outpost.worldX - 2.6f, 0f, outpost.worldZ - 2.6f, 1.1f, 1.1f, 1.1f, MaterialKind.SANDBAG)
            add(outpost.worldX + 2.6f, 0f, outpost.worldZ + 2.6f, 1.1f, 1.1f, 1.1f, MaterialKind.SANDBAG)
            add(outpost.worldX, 0f, outpost.worldZ, 1.1f, 5.8f, 1.1f, MaterialKind.RUST_METAL)
            add(
                outpost.worldX,
                5.8f,
                outpost.worldZ,
                1.4f,
                1.4f,
                1.4f,
                if (outpost.captured) MaterialKind.GLOW_CYAN else MaterialKind.GLOW_AMBER,
                false
            )
        }

        // Scattered Trees / Overgrown Pines along valley & outer ruins
        val treePositions = listOf(
            -42f to 48f, -48f to 64f, -38f to 78f,
            40f to 44f, 46f to 68f, 36f to 84f,
            -84f to 26f, -86f to -46f, 84f to 26f, 84f to -48f,
            -16f to 22f, 16f to 24f, -44f to -76f, 44f to -76f
        )
        treePositions.forEach { (tx, tz) ->
            add(tx, 0f, tz, 0.8f, 3.2f, 0.8f, MaterialKind.WOOD_TIMBER)
            add(tx, 3.0f, tz, 3.4f, 3.6f, 3.4f, MaterialKind.FOLIAGE, false)
        }

        return boxes
    }

    fun buildSurvivors(): List<WorldSurvivor> = listOf(
        WorldSurvivor(
            id = "surv_1",
            name = "Dr. Samuel Ortiz",
            role = "Field Agronomist",
            message = "Thank God! Take me to Sanctuary Valley—I have frost-resistant seed vaults!",
            x = -40f,
            z = 14f,
            yaw = 0.6f
        ),
        WorldSurvivor(
            id = "surv_2",
            name = "Nadia Koval",
            role = "Grid Electrician",
            message = "I hid from the sprinters inside the metro stairwell. Let's rebuild the solar grid!",
            x = -70f,
            z = -18f,
            yaw = 1.4f
        ),
        WorldSurvivor(
            id = "surv_3",
            name = "Chief Owen Pike",
            role = "Harbor Machinist",
            message = "My fuel valves are intact. Get me behind Sanctuary walls and I'll forge armor!",
            x = 58f,
            z = 12f,
            yaw = -1.1f
        ),
        WorldSurvivor(
            id = "surv_4",
            name = "Lt. Priya Sharma",
            role = "Radio Cryptographer",
            message = "I intercepted clean survivor signals up north. Count me in for the New Earth!",
            x = 46f,
            z = -38f,
            yaw = 2.4f
        ),
        WorldSurvivor(
            id = "surv_5",
            name = "Dr. Jonas Reid",
            role = "Bio-Core Virologist",
            message = "I backed up the pathogen genome before the spire fell! Escort me out!",
            x = -10f,
            z = -62f,
            yaw = 0.2f
        )
    )

    fun buildSupplyCrates(): List<WorldCrate> = listOf(
        WorldCrate("crate_1", -14f, 18f, "Civilian Emergency Cache", 18, 25, 4, 4),
        WorldCrate("crate_2", -45f, -4f, "Downtown Armory Drop", 26, 40, 2, 6),
        WorldCrate("crate_3", -72f, -38f, "Collapsed Convoy Crate", 24, 30, 6, 8),
        WorldCrate("crate_4", 36f, -6f, "UN Cargo Container", 28, 35, 5, 10),
        WorldCrate("crate_5", 72f, -24f, "Harbor Fuel & Steel", 32, 20, 4, 14),
        WorldCrate("crate_6", 12f, -40f, "Checkpoint Munitions", 20, 45, 5, 6),
        WorldCrate("crate_7", 6f, -72f, "Bio-Core Lab Supply", 35, 40, 8, 10)
    )

    fun determineZone(x: Float, z: Float): WorldZone {
        return WorldZone.entries.minByOrNull { zone ->
            val dx = x - zone.centerX
            val dz = z - zone.centerZ
            dx * dx + dz * dz
        } ?: WorldZone.SANCTUARY_VALLEY
    }
}
