package com.example

import com.example.audio.TacticalSoundEngine
import com.example.data.ArmorLoadout
import com.example.data.CameraPerspective
import com.example.data.OutpostEntity
import com.example.data.TacticalGearType
import com.example.data.WarriorEntity
import com.example.data.WeaponType
import com.example.data.WorldZone
import com.example.game.GameEngine
import com.example.game.GameEventCallbacks
import com.example.game.WorldGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LostEarthSimulationTest {

    @Test
    fun worldGenerator_populatesFourSectorsSurvivorsAndCrates() {
        val outposts = listOf(
            OutpostEntity("o1", "Ashfall Relay", WorldZone.ASHFALL_DOWNTOWN.title, -48f, -12f, false, 0, "")
        )
        val geometry = WorldGenerator.buildWorldGeometry(outposts)
        val survivors = WorldGenerator.buildSurvivors()
        val crates = WorldGenerator.buildSupplyCrates()

        assertTrue(geometry.size > 40)
        assertEquals(5, survivors.size)
        assertEquals(7, crates.size)
        assertEquals(WorldZone.SANCTUARY_VALLEY, WorldGenerator.determineZone(0f, 52f))
        assertEquals(WorldZone.ASHFALL_DOWNTOWN, WorldGenerator.determineZone(-46f, -8f))
        assertEquals(WorldZone.HARBOR_QUARANTINE, WorldGenerator.determineZone(48f, -10f))
        assertEquals(WorldZone.BIOCORE_SPIRE, WorldGenerator.determineZone(0f, -68f))
    }

    @Test
    fun gameEngine_supportsPerspectiveToggleMovementFiringAndMedStim() {
        var rewardsEarned = 0
        val sound = TacticalSoundEngine().apply { enabled = false }
        val engine = GameEngine(
            soundEngine = sound,
            callbacks = object : GameEventCallbacks {
                override fun onRewardsEarned(scrap: Int, meds: Int, ammo: Int, fuel: Int, bio: Int, kills: Int, xp: Int) {
                    rewardsEarned += scrap
                }
                override fun onSurvivorRescued(survivorName: String) {}
                override fun onOutpostCaptured(outpostId: String) {}
            }
        )

        engine.initializeWorld(
            outpostEntities = listOf(
                OutpostEntity("o1", "Ashfall Relay", WorldZone.ASHFALL_DOWNTOWN.title, -48f, -12f, false, 0, "")
            ),
            deployedWarriors = listOf(
                WarriorEntity("w_elena", "SPECTER", "Sgt. Elena Vance", "Recon", "", "SR-25 Sentinel", 0xFF38BDF8, true, true, 1, 0, 0)
            ),
            loadout = ArmorLoadout(),
            primary = WeaponType.ASSAULT_RIFLE,
            secondary = WeaponType.TACTICAL_SHOTGUN,
            armoryLevel = 1,
            medbayLevel = 1,
            watchtowerLevel = 1,
            solarLevel = 1
        )

        assertEquals(CameraPerspective.THIRD_PERSON, engine.perspective)
        engine.togglePerspective()
        assertEquals(CameraPerspective.FIRST_PERSON, engine.perspective)

        val startZ = engine.playerZ
        engine.inputMoveY = 1f
        repeat(10) { engine.stepSimulation(0.03f) }
        assertTrue(engine.playerZ < startZ)

        engine.health = 45
        engine.useTacticalGear(TacticalGearType.MED_STIM)
        engine.stepSimulation(0.02f)
        assertTrue(engine.health > 45)
    }
}
