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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
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

    private lateinit var meshes: GlMeshBuffers
    private lateinit var textures: TextureAtlas
    private lateinit var humanRenderer: HumanoidMeshRenderer

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

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

        meshes = GlMeshBuffers()
        textures = TextureAtlas().apply { initTextures() }
        humanRenderer = HumanoidMeshRenderer(::drawShape, textures)
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
        Matrix.perspectiveM(projMatrix, 0, currentFov, aspect, 0.1f, 260f)

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
            targetX = camX + lookDirX * 12f
            targetY = camY + lookDirY * 12f + engine.recoilKick * 0.18f
            targetZ = camZ + lookDirZ * 12f
        } else {
            val rightX = cos(engine.playerYaw)
            val rightZ = -sin(engine.playerYaw)
            val shoulderOffset = if (engine.inputAds) 0.48f else 0.62f
            val backDist = if (engine.inputAds) 2.0f else 3.45f
            val pivotY = engine.playerY + crouchY + 0.15f
            val desiredX = engine.playerX - lookDirX * backDist + rightX * shoulderOffset
            val desiredY = (pivotY + 0.35f - lookDirY * backDist).coerceAtLeast(0.32f)
            val desiredZ = engine.playerZ - lookDirZ * backDist + rightZ * shoulderOffset

            // Stepped raycast camera clamp so looking up/down/360 never clips inside walls
            val clamped = clampCameraRay(
                engine.playerX, pivotY, engine.playerZ,
                desiredX, desiredY, desiredZ
            )
            camX = clamped[0]
            camY = clamped[1]
            camZ = clamped[2]
            targetX = engine.playerX + lookDirX * 16f + rightX * 0.25f
            targetY = pivotY + lookDirY * 16f + engine.recoilKick * 0.25f
            targetZ = engine.playerZ + lookDirZ * 16f + rightZ * 0.25f
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

        // 0. Atmospheric Twilight Sky Dome (cull front faces since camera is inside sphere)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glCullFace(GLES20.GL_FRONT)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, camX, camY - 12f, camZ)
        Matrix.scaleM(modelMatrix, 0, 220f, 180f, 220f)
        drawShape(MeshShape.SPHERE, modelMatrix, textures.whiteTex, 0xFF1A2636, 1f, 1f, 0.9f, 0f)
        // Warm dusk sun halo on northwest horizon
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, camX + 68f, camY + 34f, camZ - 95f)
        Matrix.scaleM(modelMatrix, 0, 14f, 14f, 14f)
        drawShape(MeshShape.SPHERE, modelMatrix, textures.whiteTex, 0xFFF59E0B, 1f, 1f, 1.0f, 0f)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 1. Ground Terrain Plane
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.05f, 0f)
        Matrix.scaleM(modelMatrix, 0, 260f, 0.1f, 260f)
        drawShape(MeshShape.CUBE, modelMatrix, textures.terrainTex, 0xFFB0B5A6, 42f, 42f, 0f, 0.08f)

        // 2. World Architecture, Silos, Trees, Vehicles & Props
        for (box in engine.worldBoxes) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, box.x, box.y, box.z)
            Matrix.scaleM(modelMatrix, 0, box.sx, box.sy, box.sz)
            val (tex, tint, uvScale, emissive, spec) = when (box.material) {
                MaterialKind.ASPHALT -> Tuple5(textures.asphaltTex, 0xFF9EA5AE, maxOf(box.sx, box.sz) * 0.25f, 0f, 0.35f)
                MaterialKind.CONCRETE_RUIN -> Tuple5(textures.concreteTex, 0xFFB4B8BF, 2.2f, 0f, 0.16f)
                MaterialKind.SKYSCRAPER_WINDOW -> Tuple5(textures.windowTex, 0xFFD0D8E2, 3.0f, 0.05f, 0.5f)
                MaterialKind.RUST_METAL -> Tuple5(textures.metalTex, 0xFF847061, 1.6f, 0f, 0.58f)
                MaterialKind.CONTAINER_BLUE -> Tuple5(textures.metalTex, 0xFF335D82, 1.8f, 0f, 0.48f)
                MaterialKind.CONTAINER_RED -> Tuple5(textures.metalTex, 0xFF823632, 1.8f, 0f, 0.48f)
                MaterialKind.WOOD_TIMBER -> Tuple5(textures.woodTex, 0xFFBC9E7C, 1.8f, 0f, 0.12f)
                MaterialKind.FOLIAGE -> Tuple5(textures.foliageTex, 0xFF5D7F4F, 1.5f, 0f, 0.05f)
                MaterialKind.SANDBAG -> Tuple5(textures.fabricTex, 0xFFA39377, 2.0f, 0f, 0.08f)
                MaterialKind.GLOW_AMBER -> Tuple5(textures.whiteTex, 0xFFF59E0B, 1f, 0.94f, 0f)
                MaterialKind.GLOW_CYAN -> Tuple5(textures.whiteTex, 0xFF38BDF8, 1f, 0.96f, 0f)
                MaterialKind.GLOW_CRIMSON -> Tuple5(textures.whiteTex, 0xFFEF4444, 1f, 0.96f, 0f)
            }
            drawShape(box.shape, modelMatrix, tex, tint, uvScale, uvScale, emissive, spec)
        }

        // 3. Supply Crates & Outpost Beacon Rings
        for (crate in engine.crates) {
            if (crate.collected) continue
            val pulse = 0.4f + 0.3f * sin(globalTime * 4f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, crate.x, 0.45f, crate.z)
            Matrix.scaleM(modelMatrix, 0, 1.15f, 0.9f, 0.85f)
            drawShape(MeshShape.CUBE, modelMatrix, textures.metalTex, 0xFF4C6B42, 1f, 1f, 0f, 0.5f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, crate.x, 0.94f, crate.z)
            Matrix.scaleM(modelMatrix, 0, 0.45f, 0.08f, 0.45f)
            drawShape(MeshShape.CUBE, modelMatrix, textures.whiteTex, 0xFFF59E0B, 1f, 1f, pulse, 0f)
        }

        for (op in engine.outposts) {
            val ringColor = if (op.captured) 0xFF22C55E else 0xFFF59E0B
            for (corner in 0 until 8) {
                val ang = corner * (Math.PI * 2.0 / 8.0).toFloat() + globalTime * 0.25f
                val px = op.x + cos(ang) * 9.0f
                val pz = op.z + sin(ang) * 9.0f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, px, 0.22f, pz)
                Matrix.scaleM(modelMatrix, 0, 0.35f, 0.35f, 0.35f)
                drawShape(MeshShape.SPHERE, modelMatrix, textures.whiteTex, ringColor, 1f, 1f, 0.8f, 0f)
            }
        }

        // 4. Stranded Survivors
        for (surv in engine.survivors) {
            if (surv.rescued) continue
            humanRenderer.drawHumanSoldier(
                x = surv.x, y = 0f, z = surv.z,
                yawRad = surv.yaw, pitchRad = 0f,
                stance = PlayerStance.IDLE, animPhase = globalTime * 1.8f,
                loadout = ArmorLoadout(), weapon = WeaponType.COMBAT_PISTOL,
                recoilKick = 0f, muzzleFlash = false,
                isReloading = false, reloadProgress = 0f,
                accentOverrideHex = 0xFF22C55E
            )
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, surv.x, 2.35f + sin(globalTime * 3.5f) * 0.12f, surv.z)
            Matrix.rotateM(modelMatrix, 0, globalTime * 90f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.26f, 0.26f, 0.26f)
            drawShape(MeshShape.SPHERE, modelMatrix, textures.whiteTex, 0xFF22C55E, 1f, 1f, 0.9f, 0f)
        }

        // 5. Warrior Squadmates
        for (sq in engine.squadmates) {
            humanRenderer.drawHumanSoldier(
                x = sq.x, y = 0f, z = sq.z,
                yawRad = sq.yaw, pitchRad = 0f,
                stance = sq.stance, animPhase = sq.animPhase,
                loadout = ArmorLoadout(heavyPauldrons = true, tacticalPack = true),
                weapon = sq.weapon,
                recoilKick = if (sq.muzzleFlashTimer > 0f) 0.6f else 0f,
                muzzleFlash = sq.muzzleFlashTimer > 0f,
                isReloading = false, reloadProgress = 0f,
                accentOverrideHex = sq.accentHex
            )
        }

        // 6. Zombies
        for (zmb in engine.zombies) {
            humanRenderer.drawZombie(zmb)
        }

        // 7. Grenades, Particles, Bullet Tracers
        for (g in engine.grenades) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, g.x, g.y, g.z)
            Matrix.scaleM(modelMatrix, 0, 0.24f, 0.24f, 0.24f)
            val col = if (g.type == TacticalGearType.SHOCK_FLARE) 0xFF38BDF8 else 0xFFEF4444
            drawShape(MeshShape.SPHERE, modelMatrix, textures.whiteTex, col, 1f, 1f, 0.9f, 0f)
        }

        for (p in engine.particles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size)
            drawShape(MeshShape.SPHERE, modelMatrix, textures.whiteTex, p.colorHex, 1f, 1f, 0.9f, 0f)
        }

        for (tr in engine.tracers) {
            drawTracerBeam(tr)
        }

        // 8. Player Actor (3rd-Person) OR Weapon Viewmodel (1st-Person)
        if (engine.perspective == CameraPerspective.THIRD_PERSON) {
            val shownYaw = if (engine.inputAds || engine.inputFiring || engine.muzzleFlashTimer > 0f) {
                engine.playerYaw
            } else {
                engine.playerBodyYaw
            }
            humanRenderer.drawHumanSoldier(
                x = engine.playerX,
                y = engine.playerY,
                z = engine.playerZ,
                yawRad = shownYaw,
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
        drawShape(MeshShape.CUBE, modelMatrix, textures.whiteTex, tr.colorHex, 1f, 1f, 1.0f, 0f)
    }

    private fun clampCameraRay(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float
    ): FloatArray {
        val steps = 8
        var safeT = 1f
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val px = ox + (dx - ox) * t
            val py = oy + (dy - oy) * t
            val pz = oz + (dz - oz) * t
            var hit = false
            for (box in engine.worldBoxes) {
                if (!box.collidable) continue
                val minX = box.x - box.sx * 0.5f - 0.25f
                val maxX = box.x + box.sx * 0.5f + 0.25f
                val minY = box.y - box.sy * 0.5f - 0.25f
                val maxY = box.y + box.sy * 0.5f + 0.25f
                val minZ = box.z - box.sz * 0.5f - 0.25f
                val maxZ = box.z + box.sz * 0.5f + 0.25f
                if (px in minX..maxX && py in minY..maxY && pz in minZ..maxZ) {
                    hit = true
                    break
                }
            }
            if (hit) {
                safeT = ((i - 1).toFloat() / steps).coerceAtLeast(0.22f)
                break
            }
        }
        return floatArrayOf(
            ox + (dx - ox) * safeT,
            (oy + (dy - oy) * safeT).coerceAtLeast(0.28f),
            oz + (dz - oz) * safeT
        )
    }

    private fun drawShape(
        shape: MeshShape,
        model: FloatArray,
        texId: Int,
        colorHex: Long,
        uvScaleX: Float,
        uvScaleY: Float,
        emissive: Float,
        specular: Float
    ) {
        val (buf, count) = when (shape) {
            MeshShape.CUBE -> meshes.cubeBuffer to meshes.cubeVertexCount
            MeshShape.CYLINDER -> meshes.cylinderBuffer to meshes.cylinderVertexCount
            MeshShape.CONE -> meshes.coneBuffer to meshes.coneVertexCount
            MeshShape.SPHERE -> meshes.sphereBuffer to meshes.sphereVertexCount
        }
        val stride = 8 * 4
        buf.position(0)
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, stride, buf)
        buf.position(3)
        GLES20.glEnableVertexAttribArray(normHandle)
        GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, stride, buf)
        buf.position(6)
        GLES20.glEnableVertexAttribArray(uvHandle)
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, stride, buf)

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

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    companion object {
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
                vec3 sunDir = normalize(vec3(0.45, 0.72, -0.52));
                float ndl = max(dot(N, sunDir), 0.0);
                vec3 skyAmb = mix(vec3(0.14, 0.16, 0.21), vec3(0.32, 0.38, 0.48), N.y * 0.5 + 0.5);
                vec3 sunCol = vec3(1.0, 0.86, 0.68) * ndl * 0.96;

                vec3 toFlash = uFlashPos - vWorldPos;
                float flashDist = length(toFlash);
                float flashAtten = uFlashStrength / (1.0 + 0.14 * flashDist * flashDist);
                vec3 flashLight = vec3(1.0, 0.72, 0.28) * flashAtten * max(dot(N, normalize(toFlash)), 0.15);

                vec3 V = normalize(uCamPos - vWorldPos);
                vec3 H = normalize(sunDir + V);
                float specPow = pow(max(dot(N, H), 0.0), 28.0) * uSpec * ndl;

                vec3 lit = texSample * (skyAmb + sunCol + flashLight) + vec3(specPow);
                lit = mix(lit, texSample * 1.32, uEmissive);

                float dist = length(uCamPos - vWorldPos);
                float fog = clamp(1.0 - exp(-0.0095 * dist), 0.0, 0.80);
                vec3 fogColor = vec3(0.11, 0.14, 0.20);
                vec3 finalCol = mix(lit, fogColor, fog * (1.0 - uEmissive * 0.82));
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
