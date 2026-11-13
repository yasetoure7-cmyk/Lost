package com.example.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.random.Random

class TextureAtlas {
    var terrainTex = 0
    var asphaltTex = 0
    var concreteTex = 0
    var windowTex = 0
    var metalTex = 0
    var woodTex = 0
    var foliageTex = 0
    var fabricTex = 0
    var whiteTex = 0

    fun initTextures() {
        terrainTex = uploadBitmap(createNoiseTexture(128, Color.rgb(66, 72, 58), Color.rgb(48, 52, 42), 16, gridLines = false))
        asphaltTex = uploadBitmap(createAsphaltTexture(128))
        concreteTex = uploadBitmap(createNoiseTexture(128, Color.rgb(112, 114, 118), Color.rgb(86, 88, 94), 22, gridLines = true))
        windowTex = uploadBitmap(createSkyscraperTexture(128))
        metalTex = uploadBitmap(createCorrugatedTexture(128))
        woodTex = uploadBitmap(createPlankTexture(128))
        foliageTex = uploadBitmap(createNoiseTexture(64, Color.rgb(46, 78, 42), Color.rgb(28, 52, 26), 28, gridLines = false))
        fabricTex = uploadBitmap(createNoiseTexture(64, Color.rgb(210, 214, 220), Color.rgb(185, 190, 198), 10, gridLines = false))
        whiteTex = uploadBitmap(createSolidWhiteTexture())
    }

    private fun createSolidWhiteTexture(): Bitmap {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        return bmp
    }

    private fun createNoiseTexture(
        size: Int,
        c1: Int,
        c2: Int,
        jitter: Int,
        gridLines: Boolean
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(42)
        val r1 = Color.red(c1); val g1 = Color.green(c1); val b1 = Color.blue(c1)
        val r2 = Color.red(c2); val g2 = Color.green(c2); val b2 = Color.blue(c2)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val mix = rng.nextFloat()
                val n = rng.nextInt(-jitter, jitter + 1)
                var r = (r1 * mix + r2 * (1f - mix)).toInt() + n
                var g = (g1 * mix + g2 * (1f - mix)).toInt() + n
                var b = (b1 * mix + b2 * (1f - mix)).toInt() + n
                if (gridLines && (x % 32 == 0 || y % 32 == 0)) {
                    r -= 18; g -= 18; b -= 18
                }
                bmp.setPixel(x, y, Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
            }
        }
        return bmp
    }

    private fun createAsphaltTexture(size: Int): Bitmap {
        val bmp = createNoiseTexture(size, Color.rgb(54, 58, 64), Color.rgb(42, 45, 50), 12, gridLines = false)
        val canvas = Canvas(bmp)
        val lanePaint = Paint().apply {
            color = Color.rgb(198, 162, 68)
            strokeWidth = 4f
        }
        canvas.drawLine(size * 0.5f, 4f, size * 0.5f, size * 0.35f, lanePaint)
        canvas.drawLine(size * 0.5f, size * 0.6f, size * 0.5f, size * 0.92f, lanePaint)
        return bmp
    }

    private fun createSkyscraperTexture(size: Int): Bitmap {
        val bmp = createNoiseTexture(size, Color.rgb(88, 92, 100), Color.rgb(66, 70, 76), 12, gridLines = false)
        val canvas = Canvas(bmp)
        val rng = Random(99)
        val glassDark = Paint().apply { color = Color.rgb(22, 30, 40) }
        val glassAmber = Paint().apply { color = Color.rgb(88, 72, 45) }
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val left = col * 32f + 5f
                val top = row * 32f + 6f
                val p = if (rng.nextFloat() < 0.16f) glassAmber else glassDark
                canvas.drawRect(left, top, left + 22f, top + 19f, p)
            }
        }
        return bmp
    }

    private fun createCorrugatedTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(77)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val rib = if ((x / 6) % 2 == 0) 20 else -18
                val rust = if (rng.nextFloat() < 0.08f) 16 else 0
                val v = (175 + rib + rng.nextInt(-10, 11)).coerceIn(0, 255)
                bmp.setPixel(x, y, Color.rgb((v + rust).coerceAtMost(255), v, (v - rust / 2).coerceAtLeast(0)))
            }
        }
        return bmp
    }

    private fun createPlankTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(17)
        for (y in 0 until size) {
            val seam = (y % 21 == 0)
            for (x in 0 until size) {
                val grain = rng.nextInt(-12, 13)
                val baseR = if (seam) 55 else 128 + grain
                val baseG = if (seam) 42 else 96 + grain
                val baseB = if (seam) 30 else 66 + grain
                bmp.setPixel(
                    x,
                    y,
                    Color.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), baseB.coerceIn(0, 255))
                )
            }
        }
        return bmp
    }

    private fun uploadBitmap(bitmap: Bitmap): Int {
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        val id = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bitmap.recycle()
        return id
    }
}
