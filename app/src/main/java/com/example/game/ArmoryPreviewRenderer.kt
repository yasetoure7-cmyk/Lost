package com.example.game

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.ArmorLoadout
import com.example.data.PlayerStance
import com.example.data.WeaponType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArmoryPreviewRenderer : GLSurfaceView.Renderer {
    @Volatile var loadout: ArmorLoadout = ArmorLoadout()
    @Volatile var weapon: WeaponType = WeaponType.ASSAULT_RIFLE
    @Volatile var rotationDeg: Float = 25f

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
    private val pedestalMat = FloatArray(16)
    private var animTime = 0f
    private var lastNs = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.07f, 0.09f, 0.13f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        program = LostEarthGlRenderer.buildShaderProgram(
            LostEarthGlRenderer.VERTEX_SHADER,
            LostEarthGlRenderer.FRAGMENT_SHADER
        )
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
        lastNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, w, h)
        Matrix.perspectiveM(projMatrix, 0, 42f, w.toFloat() / h.toFloat(), 0.1f, 30f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        lastNs = now
        animTime += dt
        rotationDeg += dt * 18f

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 1.35f, -3.1f, 0f, 0.96f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUniform3f(camPosHandle, 0f, 1.35f, -3.1f)
        GLES20.glUniform3f(flashPosHandle, 1.5f, 2.2f, -2.0f)
        GLES20.glUniform1f(flashStrengthHandle, 0.25f)

        // Round Armory Turntable Pedestal
        Matrix.setIdentityM(pedestalMat, 0)
        Matrix.translateM(pedestalMat, 0, 0f, -0.08f, 0f)
        Matrix.rotateM(pedestalMat, 0, rotationDeg, 0f, 1f, 0f)
        Matrix.scaleM(pedestalMat, 0, 1.6f, 0.14f, 1.6f)
        drawShape(MeshShape.CYLINDER, pedestalMat, textures.metalTex, 0xFF28323E, 1f, 1f, 0.05f, 0.75f)

        humanRenderer.drawHumanSoldier(
            x = 0f,
            y = 0f,
            z = 0f,
            yawRad = Math.toRadians(rotationDeg.toDouble()).toFloat(),
            pitchRad = 0f,
            stance = PlayerStance.AIM,
            animPhase = animTime * 1.8f,
            loadout = loadout,
            weapon = weapon,
            recoilKick = 0f,
            muzzleFlash = false,
            isReloading = false,
            reloadProgress = 0f
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
}

class ArmoryPreviewGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer = ArmoryPreviewRenderer()
    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
