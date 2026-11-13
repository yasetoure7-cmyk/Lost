package com.example.game

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.ArmorLoadout
import com.example.data.CameraPerspective
import com.example.data.PlayerStance
import com.example.data.TacticalGearType
import com.example.data.WeaponType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LostEarthGlRenderer(
    private val engine: GameEngine
) : GLSurfaceView.Renderer {

    private var program = 0
    private var posHandle = 0
    private var normHandle = 0
    private var uvHandle = 0
    private var mvpHandle = 0
    private var modelHandle = 0
    private var texHandle = 0
    private var colorHandle = 0
    private var uvScaleHandle = 0
    private var emissiveHandle = 0
    private var specHandle = 0
    private var camPosHandle = 0
    private var flashPosHandle = 0
    private var flashStrengthHandle = 0

    private lateinit var cubeBuffer: FloatBuffer
    private lateinit var textures: TextureAtlas
    private lateinit var humanRenderer: HumanoidMeshRenderer

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    private var screenWidth = 1280
    private var screenHeight = 720
    private var lastFrameNs = System.nanoTime()
    private var currentFov = 58f
    private var globalTime = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.11f, 0.16f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        program = buildShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        posHandle = GLES20.glGetAttribLocation(program, "aPos")
        normHandle = GLES20.glGetAttribLocation(program, "aNormal")
        uvHandle = GLES20.glGetAttribLocation(program, "aUv")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp")
        modelHandle = GLES20.glGetUniformLocation(program, "uModel")
        texHandle = GLES20.glGetUniformLocation(program, "uTex")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        uvScaleHandle = GLES20.glGetUniformLocation(program, "uUvScale")
        emissiveHandle = GLES20.glGetUniformLocation(program, "uEmissive")
        specHandle = GLES20.glGetUniformLocation(program, "uSpec")
        camPosHandle = GLES20.glGetUniformLocation(program, "uCamPos")
        flashPosHandle = GLES20.glGetUniformLocation(program, "uFlashPos")
        flashStrengthHandle = GLES20.glGetUniformLocation(program, "uFlashStrength")

        cubeBuffer = buildUnitCubeBuffer()
        textures = TextureAtlas().apply { initTextures() }
        humanRenderer = HumanoidMeshRenderer(::drawTexturedBox, textures)
        Matrix.setIdentityM(identityMatrix, 0)
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        lastFrameNs = nowNs
        globalTime += dt

        engine.stepSimulation(dt)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val weapon = engine.equippedWeapons[engine.activeSlot]
        val targetFov = if (engine.inputAds) weapon.adsFov else 58f
        currentFov += (targetFov - currentFov) * (dt * 12f).coerceAtMost(1f)
        val aspect = screenWidth.toFloat() / screenHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, currentFov, aspect, 0.1f, 220f)

        // Compute Camera (First-Person vs Third-Person Over-the-Shoulder)
        val crouchY = if (engine.inputCrouch) 1.05f else 1.58f
        val lookDirX = -sin(engine.playerYaw) * cos(engine.playerPitch)
        val lookDirY = sin(engine.playerPitch)
        val lookDirZ = -cos(engine.playerYaw) * cos(engine.playerPitch)

        val camX: Float
        val camY: Float
        val camZ: Float
        val targetX: Float
        val targetY: Float
        val targetZ: Float

        if (engine.perspective == CameraPerspective.FIRST_PERSON) {
            val bob = if (engine.playerStance == PlayerStance.WALK || engine.playerStance == PlayerStance.SPRINT) {
                sin(engine.playerAnimPhase * 2f) * 0.03f
            } else 0f
            camX = engine.playerX
            camY = engine.playerY + crouchY + bob
            camZ = engine.playerZ
            targetX = camX + lookDirX * 10f
            targetY = camY + lookDirY * 10f + engine.recoilKick * 0.18f
            targetZ = camZ + lookDirZ * 10f
        } else {
            val rightX = cos(engine.playerYaw)
            val rightZ = -sin(engine.playerYaw)
            val shoulderOffset = if (engine.inputAds) 0.48f else 0.62f
            val backDist = if (engine.inputAds) 2.0f else 3.35f
            val desiredX = engine.playerX - lookDirX * backDist + rightX * shoulderOffset
            val desiredY = (engine.playerY + crouchY + 0.55f - lookDirY * (backDist * 0.65f)).coerceAtLeast(0.45f)
            val desiredZ = engine.playerZ - lookDirZ * backDist + rightZ * shoulderOffset

            // Clamp camera against buildings so it never clips into walls
            val clamped = clampCameraDistance(
                engine.playerX,
                engine.playerY + crouchY,
                engine.playerZ,
                desiredX,
                desiredY,
                desiredZ
            )
            camX = clamped[0]
            camY = clamped[1]
            camZ = clamped[2]
            targetX = engine.playerX + lookDirX * 14f + rightX * 0.25f
            targetY = engine.playerY + crouchY + lookDirY * 14f + engine.recoilKick * 0.25f
            targetZ = engine.playerZ + lookDirZ * 14f + rightZ * 0.25f
        }

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, targetX, targetY, targetZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUniform3f(camPosHandle, camX, camY, camZ)
        GLES20.glUniform3f(
            flashPosHandle,
            engine.playerX + lookDirX * 1.1f,
            engine.playerY + crouchY,
            engine.playerZ + lookDirZ * 1.1f
        )
        GLES20.glUniform1f(flashStrengthHandle, if (engine.muzzleFlashTimer > 0f) 1.0f else 0.0f)

        bindCubeVertexAttribs()

        // 1. Ground Terrain Plane
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.05f, 0f)
        Matrix.scaleM(modelMatrix, 0, 240f, 0.1f, 240f)
        drawTexturedBox(modelMatrix, textures.terrainTex, 0xFFA4A89A, 38f, 38f, 0f, 0.08f)

        // 2. World Architecture & Props
        for (box in engine.worldBoxes) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, box.x, box.y, box.z)
            Matrix.scaleM(modelMatrix, 0, box.sx, box.sy, box.sz)
            val (tex, tint, uvScale, emissive, spec) = when (box.material) {
                MaterialKind.ASPHALT -> Tuple5(textures.asphaltTex, 0xFF9EA5AE, maxOf(box.sx, box.sz) * 0.25f, 0f, 0.25f)
                MaterialKind.CONCRETE_RUIN -> Tuple5(textures.concreteTex, 0xFFB0B4BA, 2.2f, 0f, 0.15f)
                MaterialKind.SKYSCRAPER_WINDOW -> Tuple5(textures.windowTex, 0xFFCBD2DC, 3.0f, 0.04f, 0.45f)
                MaterialKind.RUST_METAL -> Tuple5(textures.metalTex, 0xFF7D6B5D, 1.5f, 0f, 0.55f)
                MaterialKind.CONTAINER_BLUE -> Tuple5(textures.metalTex, 0xFF335D82, 1.8f, 0f, 0.45f)
                MaterialKind.CONTAINER_RED -> Tuple5(textures.metalTex, 0xFF7D3431, 1.8f, 0f, 0.45f)
                MaterialKind.WOOD_TIMBER -> Tuple5(textures.woodTex, 0xFFBC9E7C, 1.8f, 0f, 0.12f)
                MaterialKind.FOLIAGE -> Tuple5(textures.foliageTex, 0xFF5D7F4F, 1.5f, 0f, 0.05f)
                MaterialKind.SANDBAG -> Tuple5(textures.fabricTex, 0xFFA39377, 2.0f, 0f, 0.08f)
                MaterialKind.GLOW_AMBER -> Tuple5(textures.whiteTex, 0xFFF59E0B, 1f, 0.92f, 0f)
                MaterialKind.GLOW_CYAN -> Tuple5(textures.whiteTex, 0xFF38BDF8, 1f, 0.95f, 0f)
                MaterialKind.GLOW_CRIMSON -> Tuple5(textures.whiteTex, 0xFFEF4444, 1f, 0.95f, 0f)
            }
            drawTexturedBox(modelMatrix, tex, tint, uvScale, uvScale, emissive, spec)
        }

        // 3. Supply Crates & Outpost Beacon Rings
        for (crate in engine.crates) {
            if (crate.collected) continue
            val pulse = 0.35f + 0.25f * sin(globalTime * 4f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, crate.x, 0.45f, crate.z)
            Matrix.scaleM(modelMatrix, 0, 1.15f, 0.9f, 0.85f)
            drawTexturedBox(modelMatrix, textures.metalTex, 0xFF4C6B42, 1f, 1f, 0f, 0.5f)
            // Glowing top latch
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, crate.x, 0.94f, crate.z)
            Matrix.scaleM(modelMatrix, 0, 0.45f, 0.08f, 0.45f)
            drawTexturedBox(modelMatrix, textures.whiteTex, 0xFFF59E0B, 1f, 1f, pulse, 0f)
        }

        // Outpost perimeter markers
        for (op in engine.outposts) {
            val ringColor = if (op.captured) 0xFF22C55E else 0xFFF59E0B
            for (corner in 0 until 8) {
                val ang = corner * (Math.PI * 2.0 / 8.0).toFloat() + globalTime * 0.25f
                val px = op.x + cos(ang) * 9.0f
                val pz = op.z + sin(ang) * 9.0f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, px, 0.22f, pz)
                Matrix.scaleM(modelMatrix, 0, 0.35f, 0.35f, 0.35f)
                drawTexturedBox(modelMatrix, textures.whiteTex, ringColor, 1f, 1f, 0.75f, 0f)
            }
        }

        // 4. Stranded Survivors (human allies waiting to be rescued)
        for (surv in engine.survivors) {
            if (surv.rescued) continue
            humanRenderer.drawHumanSoldier(
                x = surv.x,
                y = 0f,
                z = surv.z,
                yawRad = surv.yaw,
                pitchRad = 0f,
                stance = PlayerStance.IDLE,
                animPhase = globalTime * 1.8f,
                loadout = ArmorLoadout(),
                weapon = WeaponType.COMBAT_PISTOL,
                recoilKick = 0f,
                muzzleFlash = false,
                isReloading = false,
                reloadProgress = 0f,
                accentOverrideHex = 0xFF22C55E
            )
            // Beacon diamond above survivor
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, surv.x, 2.35f + sin(globalTime * 3.5f) * 0.12f, surv.z)
            Matrix.rotateM(modelMatrix, 0, globalTime * 90f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.26f, 0.26f, 0.26f)
            drawTexturedBox(modelMatrix, textures.whiteTex, 0xFF22C55E, 1f, 1f, 0.9f, 0f)
        }

        // 5. Warrior Squadmates
        for (sq in engine.squadmates) {
            humanRenderer.drawHumanSoldier(
                x = sq.x,
                y = 0f,
                z = sq.z,
                yawRad = sq.yaw,
                pitchRad = 0f,
                stance = sq.stance,
                animPhase = sq.animPhase,
                loadout = ArmorLoadout(heavyPauldrons = true, tacticalPack = true),
                weapon = sq.weapon,
                recoilKick = if (sq.muzzleFlashTimer > 0f) 0.6f else 0f,
                muzzleFlash = sq.muzzleFlashTimer > 0f,
                isReloading = false,
                reloadProgress = 0f,
                accentOverrideHex = sq.accentHex
            )
        }

        // 6. Zombies
        for (zmb in engine.zombies) {
            humanRenderer.drawZombie(zmb)
        }

        // 7. Thrown Grenades & Particles & Bullet Tracers
        for (g in engine.grenades) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, g.x, g.y, g.z)
            Matrix.scaleM(modelMatrix, 0, 0.24f, 0.24f, 0.24f)
            val col = if (g.type == TacticalGearType.SHOCK_FLARE) 0xFF38BDF8 else 0xFFEF4444
            drawTexturedBox(modelMatrix, textures.whiteTex, col, 1f, 1f, 0.9f, 0f)
        }

        for (p in engine.particles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size)
            drawTexturedBox(modelMatrix, textures.whiteTex, p.colorHex, 1f, 1f, 0.9f, 0f)
        }

        for (tr in engine.tracers) {
            drawTracerBeam(tr)
        }

        // 8. Player Actor (3rd-Person) OR Weapon Viewmodel (1st-Person)
        if (engine.perspective == CameraPerspective.THIRD_PERSON) {
            humanRenderer.drawHumanSoldier(
                x = engine.playerX,
                y = engine.playerY,
                z = engine.playerZ,
                yawRad = engine.playerYaw,
                pitchRad = engine.playerPitch,
                stance = engine.playerStance,
                animPhase = engine.playerAnimPhase,
                loadout = engine.armorLoadout,
                weapon = weapon,
                recoilKick = engine.recoilKick,
                muzzleFlash = engine.muzzleFlashTimer > 0f,
                isReloading = engine.reloadTimer > 0f,
                reloadProgress = if (engine.reloadDuration > 0f) 1f - (engine.reloadTimer / engine.reloadDuration) else 0f
            )
        } else {
            // Switch vpMatrix to identity-view (eye-space) and clear depth for crisp viewmodel!
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
            Matrix.perspectiveM(projMatrix, 0, 58f, aspect, 0.04f, 20f)
            System.arraycopy(projMatrix, 0, vpMatrix, 0, 16)
            GLES20.glUniform3f(camPosHandle, 0f, 0f, 0f)
            humanRenderer.drawFirstPersonWeapon(
                weapon = weapon,
                camoAccentHex = engine.armorLoadout.camo.primaryHex,
                isAds = engine.inputAds,
                isSprinting = engine.playerStance == PlayerStance.SPRINT,
                isReloading = engine.reloadTimer > 0f,
                reloadProgress = if (engine.reloadDuration > 0f) 1f - (engine.reloadTimer / engine.reloadDuration) else 0f,
                bobPhase = engine.playerAnimPhase,
                recoilKick = engine.recoilKick,
                muzzleFlash = engine.muzzleFlashTimer > 0f
            )
        }
    }

    private fun drawTracerBeam(tr: BulletTracer) {
        val dx = tr.x2 - tr.x1
        val dy = tr.y2 - tr.y1
        val dz = tr.z2 - tr.z1
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.05f)
        val midX = (tr.x1 + tr.x2) * 0.5f
        val midY = (tr.y1 + tr.y2) * 0.5f
        val midZ = (tr.z1 + tr.z2) * 0.5f
        val yaw = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
        val horiz = sqrt(dx * dx + dz * dz)
        val pitch = Math.toDegrees(kotlin.math.atan2(-dy.toDouble(), horiz.toDouble())).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, midX, midY, midZ)
        Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitch, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.045f, 0.045f, len)
        drawTexturedBox(modelMatrix, textures.whiteTex, tr.colorHex, 1f, 1f, 1.0f, 0f)
    }

    private fun clampCameraDistance(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float
    ): FloatArray {
        val vx = dx - ox
        val vy = dy - oy
        val vz = dz - oz
        var minFraction = 1f
        for (box in engine.worldBoxes) {
            if (!box.collidable) continue
            val minX = box.x - box.sx * 0.5f - 0.2f
            val maxX = box.x + box.sx * 0.5f + 0.2f
            val minY = box.y - box.sy * 0.5f - 0.2f
            val maxY = box.y + box.sy * 0.5f + 0.2f
            val minZ = box.z - box.sz * 0.5f - 0.2f
            val maxZ = box.z + box.sz * 0.5f + 0.2f
            if (dx in minX..maxX && dy in minY..maxY && dz in minZ..maxZ) {
                minFraction = minOf(minFraction, 0.45f)
            }
        }
        return floatArrayOf(ox + vx * minFraction, oy + vy * minFraction, oz + vz * minFraction)
    }

    private fun drawTexturedBox(
        model: FloatArray,
        texId: Int,
        colorHex: Long,
        uvScaleX: Float,
        uvScaleY: Float,
        emissive: Float,
        specular: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, model, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelHandle, 1, false, model, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(texHandle, 0)

        val argb = colorHex.toInt()
        GLES20.glUniform3f(
            colorHandle,
            Color.red(argb) / 255f,
            Color.green(argb) / 255f,
            Color.blue(argb) / 255f
        )
        GLES20.glUniform2f(uvScaleHandle, uvScaleX, uvScaleY)
        GLES20.glUniform1f(emissiveHandle, emissive)
        GLES20.glUniform1f(specHandle, specular)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }

    private fun bindCubeVertexAttribs() {
        val stride = 8 * 4
        cubeBuffer.position(0)
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, stride, cubeBuffer)

        cubeBuffer.position(3)
        GLES20.glEnableVertexAttribArray(normHandle)
        GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, stride, cubeBuffer)

        cubeBuffer.position(6)
        GLES20.glEnableVertexAttribArray(uvHandle)
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, stride, cubeBuffer)
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    companion object {
        fun buildUnitCubeBuffer(): FloatBuffer {
            val v = floatArrayOf(
                // Front (+Z)
                -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, 1f,
                 0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 1f, 1f,
                 0.5f,  0.5f, 0.5f, 0f, 0f, 1f, 1f, 0f,
                -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, 1f,
                 0.5f,  0.5f, 0.5f, 0f, 0f, 1f, 1f, 0f,
                -0.5f,  0.5f, 0.5f, 0f, 0f, 1f, 0f, 0f,
                // Back (-Z)
                 0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, 1f,
                -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 1f, 1f,
                -0.5f,  0.5f, -0.5f, 0f, 0f, -1f, 1f, 0f,
                 0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, 1f,
                -0.5f,  0.5f, -0.5f, 0f, 0f, -1f, 1f, 0f,
                 0.5f,  0.5f, -0.5f, 0f, 0f, -1f, 0f, 0f,
                // Left (-X)
                -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, 1f,
                -0.5f, -0.5f,  0.5f, -1f, 0f, 0f, 1f, 1f,
                -0.5f,  0.5f,  0.5f, -1f, 0f, 0f, 1f, 0f,
                -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, 1f,
                -0.5f,  0.5f,  0.5f, -1f, 0f, 0f, 1f, 0f,
                -0.5f,  0.5f, -0.5f, -1f, 0f, 0f, 0f, 0f,
                // Right (+X)
                 0.5f, -0.5f,  0.5f, 1f, 0f, 0f, 0f, 1f,
                 0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 1f, 1f,
                 0.5f,  0.5f, -0.5f, 1f, 0f, 0f, 1f, 0f,
                 0.5f, -0.5f,  0.5f, 1f, 0f, 0f, 0f, 1f,
                 0.5f,  0.5f, -0.5f, 1f, 0f, 0f, 1f, 0f,
                 0.5f,  0.5f,  0.5f, 1f, 0f, 0f, 0f, 0f,
                // Top (+Y)
                -0.5f, 0.5f,  0.5f, 0f, 1f, 0f, 0f, 1f,
                 0.5f, 0.5f,  0.5f, 0f, 1f, 0f, 1f, 1f,
                 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 1f, 0f,
                -0.5f, 0.5f,  0.5f, 0f, 1f, 0f, 0f, 1f,
                 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 1f, 0f,
                -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0f, 0f,
                // Bottom (-Y)
                -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f,
                 0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 1f, 1f,
                 0.5f, -0.5f,  0.5f, 0f, -1f, 0f, 1f, 0f,
                -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f,
                 0.5f, -0.5f,  0.5f, 0f, -1f, 0f, 1f, 0f,
                -0.5f, -0.5f,  0.5f, 0f, -1f, 0f, 0f, 0f
            )
            return ByteBuffer.allocateDirect(v.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(v); position(0) }
        }

        fun buildShaderProgram(vsCode: String, fsCode: String): Int {
            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsCode)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsCode)
            val prog = GLES20.glCreateProgram()
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
            return prog
        }

        private fun compileShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }

        const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            uniform vec2 uUvScale;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv * uUvScale;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTex;
            uniform vec3 uColor;
            uniform float uEmissive;
            uniform float uSpec;
            uniform vec3 uCamPos;
            uniform vec3 uFlashPos;
            uniform float uFlashStrength;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec3 texSample = texture2D(uTex, vUv).rgb * uColor;
                vec3 N = normalize(vNormal);
                vec3 sunDir = normalize(vec3(0.45, 0.78, 0.42));
                float ndl = max(dot(N, sunDir), 0.0);
                vec3 skyAmb = mix(vec3(0.14, 0.16, 0.21), vec3(0.28, 0.34, 0.44), N.y * 0.5 + 0.5);
                vec3 sunCol = vec3(1.0, 0.88, 0.72) * ndl * 0.92;

                // Muzzle flash dynamic point light
                vec3 toFlash = uFlashPos - vWorldPos;
                float flashDist = length(toFlash);
                float flashAtten = uFlashStrength / (1.0 + 0.14 * flashDist * flashDist);
                vec3 flashLight = vec3(1.0, 0.72, 0.28) * flashAtten * max(dot(N, normalize(toFlash)), 0.15);

                // Blinn-Phong specular highlight
                vec3 V = normalize(uCamPos - vWorldPos);
                vec3 H = normalize(sunDir + V);
                float specPow = pow(max(dot(N, H), 0.0), 24.0) * uSpec * ndl;

                vec3 lit = texSample * (skyAmb + sunCol + flashLight) + vec3(specPow);
                lit = mix(lit, texSample * 1.35, uEmissive);

                // Atmospheric twilight fog
                float dist = length(uCamPos - vWorldPos);
                float fog = clamp(1.0 - exp(-0.011 * dist), 0.0, 0.82);
                vec3 fogColor = vec3(0.09, 0.12, 0.17);
                vec3 finalCol = mix(lit, fogColor, fog * (1.0 - uEmissive * 0.8));
                gl_FragColor = vec4(finalCol, 1.0);
            }
        """
    }
}

class LostEarthGLSurfaceView(
    context: Context,
    engine: GameEngine
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(LostEarthGlRenderer(engine))
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
