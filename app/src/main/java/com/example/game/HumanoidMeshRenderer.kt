package com.example.game

import android.opengl.Matrix
import com.example.data.ArmorHelmet
import com.example.data.ArmorLoadout
import com.example.data.ArmorVest
import com.example.data.PlayerStance
import com.example.data.WeaponType
import com.example.data.ZombieType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class HumanoidMeshRenderer(
    private val drawCube: (
        modelMatrix: FloatArray,
        texId: Int,
        colorHex: Long,
        uvScaleX: Float,
        uvScaleY: Float,
        emissive: Float,
        specular: Float
    ) -> Unit,
    private val textures: TextureAtlas
) {
    private val baseMat = FloatArray(16)
    private val partMat = FloatArray(16)
    private val limbMat = FloatArray(16)

    fun drawHumanSoldier(
        x: Float,
        y: Float,
        z: Float,
        yawRad: Float,
        pitchRad: Float,
        stance: PlayerStance,
        animPhase: Float,
        loadout: ArmorLoadout,
        weapon: WeaponType,
        recoilKick: Float,
        muzzleFlash: Boolean,
        isReloading: Boolean,
        reloadProgress: Float,
        accentOverrideHex: Long? = null
    ) {
        val primaryColor = loadout.camo.primaryHex
        val accentColor = accentOverrideHex ?: loadout.camo.accentHex
        val skinTone = 0xFFC79B7B
        val bootColor = 0xFF1D2126
        val gunColor = weapon.barrelColorHex

        val isMoving = stance == PlayerStance.WALK || stance == PlayerStance.SPRINT || stance == PlayerStance.CROUCH
        val strideAmp = when (stance) {
            PlayerStance.SPRINT -> 42f
            PlayerStance.WALK -> 26f
            PlayerStance.CROUCH -> 18f
            else -> 0f
        }
        val crouchDrop = if (stance == PlayerStance.CROUCH) -0.34f else 0f
        val bobY = if (isMoving) abs(sin(animPhase)) * 0.06f else sin(animPhase) * 0.012f
        val torsoLean = when (stance) {
            PlayerStance.SPRINT -> 18f
            PlayerStance.CROUCH -> 14f
            PlayerStance.AIM -> 6f
            else -> 2f
        }

        // Ground contact shadow disc
        Matrix.setIdentityM(baseMat, 0)
        Matrix.translateM(baseMat, 0, x, 0.025f, z)
        Matrix.scaleM(baseMat, 0, 0.85f, 0.01f, 0.85f)
        drawCube(baseMat, textures.whiteTex, 0xFF0A0C10, 1f, 1f, 0f, 0f)

        // Root transform at player feet
        Matrix.setIdentityM(baseMat, 0)
        Matrix.translateM(baseMat, 0, x, y + bobY, z)
        Matrix.rotateM(baseMat, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)

        val legSwing = sin(animPhase) * strideAmp
        val kneeBend = if (stance == PlayerStance.CROUCH) 32f else 6f

        // Left & Right Legs (Thigh + Shin + Combat Boot)
        drawLeg(-0.14f, 0.86f + crouchDrop, legSwing, kneeBend, primaryColor, bootColor)
        drawLeg(0.14f, 0.86f + crouchDrop, -legSwing, kneeBend, primaryColor, bootColor)

        // Pelvis / Tactical Belt
        Matrix.translateM(partMat, 0, baseMat, 0, 0f, 0.88f + crouchDrop, 0f)
        Matrix.scaleM(partMat, 0, 0.42f, 0.16f, 0.26f)
        drawCube(partMat, textures.fabricTex, 0xFF232830, 1f, 1f, 0f, 0.25f)

        // Torso + Armor Vest
        val vestWidth = when (loadout.vest) {
            ArmorVest.LIGHT_CARRIER -> 0.46f
            ArmorVest.ASSAULT_KEVLAR -> 0.50f
            ArmorVest.EXO_REINFORCED -> 0.56f
        }
        val vestDepth = when (loadout.vest) {
            ArmorVest.LIGHT_CARRIER -> 0.28f
            ArmorVest.ASSAULT_KEVLAR -> 0.32f
            ArmorVest.EXO_REINFORCED -> 0.36f
        }

        // Upper body pivot from waist
        val torsoMat = FloatArray(16)
        Matrix.translateM(torsoMat, 0, baseMat, 0, 0f, 0.96f + crouchDrop, 0f)
        Matrix.rotateM(torsoMat, 0, torsoLean, -1f, 0f, 0f)

        // Core chest
        Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.25f, 0f)
        Matrix.scaleM(partMat, 0, vestWidth, 0.48f, vestDepth)
        drawCube(partMat, textures.fabricTex, primaryColor, 1f, 1f, 0f, 0.4f)

        // Chest trauma plate + accent stripe
        Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.27f, -vestDepth * 0.52f)
        Matrix.scaleM(partMat, 0, vestWidth * 0.72f, 0.34f, 0.05f)
        drawCube(partMat, textures.metalTex, accentColor, 1f, 1f, 0.05f, 0.65f)

        // Tactical Backpack
        if (loadout.tacticalPack) {
            Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.26f, vestDepth * 0.68f)
            Matrix.scaleM(partMat, 0, 0.34f, 0.40f, 0.18f)
            drawCube(partMat, textures.fabricTex, 0xFF2A313A, 1f, 1f, 0f, 0.2f)
        }

        // Heavy Pauldrons (Shoulder Armor)
        if (loadout.heavyPauldrons) {
            for (side in floatArrayOf(-1f, 1f)) {
                Matrix.translateM(partMat, 0, torsoMat, 0, side * (vestWidth * 0.6f), 0.42f, 0f)
                Matrix.scaleM(partMat, 0, 0.16f, 0.18f, 0.24f)
                drawCube(partMat, textures.metalTex, accentColor, 1f, 1f, 0f, 0.5f)
            }
        }

        // Head & Helmet
        val headMat = FloatArray(16)
        Matrix.translateM(headMat, 0, torsoMat, 0, 0f, 0.64f, 0f)
        Matrix.rotateM(headMat, 0, Math.toDegrees(pitchRad.toDouble()).toFloat() * 0.45f, 1f, 0f, 0f)

        // Head base
        Matrix.scaleM(partMat, 0, headMat, 0, 0.24f, 0.26f, 0.24f)
        drawCube(partMat, textures.fabricTex, skinTone, 1f, 1f, 0f, 0.1f)

        // Helmet / Visor customization
        when (loadout.helmet) {
            ArmorHelmet.RECON_VISOR -> {
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.07f, 0.01f)
                Matrix.scaleM(partMat, 0, 0.27f, 0.17f, 0.28f)
                drawCube(partMat, textures.metalTex, primaryColor, 1f, 1f, 0f, 0.55f)
                // Glowing tactical visor strip
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.03f, -0.135f)
                Matrix.scaleM(partMat, 0, 0.22f, 0.07f, 0.04f)
                drawCube(partMat, textures.whiteTex, 0xFF38BDF8, 1f, 1f, 0.85f, 0.9f)
            }
            ArmorHelmet.OPS_BALLISTIC -> {
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.06f, 0f)
                Matrix.scaleM(partMat, 0, 0.28f, 0.20f, 0.29f)
                drawCube(partMat, textures.metalTex, 0xFF2C343D, 1f, 1f, 0f, 0.5f)
                // NVG mount
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.07f, -0.16f)
                Matrix.scaleM(partMat, 0, 0.10f, 0.08f, 0.08f)
                drawCube(partMat, textures.metalTex, accentColor, 1f, 1f, 0.35f, 0.8f)
            }
            ArmorHelmet.JUGGERNAUT_HELM -> {
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.03f, 0f)
                Matrix.scaleM(partMat, 0, 0.31f, 0.28f, 0.31f)
                drawCube(partMat, textures.metalTex, primaryColor, 1f, 1f, 0f, 0.65f)
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.04f, -0.165f)
                Matrix.scaleM(partMat, 0, 0.22f, 0.06f, 0.04f)
                drawCube(partMat, textures.whiteTex, 0xFFF59E0B, 1f, 1f, 0.85f, 0.9f)
            }
            ArmorHelmet.GHILLIE_HOOD -> {
                Matrix.translateM(partMat, 0, headMat, 0, 0f, 0.06f, 0.02f)
                Matrix.scaleM(partMat, 0, 0.30f, 0.24f, 0.32f)
                drawCube(partMat, textures.foliageTex, 0xFF4F6142, 1f, 1f, 0f, 0.1f)
            }
        }

        // Arms & Rifle Rig
        val aimRaise = when {
            isReloading -> -18f + sin(reloadProgress * 6.28f) * 20f
            stance == PlayerStance.AIM -> 18f
            stance == PlayerStance.SPRINT -> -24f
            else -> 4f
        }
        val gunPitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat() + aimRaise + recoilKick * 12f

        // Right arm (trigger arm)
        val rArmMat = FloatArray(16)
        Matrix.translateM(rArmMat, 0, torsoMat, 0, vestWidth * 0.58f, 0.38f, 0f)
        Matrix.rotateM(rArmMat, 0, 52f + gunPitchDeg * 0.55f, 1f, 0f, 0f)
        Matrix.translateM(partMat, 0, rArmMat, 0, 0f, -0.18f, 0f)
        Matrix.scaleM(partMat, 0, 0.12f, 0.38f, 0.12f)
        drawCube(partMat, textures.fabricTex, primaryColor, 1f, 1f, 0f, 0.25f)

        // Left arm (support / reload arm)
        val lArmMat = FloatArray(16)
        Matrix.translateM(lArmMat, 0, torsoMat, 0, -vestWidth * 0.58f, 0.38f, 0f)
        val reloadOffset = if (isReloading) sin(reloadProgress * 3.14f) * 28f else 0f
        Matrix.rotateM(lArmMat, 0, 62f + gunPitchDeg * 0.55f - reloadOffset, 1f, 0f, 0f)
        Matrix.rotateM(lArmMat, 0, -24f, 0f, 0f, 1f)
        Matrix.translateM(partMat, 0, lArmMat, 0, 0f, -0.19f, 0f)
        Matrix.scaleM(partMat, 0, 0.115f, 0.40f, 0.115f)
        drawCube(partMat, textures.fabricTex, primaryColor, 1f, 1f, 0f, 0.25f)

        // Held Weapon attached to torso right shoulder
        val gunMat = FloatArray(16)
        Matrix.translateM(gunMat, 0, torsoMat, 0, 0.14f, 0.30f, -0.34f + recoilKick * 0.11f)
        if (stance == PlayerStance.SPRINT) {
            Matrix.rotateM(gunMat, 0, 42f, 0f, 1f, 0f)
            Matrix.rotateM(gunMat, 0, -20f, 1f, 0f, 0f)
        } else {
            Matrix.rotateM(gunMat, 0, Math.toDegrees(pitchRad.toDouble()).toFloat() + recoilKick * 8f, 1f, 0f, 0f)
        }

        val len = weapon.lengthScale
        // Receiver
        Matrix.scaleM(partMat, 0, gunMat, 0, 0.08f, 0.11f, 0.44f * len)
        drawCube(partMat, textures.metalTex, gunColor, 1f, 1f, 0f, 0.75f)
        // Barrel
        Matrix.translateM(partMat, 0, gunMat, 0, 0f, 0.02f, -0.34f * len)
        Matrix.scaleM(partMat, 0, 0.045f, 0.045f, 0.42f * len)
        drawCube(partMat, textures.metalTex, 0xFF1A1E24, 1f, 1f, 0f, 0.85f)
        // Magazine
        Matrix.translateM(partMat, 0, gunMat, 0, 0f, -0.10f, -0.05f)
        Matrix.scaleM(partMat, 0, 0.055f, 0.15f, 0.09f)
        drawCube(partMat, textures.metalTex, 0xFF20252C, 1f, 1f, 0f, 0.6f)

        if (muzzleFlash) {
            Matrix.translateM(partMat, 0, gunMat, 0, 0f, 0.02f, -0.58f * len)
            Matrix.scaleM(partMat, 0, 0.22f, 0.22f, 0.28f)
            drawCube(partMat, textures.whiteTex, 0xFFFBBF24, 1f, 1f, 1.0f, 0f)
        }
    }

    private fun drawLeg(
        offsetX: Float,
        hipY: Float,
        swingDeg: Float,
        kneeBendDeg: Float,
        pantsColor: Long,
        bootColor: Long
    ) {
        Matrix.translateM(limbMat, 0, baseMat, 0, offsetX, hipY, 0f)
        Matrix.rotateM(limbMat, 0, swingDeg, 1f, 0f, 0f)
        // Thigh
        Matrix.translateM(partMat, 0, limbMat, 0, 0f, -0.22f, 0f)
        Matrix.scaleM(partMat, 0, 0.16f, 0.42f, 0.17f)
        drawCube(partMat, textures.fabricTex, pantsColor, 1f, 1f, 0f, 0.2f)

        // Shin + Knee
        val shinMat = FloatArray(16)
        Matrix.translateM(shinMat, 0, limbMat, 0, 0f, -0.42f, 0f)
        Matrix.rotateM(shinMat, 0, -kneeBendDeg - abs(swingDeg) * 0.25f, 1f, 0f, 0f)
        Matrix.translateM(partMat, 0, shinMat, 0, 0f, -0.20f, 0f)
        Matrix.scaleM(partMat, 0, 0.135f, 0.38f, 0.145f)
        drawCube(partMat, textures.fabricTex, 0xFF2B313A, 1f, 1f, 0f, 0.25f)

        // Tactical Boot
        Matrix.translateM(partMat, 0, shinMat, 0, 0f, -0.39f, -0.04f)
        Matrix.scaleM(partMat, 0, 0.145f, 0.11f, 0.23f)
        drawCube(partMat, textures.metalTex, bootColor, 1f, 1f, 0f, 0.4f)
    }

    fun drawFirstPersonWeapon(
        weapon: WeaponType,
        camoAccentHex: Long,
        isAds: Boolean,
        isSprinting: Boolean,
        isReloading: Boolean,
        reloadProgress: Float,
        bobPhase: Float,
        recoilKick: Float,
        muzzleFlash: Boolean,
        viewProjFromEye: Boolean = false
    ) {
        // Rendered in camera-space (origin = eye)
        val adsBlend = if (isAds) 1f else 0f
        val xOff = (0.24f * (1f - adsBlend)) + (0.0f * adsBlend) + cos(bobPhase * 0.5f) * (0.012f * (1f - adsBlend * 0.7f))
        val yOff = (-0.21f * (1f - adsBlend)) + (-0.125f * adsBlend) + abs(sin(bobPhase)) * 0.014f - if (isReloading) sin(reloadProgress * 3.14f) * 0.14f else 0f
        val zOff = -0.46f + recoilKick * 0.07f

        Matrix.setIdentityM(baseMat, 0)
        Matrix.translateM(baseMat, 0, xOff, yOff, zOff)
        if (isSprinting && !isAds) {
            Matrix.rotateM(baseMat, 0, 32f, 0f, 1f, 0f)
            Matrix.rotateM(baseMat, 0, -16f, 1f, 0f, 0f)
        }
        Matrix.rotateM(baseMat, 0, recoilKick * 7.5f, 1f, 0f, 0f)

        val len = weapon.lengthScale
        // Forearm sleeve
        Matrix.translateM(partMat, 0, baseMat, 0, 0.08f, -0.10f, 0.26f)
        Matrix.rotateM(partMat, 0, -14f, 0f, 1f, 0f)
        Matrix.scaleM(partMat, 0, 0.11f, 0.10f, 0.36f)
        drawCube(partMat, textures.fabricTex, camoAccentHex, 1f, 1f, 0f, 0.25f)

        // Receiver body
        Matrix.scaleM(partMat, 0, baseMat, 0, 0.075f, 0.10f, 0.42f * len)
        drawCube(partMat, textures.metalTex, weapon.barrelColorHex, 1f, 1f, 0f, 0.8f)

        // Top rail + glowing holographic optic
        Matrix.translateM(partMat, 0, baseMat, 0, 0f, 0.068f, 0.02f)
        Matrix.scaleM(partMat, 0, 0.048f, 0.036f, 0.14f)
        drawCube(partMat, textures.metalTex, 0xFF15191E, 1f, 1f, 0f, 0.85f)

        Matrix.translateM(partMat, 0, baseMat, 0, 0f, 0.095f, 0.02f)
        Matrix.scaleM(partMat, 0, 0.038f, 0.028f, 0.015f)
        drawCube(partMat, textures.whiteTex, 0xFFF59E0B, 1f, 1f, 0.85f, 0f)

        // Barrel
        Matrix.translateM(partMat, 0, baseMat, 0, 0f, 0.015f, -0.34f * len)
        Matrix.scaleM(partMat, 0, 0.038f, 0.038f, 0.38f * len)
        drawCube(partMat, textures.metalTex, 0xFF171A20, 1f, 1f, 0f, 0.9f)

        if (muzzleFlash) {
            Matrix.translateM(partMat, 0, baseMat, 0, 0f, 0.015f, -0.56f * len)
            Matrix.scaleM(partMat, 0, 0.16f, 0.16f, 0.22f)
            drawCube(partMat, textures.whiteTex, 0xFFFBBF24, 1f, 1f, 1.0f, 0f)
        }
    }

    fun drawZombie(zmb: ZombieActor) {
        val sc = zmb.type.scale
        val (skinColor, clothColor, glowColor) = when (zmb.type) {
            ZombieType.WALKER -> Triple(0xFF6B7B63, 0xFF3E3A36, 0xFFDC2626)
            ZombieType.RUNNER -> Triple(0xFF7B4A4A, 0xFF2F2828, 0xFFF97316)
            ZombieType.BRUTE -> Triple(0xFF4C5549, 0xFF262D35, 0xFFEF4444)
            ZombieType.TOXIC -> Triple(0xFF4F7A46, 0xFF2B3D2C, 0xFF22C55E)
        }

        val flashTint = if (zmb.hitFlashTimer > 0f) 0xFFFFFFFF else skinColor
        val collapseDeg = if (zmb.dead) ((1f - (zmb.deathTimer / 2.2f).coerceIn(0f, 1f)) * 4f).coerceAtMost(1f) * 86f else 0f

        // Ground shadow
        Matrix.setIdentityM(baseMat, 0)
        Matrix.translateM(baseMat, 0, zmb.x, 0.025f, zmb.z)
        Matrix.scaleM(baseMat, 0, 0.85f * sc, 0.01f, 0.85f * sc)
        drawCube(baseMat, textures.whiteTex, 0xFF0A0C10, 1f, 1f, 0f, 0f)

        Matrix.setIdentityM(baseMat, 0)
        Matrix.translateM(baseMat, 0, zmb.x, 0f, zmb.z)
        Matrix.rotateM(baseMat, 0, Math.toDegrees(zmb.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(baseMat, 0, sc, sc, sc)
        if (collapseDeg > 0f) {
            Matrix.rotateM(baseMat, 0, collapseDeg, 1f, 0f, 0f)
        }

        val shamble = sin(zmb.animPhase)
        val legSwing = shamble * (if (zmb.type == ZombieType.RUNNER) 38f else 22f)

        // Legs
        drawLeg(-0.14f, 0.85f, legSwing, 8f, clothColor, 0xFF1F1D1B)
        drawLeg(0.14f, 0.85f, -legSwing, 8f, clothColor, 0xFF1F1D1B)

        // Hunched torso
        val torsoMat = FloatArray(16)
        Matrix.translateM(torsoMat, 0, baseMat, 0, 0f, 0.90f, 0f)
        Matrix.rotateM(torsoMat, 0, 16f + shamble * 5f, -1f, 0f, 0f)
        Matrix.rotateM(torsoMat, 0, shamble * 7f, 0f, 0f, 1f)

        val chestW = if (zmb.type == ZombieType.BRUTE) 0.58f else 0.44f
        Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.25f, 0f)
        Matrix.scaleM(partMat, 0, chestW, 0.48f, 0.28f)
        drawCube(partMat, textures.fabricTex, if (zmb.hitFlashTimer > 0f) 0xFFEF4444 else clothColor, 1f, 1f, 0f, 0.15f)

        // Toxic glowing bio-pustules or Brute armor plates
        if (zmb.type == ZombieType.TOXIC || zmb.type == ZombieType.BRUTE) {
            Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.28f, -0.15f)
            Matrix.scaleM(partMat, 0, 0.24f, 0.22f, 0.06f)
            drawCube(partMat, textures.metalTex, glowColor, 1f, 1f, if (zmb.type == ZombieType.TOXIC) 0.7f else 0.1f, 0.5f)
        }

        // Head + glowing infected eyes
        Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.62f, -0.04f)
        Matrix.scaleM(partMat, 0, 0.24f, 0.25f, 0.24f)
        drawCube(partMat, textures.fabricTex, flashTint, 1f, 1f, 0f, 0.1f)

        if (!zmb.dead) {
            Matrix.translateM(partMat, 0, torsoMat, 0, 0f, 0.64f, -0.165f)
            Matrix.scaleM(partMat, 0, 0.14f, 0.04f, 0.03f)
            drawCube(partMat, textures.whiteTex, glowColor, 1f, 1f, 0.9f, 0f)
        }

        // Outstretched zombie arms
        for (side in floatArrayOf(-1f, 1f)) {
            val armMat = FloatArray(16)
            Matrix.translateM(armMat, 0, torsoMat, 0, side * (chestW * 0.58f), 0.38f, 0f)
            Matrix.rotateM(armMat, 0, 78f + shamble * 12f * side, 1f, 0f, 0f)
            Matrix.translateM(partMat, 0, armMat, 0, 0f, -0.22f, 0f)
            Matrix.scaleM(partMat, 0, 0.11f, 0.44f, 0.11f)
            drawCube(partMat, textures.fabricTex, flashTint, 1f, 1f, 0f, 0.1f)
        }
    }
}
