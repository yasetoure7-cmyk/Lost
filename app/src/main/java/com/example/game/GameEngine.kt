package com.example.game

import com.example.audio.TacticalSoundEngine
import com.example.data.ArmorLoadout
import com.example.data.CameraPerspective
import com.example.data.OutpostEntity
import com.example.data.PlayerStance
import com.example.data.TacticalGearType
import com.example.data.WarriorEntity
import com.example.data.WeaponType
import com.example.data.WorldZone
import com.example.data.ZombieType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class ZombieActor(
    val id: Int,
    val type: ZombieType,
    var x: Float,
    var z: Float,
    var yaw: Float,
    var hp: Int,
    val maxHp: Int,
    var animPhase: Float = Random.nextFloat() * 6.28f,
    var hitFlashTimer: Float = 0f,
    var stunnedTimer: Float = 0f,
    var attackCooldown: Float = 0f,
    var dead: Boolean = false,
    var deathTimer: Float = 0f
)

data class SquadActor(
    val id: String,
    val callsign: String,
    val fullName: String,
    val weapon: WeaponType,
    val accentHex: Long,
    val level: Int,
    var x: Float,
    var z: Float,
    var yaw: Float,
    var animPhase: Float = 0f,
    var stance: PlayerStance = PlayerStance.IDLE,
    var fireCooldown: Float = 0f,
    var muzzleFlashTimer: Float = 0f,
    var healCooldown: Float = 0f
)

data class BulletTracer(
    val x1: Float,
    val y1: Float,
    val z1: Float,
    val x2: Float,
    val y2: Float,
    val z2: Float,
    val colorHex: Long,
    var life: Float = 0.09f
)

data class WorldParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val size: Float,
    val colorHex: Long,
    var life: Float,
    val maxLife: Float,
    val gravity: Float = 9.8f
)

data class ActiveGrenade(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val type: TacticalGearType,
    var fuse: Float = 1.15f
)

data class MiniMapBlip(
    val x: Float,
    val z: Float,
    val kind: BlipKind,
    val label: String = ""
)

enum class BlipKind {
    ZOMBIE,
    SQUAD,
    SURVIVOR,
    OUTPOST_HOSTILE,
    OUTPOST_SECURED,
    CRATE
}

data class HudState(
    val health: Int = 100,
    val maxHealth: Int = 100,
    val armor: Int = 55,
    val maxArmor: Int = 55,
    val stamina: Float = 1f,
    val perspective: CameraPerspective = CameraPerspective.THIRD_PERSON,
    val stance: PlayerStance = PlayerStance.IDLE,
    val isCrouching: Boolean = false,
    val isAds: Boolean = false,
    val isSprinting: Boolean = false,
    val activeSlot: Int = 0,
    val weapons: List<WeaponType> = listOf(
        WeaponType.ASSAULT_RIFLE,
        WeaponType.TACTICAL_SHOTGUN,
        WeaponType.COMBAT_PISTOL
    ),
    val magAmmo: Int = 30,
    val reserveAmmo: Int = 180,
    val isReloading: Boolean = false,
    val reloadProgress: Float = 0f,
    val fragCount: Int = 3,
    val medStimCount: Int = 2,
    val shockFlareCount: Int = 2,
    val playerX: Float = 0f,
    val playerZ: Float = 44f,
    val playerYaw: Float = 0f,
    val currentZone: WorldZone = WorldZone.SANCTUARY_VALLEY,
    val killCountSession: Int = 0,
    val hitMarkerActive: Boolean = false,
    val hitMarkerHeadshot: Boolean = false,
    val damageFlash: Float = 0f,
    val damageAngleRad: Float = 0f,
    val interactPrompt: String? = null,
    val activeOutpostName: String? = null,
    val activeOutpostProgress: Float = 0f,
    val targetEnemyName: String? = null,
    val targetEnemyHpFrac: Float = 0f,
    val radioSpeaker: String? = null,
    val radioMessage: String? = null,
    val mapBlips: List<MiniMapBlip> = emptyList(),
    val isDowned: Boolean = false
)

interface GameEventCallbacks {
    fun onRewardsEarned(scrap: Int, meds: Int, ammo: Int, fuel: Int, bio: Int, kills: Int, xp: Int)
    fun onSurvivorRescued(survivorName: String)
    fun onOutpostCaptured(outpostId: String)
}

class GameEngine(
    private val soundEngine: TacticalSoundEngine,
    private val callbacks: GameEventCallbacks
) {
    var worldBoxes: List<WorldBox> = emptyList()
        private set
    val outposts = mutableListOf<WorldOutpost>()
    val survivors = mutableListOf<WorldSurvivor>()
    val crates = mutableListOf<WorldCrate>()
    val zombies = mutableListOf<ZombieActor>()
    val squadmates = mutableListOf<SquadActor>()
    val tracers = mutableListOf<BulletTracer>()
    val particles = mutableListOf<WorldParticle>()
    val grenades = mutableListOf<ActiveGrenade>()

    // Player simulation state
    var playerX = 0f
    var playerY = 0f
    var playerZ = 44f
    var playerVy = 0f
    var onGround = true
    var playerYaw = 0f // Unrestricted 360-degree horizontal look
    var playerPitch = -0.05f // Wide vertical look (-1.38..+1.38 rad, ~±79 deg)
    var playerBodyYaw = 0f // Smooth directional stride orientation in 3rd-person
    var playerAnimPhase = 0f
    var playerStance = PlayerStance.IDLE
    var armorLoadout = ArmorLoadout()

    var health = 100
    var maxHealth = 100
    var armor = 55
    var maxArmor = 55
    var stamina = 1f

    var perspective = CameraPerspective.THIRD_PERSON
    var activeSlot = 0
    val equippedWeapons = mutableListOf(
        WeaponType.ASSAULT_RIFLE,
        WeaponType.TACTICAL_SHOTGUN,
        WeaponType.COMBAT_PISTOL
    )
    val magAmmoMap = IntArray(3) { 30 }
    val reserveAmmoMap = IntArray(3) { 180 }

    var fragCount = 3
    var medStimCount = 2
    var shockFlareCount = 2

    var muzzleFlashTimer = 0f
    var recoilKick = 0f
    var reloadTimer = 0f
    var reloadDuration = 0f
    var fireCooldown = 0f
    var hitMarkerTimer = 0f
    var hitMarkerHeadshot = false
    var damageFlashTimer = 0f
    var lastDamageAngle = 0f
    var isDowned = false
    var sessionKills = 0

    // Touch input state (set from Compose multi-touch UI)
    @Volatile var inputMoveX = 0f
    @Volatile var inputMoveY = 0f
    @Volatile var inputSprint = false
    @Volatile var inputCrouch = false
    @Volatile var inputAds = false
    @Volatile var inputFiring = false

    private var pendingJump = false
    private var pendingReload = false
    private var pendingInteract = false
    private var pendingGear: TacticalGearType? = null
    private var pendingSlot = -1

    private var radioTimer = 0f
    private var radioSpeaker: String? = null
    private var radioMessage: String? = null
    private var hudSyncAccum = 0f
    private var zombieSpawnTimer = 1.5f
    private var atmosphereTimer = 0f
    private var nextZombieId = 1
    private var villageBonusArmor = 0
    private var villageMedCapacityBonus = 0
    private var villageSquadDmgMult = 1f

    var onHudUpdated: ((HudState) -> Unit)? = null

    fun initializeWorld(
        outpostEntities: List<OutpostEntity>,
        deployedWarriors: List<WarriorEntity>,
        loadout: ArmorLoadout,
        primary: WeaponType,
        secondary: WeaponType,
        armoryLevel: Int,
        medbayLevel: Int,
        watchtowerLevel: Int,
        solarLevel: Int
    ) {
        synchronized(this) {
            villageBonusArmor = (armoryLevel - 1) * 15
            villageMedCapacityBonus = (medbayLevel - 1)
            villageSquadDmgMult = 1f + (watchtowerLevel - 1) * 0.1f

            armorLoadout = loadout
            maxArmor = loadout.maxArmor + villageBonusArmor
            armor = maxArmor
            maxHealth = 100
            health = 100

            equippedWeapons[0] = primary
            equippedWeapons[1] = if (secondary == primary) WeaponType.MARKSMAN_SNIPER else secondary
            equippedWeapons[2] = WeaponType.COMBAT_PISTOL
            for (i in 0..2) {
                magAmmoMap[i] = equippedWeapons[i].magSize
                reserveAmmoMap[i] = equippedWeapons[i].startReserve
            }
            activeSlot = 0
            fragCount = 3 + (solarLevel - 1)
            medStimCount = 2 + villageMedCapacityBonus
            shockFlareCount = 2 + (solarLevel - 1)

            worldBoxes = WorldGenerator.buildWorldGeometry(outpostEntities)
            outposts.clear()
            outpostEntities.forEach { e ->
                outposts.add(WorldOutpost(e.id, e.name, e.zoneName, e.worldX, e.worldZ, e.captured))
            }
            survivors.clear()
            survivors.addAll(WorldGenerator.buildSurvivors())
            crates.clear()
            crates.addAll(WorldGenerator.buildSupplyCrates())

            squadmates.clear()
            deployedWarriors.forEachIndexed { i, w ->
                val weapon = WeaponType.entries.firstOrNull { it.displayName == w.weaponName }
                    ?: WeaponType.ASSAULT_RIFLE
                squadmates.add(
                    SquadActor(
                        id = w.id,
                        callsign = w.callsign,
                        fullName = w.fullName,
                        weapon = weapon,
                        accentHex = w.accentHex,
                        level = w.level,
                        x = (i - 1) * 2.8f,
                        z = 48f + i * 1.4f,
                        yaw = 0f
                    )
                )
            }

            zombies.clear()
            tracers.clear()
            particles.clear()
            grenades.clear()

            playerX = 0f
            playerY = 0f
            playerZ = 44f
            playerYaw = 0f
            playerBodyYaw = 0f
            playerPitch = -0.05f
            isDowned = false
            sessionKills = 0

            spawnZoneHorde(WorldZone.ASHFALL_DOWNTOWN, 8)
            spawnZoneHorde(WorldZone.HARBOR_QUARANTINE, 7)
            spawnZoneHorde(WorldZone.BIOCORE_SPIRE, 8)

            showRadio("SANCTUARY HQ", "Gate open. Left stick moves 360° • Right screen aims & fires.")
            pushHudSnapshot()
        }
    }

    fun syncArmoryAndSquad(
        loadout: ArmorLoadout,
        primary: WeaponType,
        secondary: WeaponType,
        deployedWarriors: List<WarriorEntity>
    ) {
        synchronized(this) {
            armorLoadout = loadout
            maxArmor = loadout.maxArmor + villageBonusArmor
            armor = armor.coerceAtMost(maxArmor)
            equippedWeapons[0] = primary
            equippedWeapons[1] = if (secondary == primary) WeaponType.MARKSMAN_SNIPER else secondary

            val existingIds = squadmates.map { it.id }.toSet()
            val targetIds = deployedWarriors.map { it.id }.toSet()
            if (existingIds != targetIds) {
                squadmates.clear()
                deployedWarriors.forEachIndexed { i, w ->
                    val weapon = WeaponType.entries.firstOrNull { it.displayName == w.weaponName }
                        ?: WeaponType.ASSAULT_RIFLE
                    squadmates.add(
                        SquadActor(
                            id = w.id,
                            callsign = w.callsign,
                            fullName = w.fullName,
                            weapon = weapon,
                            accentHex = w.accentHex,
                            level = w.level,
                            x = playerX + (i - 1) * 2.5f,
                            z = playerZ + 3f,
                            yaw = playerYaw
                        )
                    )
                }
            }
        }
    }

    fun applyAimDelta(dx: Float, dy: Float) {
        val sens = if (inputAds) 0.0024f else 0.0042f
        playerYaw -= dx * sens
        // Full 360° wrap to avoid precision drift while spinning continuously
        val twoPi = (2.0 * PI).toFloat()
        if (playerYaw > PI.toFloat()) playerYaw -= twoPi
        if (playerYaw < -PI.toFloat()) playerYaw += twoPi
        // Wide vertical look range (±79°) so user can look straight up at towers or down at ground
        playerPitch = (playerPitch - dy * sens).coerceIn(-1.38f, 1.38f)
    }

    fun togglePerspective() {
        perspective = if (perspective == CameraPerspective.THIRD_PERSON) {
            CameraPerspective.FIRST_PERSON
        } else {
            CameraPerspective.THIRD_PERSON
        }
        pushHudSnapshot()
    }

    fun selectWeaponSlot(slot: Int) {
        if (slot in 0..2) pendingSlot = slot
    }

    fun triggerJump() {
        pendingJump = true
    }

    fun triggerReload() {
        pendingReload = true
    }

    fun triggerInteract() {
        pendingInteract = true
    }

    fun useTacticalGear(type: TacticalGearType) {
        pendingGear = type
    }

    fun respawnAtSanctuary() {
        synchronized(this) {
            playerX = 0f
            playerY = 0f
            playerZ = 54f
            playerYaw = 0f
            playerBodyYaw = 0f
            health = maxHealth
            armor = maxArmor
            stamina = 1f
            isDowned = false
            squadmates.forEachIndexed { idx, sq ->
                sq.x = (idx - 1) * 2.8f
                sq.z = 57f
            }
            showRadio("MEDBAY", "Emergency evac complete. Back at Sanctuary Valley perimeter.")
            pushHudSnapshot()
        }
    }

    fun stepSimulation(rawDt: Float) {
        val dt = rawDt.coerceIn(0.004f, 0.045f)
        synchronized(this) {
            updateTimers(dt)
            handlePendingActions()
            if (!isDowned) {
                updatePlayerMovement(dt)
                updateWeaponAndFiring(dt)
                updateOutpostCapture(dt)
            }
            updateSquadAi(dt)
            updateZombies(dt)
            updateGrenades(dt)
            updateParticlesAndTracers(dt)
            updateAtmosphereParticles(dt)
            updateDynamicSpawns(dt)

            hudSyncAccum += dt
            if (hudSyncAccum >= 0.05f) {
                hudSyncAccum = 0f
                pushHudSnapshot()
            }
        }
    }

    private fun updateTimers(dt: Float) {
        if (muzzleFlashTimer > 0f) muzzleFlashTimer = (muzzleFlashTimer - dt).coerceAtLeast(0f)
        if (recoilKick > 0f) recoilKick = (recoilKick - dt * 4.5f).coerceAtLeast(0f)
        if (fireCooldown > 0f) fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        if (hitMarkerTimer > 0f) hitMarkerTimer = (hitMarkerTimer - dt).coerceAtLeast(0f)
        if (damageFlashTimer > 0f) damageFlashTimer = (damageFlashTimer - dt * 1.8f).coerceAtLeast(0f)
        if (radioTimer > 0f) {
            radioTimer -= dt
            if (radioTimer <= 0f) {
                radioSpeaker = null
                radioMessage = null
            }
        }
        if (reloadTimer > 0f) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                val weapon = equippedWeapons[activeSlot]
                val needed = weapon.magSize - magAmmoMap[activeSlot]
                val loaded = needed.coerceAtMost(reserveAmmoMap[activeSlot])
                magAmmoMap[activeSlot] += loaded
                reserveAmmoMap[activeSlot] -= loaded
            }
        }
    }

    private fun handlePendingActions() {
        if (pendingSlot in 0..2 && pendingSlot != activeSlot) {
            activeSlot = pendingSlot
            reloadTimer = 0f
            soundEngine.playReload()
        }
        pendingSlot = -1

        if (pendingJump && onGround && !isDowned) {
            playerVy = 6.6f
            onGround = false
            inputCrouch = false
        }
        pendingJump = false

        if (pendingReload && reloadTimer <= 0f && !isDowned) {
            val weapon = equippedWeapons[activeSlot]
            if (magAmmoMap[activeSlot] < weapon.magSize && reserveAmmoMap[activeSlot] > 0) {
                reloadDuration = weapon.reloadMs / 1000f
                reloadTimer = reloadDuration
                soundEngine.playReload()
            }
        }
        pendingReload = false

        if (pendingInteract && !isDowned) {
            tryInteractNearby()
        }
        pendingInteract = false

        pendingGear?.let { gear ->
            if (!isDowned) activateGear(gear)
            pendingGear = null
        }
    }

    private fun updatePlayerMovement(dt: Float) {
        val forwardX = -sin(playerYaw)
        val forwardZ = -cos(playerYaw)
        val rightX = cos(playerYaw)
        val rightZ = -sin(playerYaw)

        val rawMag = hypot(inputMoveX, inputMoveY)
        val moving = rawMag > 0.06f
        val wantSprint = moving && (inputSprint || rawMag > 0.88f) && !inputCrouch && !inputAds && stamina > 0.08f

        if (wantSprint) {
            stamina = (stamina - dt * 0.20f).coerceAtLeast(0f)
        } else {
            stamina = (stamina + dt * 0.18f).coerceAtMost(1f)
        }

        val baseSpeed = 5.6f * armorLoadout.vest.speedFactor
        val speed = when {
            inputCrouch -> baseSpeed * 0.56f
            inputAds -> baseSpeed * 0.65f
            wantSprint -> baseSpeed * 1.56f
            else -> baseSpeed
        }

        // True analog 360-degree movement relative to camera yaw
        val normMag = rawMag.coerceAtMost(1f)
        val dirX = if (moving) (rightX * inputMoveX + forwardX * inputMoveY) / rawMag else 0f
        val dirZ = if (moving) (rightZ * inputMoveX + forwardZ * inputMoveY) / rawMag else 0f

        val moveWorldX = dirX * normMag * speed * dt
        val moveWorldZ = dirZ * normMag * speed * dt

        val resolved = resolveCircleCollision(playerX + moveWorldX, playerZ + moveWorldZ, 0.55f)
        playerX = resolved.first
        playerZ = resolved.second

        // Smoothly orient actor body: face aim direction when shooting/ADS, or follow movement angle when sprinting/roaming
        val desiredBodyYaw = if (moving && !inputAds && !inputFiring && muzzleFlashTimer <= 0f) {
            atan2(-dirX, -dirZ)
        } else {
            playerYaw
        }
        playerBodyYaw = lerpAngle(playerBodyYaw, desiredBodyYaw, (dt * 12f).coerceAtMost(1f))

        if (!onGround) {
            playerVy -= 18.5f * dt
            playerY += playerVy * dt
            if (playerY <= 0f) {
                playerY = 0f
                playerVy = 0f
                onGround = true
            }
        }

        playerStance = when {
            inputCrouch -> PlayerStance.CROUCH
            inputAds -> PlayerStance.AIM
            wantSprint -> PlayerStance.SPRINT
            moving -> PlayerStance.WALK
            else -> PlayerStance.IDLE
        }

        if (moving) {
            val rate = if (wantSprint) 11.5f else if (inputCrouch) 5.8f else 8.2f
            playerAnimPhase += dt * rate * normMag.coerceAtLeast(0.45f)
        } else {
            playerAnimPhase += dt * 1.8f
        }
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = (b - a) % (2f * PI.toFloat())
        if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        if (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        return a + diff * t
    }

    private fun updateWeaponAndFiring(dt: Float) {
        if (!inputFiring || fireCooldown > 0f || reloadTimer > 0f) return
        val weapon = equippedWeapons[activeSlot]
        if (magAmmoMap[activeSlot] <= 0) {
            if (reserveAmmoMap[activeSlot] > 0) {
                reloadDuration = weapon.reloadMs / 1000f
                reloadTimer = reloadDuration
                soundEngine.playReload()
            }
            return
        }

        magAmmoMap[activeSlot] -= 1
        fireCooldown = weapon.fireRateMs / 1000f
        muzzleFlashTimer = 0.07f
        recoilKick = if (weapon == WeaponType.TACTICAL_SHOTGUN || weapon == WeaponType.MARKSMAN_SNIPER) 1f else 0.55f
        soundEngine.playGunshot(weapon)

        val eyeY = playerY + if (inputCrouch) 1.05f else 1.58f
        val dirX = -sin(playerYaw) * cos(playerPitch)
        val dirY = sin(playerPitch)
        val dirZ = -cos(playerYaw) * cos(playerPitch)

        val spreadRad = Math.toRadians((weapon.spreadDeg * if (inputAds) 0.28f else 1f).toDouble()).toFloat()
        val muzzleX = playerX + dirX * 0.8f + cos(playerYaw) * 0.22f
        val muzzleY = eyeY - 0.12f + dirY * 0.5f
        val muzzleZ = playerZ + dirZ * 0.8f - sin(playerYaw) * 0.22f

        repeat(weapon.pellets) {
            val rx = dirX + (Random.nextFloat() - 0.5f) * spreadRad
            val ry = dirY + (Random.nextFloat() - 0.5f) * spreadRad
            val rz = dirZ + (Random.nextFloat() - 0.5f) * spreadRad
            val len = sqrt(rx * rx + ry * ry + rz * rz).coerceAtLeast(0.001f)
            val ndx = rx / len
            val ndy = ry / len
            val ndz = rz / len

            val hitResult = raycastCombatShot(
                originX = playerX,
                originY = eyeY,
                originZ = playerZ,
                dirX = ndx,
                dirY = ndy,
                dirZ = ndz,
                maxDist = weapon.rangeMeters
            )
            val endX = playerX + ndx * hitResult.distance
            val endY = eyeY + ndy * hitResult.distance
            val endZ = playerZ + ndz * hitResult.distance

            tracers.add(BulletTracer(muzzleX, muzzleY, muzzleZ, endX, endY, endZ, 0xFFFBBF24))

            val hitZombie = hitResult.zombie
            if (hitZombie != null) {
                val dmg = if (hitResult.headshot) (weapon.damage * 1.7f).toInt() else weapon.damage
                damageZombie(hitZombie, dmg, hitResult.headshot, endX, endY, endZ)
            } else if (hitResult.hitWorld) {
                spawnImpactSparks(endX, endY, endZ, 0xFFE2E8F0, 5)
            }
        }
    }

    private data class RaycastResult(
        val distance: Float,
        val zombie: ZombieActor? = null,
        val headshot: Boolean = false,
        val hitWorld: Boolean = false
    )

    private fun raycastCombatShot(
        originX: Float,
        originY: Float,
        originZ: Float,
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        maxDist: Float
    ): RaycastResult {
        var bestDist = maxDist
        var hitWorld = false

        for (box in worldBoxes) {
            if (!box.collidable) continue
            val t = intersectRayBox(
                originX, originY, originZ,
                dirX, dirY, dirZ,
                box.x - box.sx * 0.5f, box.x + box.sx * 0.5f,
                box.y - box.sy * 0.5f, box.y + box.sy * 0.5f,
                box.z - box.sz * 0.5f, box.z + box.sz * 0.5f
            )
            if (t in 0.2f..bestDist) {
                bestDist = t
                hitWorld = true
            }
        }

        var bestZombie: ZombieActor? = null
        var bestHeadshot = false
        for (zmb in zombies) {
            if (zmb.dead) continue
            val radius = 0.58f * zmb.type.scale
            val height = 1.9f * zmb.type.scale
            val ocX = zmb.x - originX
            val ocZ = zmb.z - originZ
            val proj = ocX * dirX + ocZ * dirZ
            if (proj <= 0.2f || proj >= bestDist) continue
            val closestX = originX + dirX * proj
            val closestY = originY + dirY * proj
            val closestZ = originZ + dirZ * proj
            val dx = closestX - zmb.x
            val dz = closestZ - zmb.z
            if (dx * dx + dz * dz <= radius * radius && closestY in 0f..height) {
                bestDist = proj
                bestZombie = zmb
                bestHeadshot = closestY >= height * 0.72f
                hitWorld = false
            }
        }
        return RaycastResult(bestDist, bestZombie, bestHeadshot, hitWorld)
    }

    private fun intersectRayBox(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        minX: Float, maxX: Float,
        minY: Float, maxY: Float,
        minZ: Float, maxZ: Float
    ): Float {
        var tMin = 0f
        var tMax = 999f
        fun slab(o: Float, d: Float, lo: Float, hi: Float): Boolean {
            if (kotlin.math.abs(d) < 1e-5f) {
                return o in lo..hi
            }
            var t1 = (lo - o) / d
            var t2 = (hi - o) / d
            if (t1 > t2) {
                val tmp = t1; t1 = t2; t2 = tmp
            }
            tMin = maxOf(tMin, t1)
            tMax = minOf(tMax, t2)
            return tMin <= tMax
        }
        if (!slab(ox, dx, minX, maxX)) return -1f
        if (!slab(oy, dy, minY, maxY)) return -1f
        if (!slab(oz, dz, minZ, maxZ)) return -1f
        return tMin
    }

    private fun damageZombie(zmb: ZombieActor, dmg: Int, headshot: Boolean, hx: Float, hy: Float, hz: Float) {
        zmb.hp -= dmg
        zmb.hitFlashTimer = 0.14f
        hitMarkerTimer = 0.16f
        hitMarkerHeadshot = headshot
        soundEngine.playHitMarker(headshot)
        spawnImpactSparks(hx, hy, hz, 0xFFDC2626, 8)

        if (zmb.hp <= 0 && !zmb.dead) {
            zmb.dead = true
            zmb.deathTimer = 2.2f
            sessionKills++
            val ammoBonus = if (Random.nextFloat() < 0.35f) 4 else 0
            val medBonus = if (zmb.type == ZombieType.TOXIC || Random.nextFloat() < 0.15f) 1 else 0
            val bioBonus = if (zmb.type == ZombieType.TOXIC || zmb.type == ZombieType.BRUTE) 1 else 0
            callbacks.onRewardsEarned(
                scrap = zmb.type.scrapReward,
                meds = medBonus,
                ammo = ammoBonus,
                fuel = 0,
                bio = bioBonus,
                kills = 1,
                xp = zmb.type.xpReward
            )
            if (ammoBonus > 0) {
                val w = equippedWeapons[activeSlot]
                reserveAmmoMap[activeSlot] = (reserveAmmoMap[activeSlot] + 8).coerceAtMost(w.maxReserve)
            }
        }
    }

    private fun updateSquadAi(dt: Float) {
        val count = squadmates.size.coerceAtLeast(1)
        squadmates.forEachIndexed { idx, sq ->
            if (sq.muzzleFlashTimer > 0f) sq.muzzleFlashTimer = (sq.muzzleFlashTimer - dt).coerceAtLeast(0f)
            if (sq.fireCooldown > 0f) sq.fireCooldown = (sq.fireCooldown - dt).coerceAtLeast(0f)
            if (sq.healCooldown > 0f) sq.healCooldown = (sq.healCooldown - dt).coerceAtLeast(0f)

            if (sq.callsign == "PULSE" && sq.healCooldown <= 0f && health < maxHealth) {
                val distP = hypot(playerX - sq.x, playerZ - sq.z)
                if (distP < 12f) {
                    health = (health + 8).coerceAtMost(maxHealth)
                    sq.healCooldown = 6f
                    showRadio("PULSE", "Patching you up, Commander. Stay in the fight!")
                }
            }

            val angleOffset = ((idx - (count - 1) * 0.5f) * 1.0f) + PI.toFloat() * 0.85f
            val targetAngle = playerYaw + angleOffset
            val formX = playerX - sin(targetAngle) * 3.4f
            val formZ = playerZ - cos(targetAngle) * 3.4f

            val dx = formX - sq.x
            val dz = formZ - sq.z
            val dist = hypot(dx, dz)

            if (dist > 0.9f) {
                val spd = if (dist > 7f) 6.6f else 4.2f
                val step = minOf(dist, spd * dt)
                val nx = sq.x + (dx / dist) * step
                val nz = sq.z + (dz / dist) * step
                val res = resolveCircleCollision(nx, nz, 0.45f)
                sq.x = res.first
                sq.z = res.second
                sq.stance = if (dist > 7f) PlayerStance.SPRINT else PlayerStance.WALK
                sq.animPhase += dt * (if (dist > 7f) 10f else 7f)
            } else {
                sq.stance = PlayerStance.IDLE
                sq.animPhase += dt * 1.6f
            }

            val target = zombies.filter { !it.dead }.minByOrNull { hypot(it.x - sq.x, it.z - sq.z) }
            if (target != null) {
                val tx = target.x - sq.x
                val tz = target.z - sq.z
                val tDist = hypot(tx, tz)
                if (tDist < 26f) {
                    sq.yaw = atan2(-tx, -tz)
                    if (sq.fireCooldown <= 0f) {
                        sq.fireCooldown = (sq.weapon.fireRateMs / 1000f) * 1.85f + 0.25f
                        sq.muzzleFlashTimer = 0.06f
                        val dmg = ((sq.weapon.damage * 0.55f + sq.level * 5f) * villageSquadDmgMult).toInt()
                        tracers.add(
                            BulletTracer(
                                sq.x, 1.35f, sq.z,
                                target.x, 1.25f, target.z,
                                sq.accentHex
                            )
                        )
                        damageZombie(target, dmg, false, target.x, 1.2f, target.z)
                    }
                } else if (dist > 0.9f) {
                    sq.yaw = atan2(-dx, -dz)
                }
            } else if (dist > 0.9f) {
                sq.yaw = atan2(-dx, -dz)
            }
        }
    }

    private fun updateZombies(dt: Float) {
        val iterator = zombies.iterator()
        while (iterator.hasNext()) {
            val zmb = iterator.next()
            if (zmb.hitFlashTimer > 0f) zmb.hitFlashTimer = (zmb.hitFlashTimer - dt).coerceAtLeast(0f)
            if (zmb.stunnedTimer > 0f) zmb.stunnedTimer = (zmb.stunnedTimer - dt).coerceAtLeast(0f)
            if (zmb.attackCooldown > 0f) zmb.attackCooldown = (zmb.attackCooldown - dt).coerceAtLeast(0f)

            if (zmb.dead) {
                zmb.deathTimer -= dt
                if (zmb.deathTimer <= 0f) iterator.remove()
                continue
            }
            if (zmb.stunnedTimer > 0f) continue

            val dx = playerX - zmb.x
            val dz = playerZ - zmb.z
            val dist = hypot(dx, dz)

            val inSanctuarySafeCore = zmb.z > 37f && zmb.x in -24f..24f
            if (inSanctuarySafeCore) {
                zmb.z -= dt * 3f
            }

            if (dist < 44f && !isDowned) {
                zmb.yaw = atan2(-dx, -dz)
                if (dist > 1.45f) {
                    val spd = zmb.type.speed
                    val nx = zmb.x + (dx / dist) * spd * dt
                    val nz = zmb.z + (dz / dist) * spd * dt
                    val resolved = resolveCircleCollision(nx, nz, 0.45f * zmb.type.scale)
                    zmb.x = resolved.first
                    zmb.z = resolved.second
                    zmb.animPhase += dt * spd * 2.6f
                } else if (zmb.attackCooldown <= 0f) {
                    zmb.attackCooldown = 1.15f
                    applyDamageToPlayer(zmb.type.damage, zmb.x, zmb.z)
                    soundEngine.playZombieGrowl()
                }
            } else {
                zmb.animPhase += dt * 1.4f
            }
        }
    }

    private fun applyDamageToPlayer(rawDamage: Int, sourceX: Float, sourceZ: Float) {
        if (isDowned) return
        lastDamageAngle = atan2(sourceX - playerX, sourceZ - playerZ) - playerYaw
        damageFlashTimer = 1f
        var remaining = rawDamage
        if (armor > 0) {
            val absorbed = minOf(armor, (remaining * 0.7f).toInt().coerceAtLeast(1))
            armor -= absorbed
            remaining -= absorbed
        }
        health = (health - remaining).coerceAtLeast(0)
        if (health <= 0) {
            isDowned = true
            showRadio("SANCTUARY MEDEVAC", "Vital signs critical! Tap EMERGENCY EVAC to redeploy.")
        }
    }

    private fun updateOutpostCapture(dt: Float) {
        for (op in outposts) {
            if (op.captured) continue
            val dist = hypot(playerX - op.x, playerZ - op.z)
            if (dist <= 9.5f) {
                if (!op.waveSpawned) {
                    op.waveSpawned = true
                    showRadio("RECON", "Capturing ${op.name}! Hold the perimeter against incoming hostiles!")
                    repeat(5) { idx ->
                        val ang = idx * (2f * PI.toFloat() / 5f)
                        val sx = op.x + cos(ang) * 16f
                        val sz = op.z + sin(ang) * 16f
                        val type = if (idx == 0) ZombieType.BRUTE else if (idx % 2 == 0) ZombieType.RUNNER else ZombieType.WALKER
                        zombies.add(ZombieActor(nextZombieId++, type, sx, sz, 0f, type.maxHp, type.maxHp))
                    }
                }
                op.progress = (op.progress + dt * 0.14f).coerceAtMost(1f)
                if (op.progress >= 1f) {
                    op.captured = true
                    soundEngine.playSuccessChime()
                    callbacks.onOutpostCaptured(op.id)
                    worldBoxes = WorldGenerator.buildWorldGeometry(
                        outposts.map {
                            OutpostEntity(it.id, it.name, it.zoneName, it.x, it.z, it.captured, 1, "")
                        }
                    )
                    showRadio("SANCTUARY HQ", "${op.name} secured! New civilization beacon online.")
                }
            }
        }
    }

    private fun tryInteractNearby() {
        val nearSurvivor = survivors.firstOrNull { !it.rescued && hypot(playerX - it.x, playerZ - it.z) < 4.5f }
        if (nearSurvivor != null) {
            nearSurvivor.rescued = true
            soundEngine.playSuccessChime()
            callbacks.onSurvivorRescued(nearSurvivor.name)
            showRadio(nearSurvivor.name.uppercase(), nearSurvivor.message)
            spawnImpactSparks(nearSurvivor.x, 1.4f, nearSurvivor.z, 0xFF22C55E, 14)
            return
        }

        val nearCrate = crates.firstOrNull { !it.collected && hypot(playerX - it.x, playerZ - it.z) < 3.8f }
        if (nearCrate != null) {
            nearCrate.collected = true
            soundEngine.playReload()
            for (i in 0..2) {
                val w = equippedWeapons[i]
                reserveAmmoMap[i] = (reserveAmmoMap[i] + nearCrate.ammoAmount).coerceAtMost(w.maxReserve)
            }
            fragCount += 1
            armor = (armor + 20).coerceAtMost(maxArmor)
            callbacks.onRewardsEarned(
                scrap = nearCrate.scrapAmount,
                meds = nearCrate.medAmount,
                ammo = nearCrate.ammoAmount,
                fuel = nearCrate.fuelAmount,
                bio = 0,
                kills = 0,
                xp = 45
            )
            showRadio("SCAVENGE", "${nearCrate.label}: +${nearCrate.scrapAmount} Scrap, +${nearCrate.ammoAmount} Ammo, +1 Frag")
            spawnImpactSparks(nearCrate.x, 0.8f, nearCrate.z, 0xFFF59E0B, 10)
        }
    }

    private fun activateGear(type: TacticalGearType) {
        when (type) {
            TacticalGearType.MED_STIM -> {
                if (medStimCount <= 0 || (health >= maxHealth && armor >= maxArmor)) return
                medStimCount--
                health = (health + 55).coerceAtMost(maxHealth)
                armor = (armor + 30).coerceAtMost(maxArmor)
                soundEngine.playMedStim()
                showRadio("BIO-STIM", "Trauma stim applied. Vitals stabilized.")
            }
            TacticalGearType.FRAG_GRENADE -> {
                if (fragCount <= 0) return
                fragCount--
                launchGrenade(TacticalGearType.FRAG_GRENADE)
            }
            TacticalGearType.SHOCK_FLARE -> {
                if (shockFlareCount <= 0) return
                shockFlareCount--
                launchGrenade(TacticalGearType.SHOCK_FLARE)
            }
        }
    }

    private fun launchGrenade(type: TacticalGearType) {
        val dirX = -sin(playerYaw) * cos(playerPitch)
        val dirY = sin(playerPitch) + 0.35f
        val dirZ = -cos(playerYaw) * cos(playerPitch)
        grenades.add(
            ActiveGrenade(
                x = playerX + dirX * 0.8f,
                y = playerY + 1.45f,
                z = playerZ + dirZ * 0.8f,
                vx = dirX * 14f,
                vy = dirY * 11f,
                vz = dirZ * 14f,
                type = type
            )
        )
    }

    private fun updateGrenades(dt: Float) {
        val it = grenades.iterator()
        while (it.hasNext()) {
            val g = it.next()
            g.vy -= 18f * dt
            g.x += g.vx * dt
            g.y += g.vy * dt
            g.z += g.vz * dt
            if (g.y < 0.2f) {
                g.y = 0.2f
                g.vy = -g.vy * 0.35f
                g.vx *= 0.65f
                g.vz *= 0.65f
            }
            g.fuse -= dt
            if (g.fuse <= 0f) {
                if (g.type == TacticalGearType.FRAG_GRENADE) {
                    soundEngine.playExplosion()
                    spawnImpactSparks(g.x, g.y + 0.4f, g.z, 0xFFF97316, 28)
                    zombies.filter { !it.dead && hypot(it.x - g.x, it.z - g.z) < 8.5f }.forEach { z ->
                        val d = hypot(z.x - g.x, z.z - g.z)
                        val dmg = (180f * (1f - d / 9.5f)).toInt().coerceAtLeast(35)
                        damageZombie(z, dmg, false, z.x, 1.1f, z.z)
                    }
                } else {
                    soundEngine.playExplosion()
                    spawnImpactSparks(g.x, g.y + 0.4f, g.z, 0xFF38BDF8, 26)
                    zombies.filter { !it.dead && hypot(it.x - g.x, it.z - g.z) < 11f }.forEach { z ->
                        z.stunnedTimer = 4.5f
                        damageZombie(z, 35, false, z.x, 1.1f, z.z)
                    }
                }
                it.remove()
            }
        }
    }

    private fun updateParticlesAndTracers(dt: Float) {
        val tIt = tracers.iterator()
        while (tIt.hasNext()) {
            val t = tIt.next()
            t.life -= dt
            if (t.life <= 0f) tIt.remove()
        }
        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vy -= p.gravity * dt
            if (p.y < 0.05f) p.y = 0.05f
            p.life -= dt
            if (p.life <= 0f) pIt.remove()
        }
    }

    private fun updateAtmosphereParticles(dt: Float) {
        atmosphereTimer -= dt
        if (atmosphereTimer > 0f || particles.size > 70) return
        atmosphereTimer = 0.14f
        // Sanctuary campfire embers & drifting city ash around player
        if (hypot(playerX - 0f, playerZ - 60f) < 34f) {
            particles.add(
                WorldParticle(
                    x = (Random.nextFloat() - 0.5f) * 1.0f,
                    y = 0.9f,
                    z = 60f + (Random.nextFloat() - 0.5f) * 1.0f,
                    vx = (Random.nextFloat() - 0.5f) * 0.4f,
                    vy = 1.6f + Random.nextFloat() * 1.2f,
                    vz = (Random.nextFloat() - 0.5f) * 0.4f,
                    size = 0.10f,
                    colorHex = 0xFFF59E0B,
                    life = 1.2f,
                    maxLife = 1.2f,
                    gravity = -0.3f
                )
            )
        } else {
            particles.add(
                WorldParticle(
                    x = playerX + (Random.nextFloat() - 0.5f) * 26f,
                    y = 1.2f + Random.nextFloat() * 5f,
                    z = playerZ + (Random.nextFloat() - 0.5f) * 26f,
                    vx = 0.5f + Random.nextFloat() * 0.6f,
                    vy = -0.25f,
                    vz = (Random.nextFloat() - 0.5f) * 0.4f,
                    size = 0.08f,
                    colorHex = 0xFF94A3B8,
                    life = 2.0f,
                    maxLife = 2.0f,
                    gravity = 0.05f
                )
            )
        }
    }

    private fun updateDynamicSpawns(dt: Float) {
        zombieSpawnTimer -= dt
        if (zombieSpawnTimer > 0f) return
        zombieSpawnTimer = 5.5f
        val aliveCount = zombies.count { !it.dead }
        if (aliveCount >= 22) return
        val zone = WorldGenerator.determineZone(playerX, playerZ)
        if (zone == WorldZone.SANCTUARY_VALLEY) return
        spawnZoneHorde(zone, 2)
    }

    private fun spawnZoneHorde(zone: WorldZone, count: Int) {
        repeat(count) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val rad = 12f + Random.nextFloat() * 22f
            val sx = (zone.centerX + cos(ang) * rad).coerceIn(-94f, 94f)
            val sz = (zone.centerZ + sin(ang) * rad).coerceIn(-94f, 24f)
            val roll = Random.nextFloat()
            val type = when {
                zone == WorldZone.BIOCORE_SPIRE && roll < 0.28f -> ZombieType.BRUTE
                zone == WorldZone.BIOCORE_SPIRE && roll < 0.55f -> ZombieType.TOXIC
                roll < 0.30f -> ZombieType.RUNNER
                roll < 0.44f -> ZombieType.TOXIC
                roll < 0.54f -> ZombieType.BRUTE
                else -> ZombieType.WALKER
            }
            val resolved = resolveCircleCollision(sx, sz, 0.6f)
            zombies.add(
                ZombieActor(
                    id = nextZombieId++,
                    type = type,
                    x = resolved.first,
                    z = resolved.second,
                    yaw = Random.nextFloat() * 6.28f,
                    hp = type.maxHp,
                    maxHp = type.maxHp
                )
            )
        }
    }

    private fun spawnImpactSparks(x: Float, y: Float, z: Float, colorHex: Long, count: Int) {
        repeat(count) {
            particles.add(
                WorldParticle(
                    x = x,
                    y = y,
                    z = z,
                    vx = (Random.nextFloat() - 0.5f) * 6f,
                    vy = 1.5f + Random.nextFloat() * 4.5f,
                    vz = (Random.nextFloat() - 0.5f) * 6f,
                    size = 0.14f + Random.nextFloat() * 0.14f,
                    colorHex = colorHex,
                    life = 0.45f + Random.nextFloat() * 0.25f,
                    maxLife = 0.7f
                )
            )
        }
    }

    private fun resolveCircleCollision(x: Float, z: Float, radius: Float): Pair<Float, Float> {
        var cx = x.coerceIn(-WorldGenerator.WORLD_HALF_EXTENT, WorldGenerator.WORLD_HALF_EXTENT)
        var cz = z.coerceIn(-WorldGenerator.WORLD_HALF_EXTENT, WorldGenerator.WORLD_HALF_EXTENT)
        for (box in worldBoxes) {
            if (!box.collidable || box.y - box.sy * 0.5f > 2.0f) continue
            val minX = box.x - box.sx * 0.5f
            val maxX = box.x + box.sx * 0.5f
            val minZ = box.z - box.sz * 0.5f
            val maxZ = box.z + box.sz * 0.5f
            val nearestX = cx.coerceIn(minX, maxX)
            val nearestZ = cz.coerceIn(minZ, maxZ)
            val dx = cx - nearestX
            val dz = cz - nearestZ
            val distSq = dx * dx + dz * dz
            if (distSq < radius * radius) {
                val dist = sqrt(distSq)
                if (dist > 0.0001f) {
                    val push = radius - dist
                    cx += (dx / dist) * push
                    cz += (dz / dist) * push
                } else {
                    cx += radius
                }
            }
        }
        return cx to cz
    }

    fun showRadio(speaker: String, message: String) {
        radioSpeaker = speaker
        radioMessage = message
        radioTimer = 4.8f
    }

    private fun pushHudSnapshot() {
        val nearSurvivor = survivors.firstOrNull { !it.rescued && hypot(playerX - it.x, playerZ - it.z) < 4.5f }
        val nearCrate = crates.firstOrNull { !it.collected && hypot(playerX - it.x, playerZ - it.z) < 3.8f }
        val interactText = when {
            nearSurvivor != null -> "RESCUE ${nearSurvivor.name.uppercase()}"
            nearCrate != null -> "SCAVENGE ${nearCrate.label.uppercase()}"
            else -> null
        }

        val activeOp = outposts.firstOrNull { !it.captured && hypot(playerX - it.x, playerZ - it.z) <= 9.5f }

        // Check enemy currently under crosshair for tactical target readout
        val eyeY = playerY + if (inputCrouch) 1.05f else 1.58f
        val dirX = -sin(playerYaw) * cos(playerPitch)
        val dirY = sin(playerPitch)
        val dirZ = -cos(playerYaw) * cos(playerPitch)
        val aimScan = raycastCombatShot(playerX, eyeY, playerZ, dirX, dirY, dirZ, 75f)
        val targetZombie = aimScan.zombie

        val blips = buildList {
            zombies.forEach { if (!it.dead) add(MiniMapBlip(it.x, it.z, BlipKind.ZOMBIE)) }
            squadmates.forEach { add(MiniMapBlip(it.x, it.z, BlipKind.SQUAD, it.callsign)) }
            survivors.forEach { if (!it.rescued) add(MiniMapBlip(it.x, it.z, BlipKind.SURVIVOR, it.name)) }
            crates.forEach { if (!it.collected) add(MiniMapBlip(it.x, it.z, BlipKind.CRATE)) }
            outposts.forEach {
                add(
                    MiniMapBlip(
                        it.x,
                        it.z,
                        if (it.captured) BlipKind.OUTPOST_SECURED else BlipKind.OUTPOST_HOSTILE,
                        it.name
                    )
                )
            }
        }

        onHudUpdated?.invoke(
            HudState(
                health = health,
                maxHealth = maxHealth,
                armor = armor,
                maxArmor = maxArmor,
                stamina = stamina,
                perspective = perspective,
                stance = playerStance,
                isCrouching = inputCrouch,
                isAds = inputAds,
                isSprinting = playerStance == PlayerStance.SPRINT,
                activeSlot = activeSlot,
                weapons = equippedWeapons.toList(),
                magAmmo = magAmmoMap[activeSlot],
                reserveAmmo = reserveAmmoMap[activeSlot],
                isReloading = reloadTimer > 0f,
                reloadProgress = if (reloadTimer > 0f && reloadDuration > 0f) 1f - (reloadTimer / reloadDuration) else 0f,
                fragCount = fragCount,
                medStimCount = medStimCount,
                shockFlareCount = shockFlareCount,
                playerX = playerX,
                playerZ = playerZ,
                playerYaw = playerYaw,
                currentZone = WorldGenerator.determineZone(playerX, playerZ),
                killCountSession = sessionKills,
                hitMarkerActive = hitMarkerTimer > 0f,
                hitMarkerHeadshot = hitMarkerHeadshot,
                damageFlash = damageFlashTimer,
                damageAngleRad = lastDamageAngle,
                interactPrompt = interactText,
                activeOutpostName = activeOp?.name,
                activeOutpostProgress = activeOp?.progress ?: 0f,
                targetEnemyName = targetZombie?.type?.displayName,
                targetEnemyHpFrac = if (targetZombie != null) (targetZombie.hp.toFloat() / targetZombie.maxHp).coerceIn(0f, 1f) else 0f,
                radioSpeaker = radioSpeaker,
                radioMessage = radioMessage,
                mapBlips = blips,
                isDowned = isDowned
            )
        )
    }
}
