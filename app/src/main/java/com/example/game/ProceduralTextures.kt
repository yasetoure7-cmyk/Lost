package com.example.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.math.sin
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
        terrainTex = uploadBitmap(createTerrainTexture(256))
        asphaltTex = uploadBitmap(createAsphaltTexture(256))
        concreteTex = uploadBitmap(createConcreteTexture(256))
        windowTex = uploadBitmap(createSkyscraperTexture(256))
        metalTex = uploadBitmap(createCorrugatedTexture(256))
        woodTex = uploadBitmap(createPlankTexture(256))
        foliageTex = uploadBitmap(createFoliageTexture(128))
        fabricTex = uploadBitmap(createTacticalWeaveTexture(128))
        whiteTex = uploadBitmap(createSolidWhiteTexture())
    }

    private fun createSolidWhiteTexture(): Bitmap {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        return bmp
    }

    private fun createTerrainTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(101)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val wave = (sin(x * 0.08) * sin(y * 0.08)).toFloat()
                val patch = (wave + 1f) * 0.5f
                val n = rng.nextInt(-14, 15)
                val r = (54 * patch + 74 * (1f - patch) + n).toInt().coerceIn(0, 255)
                val g = (66 * patch + 68 * (1f - patch) + n).toInt().coerceIn(0, 255)
                val b = (42 * patch + 52 * (1f - patch) + n).toInt().coerceIn(0, 255)
                bmp.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bmp
    }

    private fun createConcreteTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(42)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val n = rng.nextInt(-18, 19)
                var v = 104 + n
                if (x % 64 == 0 || y % 64 == 0) v -= 24
                if ((x + y) % 97 == 0 && rng.nextFloat() < 0.4f) v -= 30
                val c = v.coerceIn(20, 220)
                bmp.setPixel(x, y, Color.rgb(c, c + 1, c + 3))
            }
        }
        return bmp
    }

    private fun createAsphaltTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(202)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val grit = rng.nextInt(-12, 13)
                val v = (46 + grit).coerceIn(18, 90)
                bmp.setPixel(x, y, Color.rgb(v, v + 2, v + 5))
            }
        }
        val canvas = Canvas(bmp)
        val lanePaint = Paint().apply {
            color = Color.rgb(196, 158, 64)
            strokeWidth = 6f
        }
        canvas.drawLine(size * 0.5f, 8f, size * 0.5f, size * 0.36f, lanePaint)
        canvas.drawLine(size * 0.5f, size * 0.58f, size * 0.5f, size * 0.90f, lanePaint)
        val edgePaint = Paint().apply {
            color = Color.rgb(125, 130, 136)
            strokeWidth = 4f
        }
        canvas.drawLine(8f, 0f, 8f, size.toFloat(), edgePaint)
        canvas.drawLine(size - 8f, 0f, size - 8f, size.toFloat(), edgePaint)
        return bmp
    }

    private fun createSkyscraperTexture(size: Int): Bitmap {
        val bmp = createConcreteTexture(size)
        val canvas = Canvas(bmp)
        val rng = Random(99)
        val framePaint = Paint().apply { color = Color.rgb(42, 46, 54) }
        val glassDark = Paint().apply { color = Color.rgb(18, 26, 36) }
        val glassReflect = Paint().apply { color = Color.rgb(52, 68, 84) }
        val glassAmber = Paint().apply { color = Color.rgb(120, 86, 42) }
        val step = size / 4
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val left = col * step + 7f
                val top = row * step + 8f
                canvas.drawRect(left - 2f, top - 2f, left + step - 12f, top + step - 16f, framePaint)
                val p = when {
                    rng.nextFloat() < 0.14f -> glassAmber
                    rng.nextFloat() < 0.35f -> glassReflect
                    else -> glassDark
                }
                canvas.drawRect(left, top, left + step - 14f, top + step - 18f, p)
            }
        }
        return bmp
    }

    private fun createCorrugatedTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(77)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val rib = (sin(x * 0.42) * 26.0).toInt()
                val rust = if (rng.nextFloat() < 0.09f) 22 else 0
                val v = (172 + rib + rng.nextInt(-10, 11)).coerceIn(0, 255)
                bmp.setPixel(
                    x, y,
                    Color.rgb((v + rust).coerceAtMost(255), v, (v - rust / 2).coerceAtLeast(0))
                )
            }
        }
        return bmp
    }

    private fun createPlankTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(17)
        for (y in 0 until size) {
            val seam = (y % 32 == 0)
            for (x in 0 until size) {
                val grain = rng.nextInt(-10, 11) + (sin(x * 0.25 + y * 0.05) * 8.0).toInt()
                val baseR = if (seam) 48 else 132 + grain
                val baseG = if (seam) 36 else 98 + grain
                val baseB = if (seam) 24 else 68 + grain
                bmp.setPixel(
                    x, y,
                    Color.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), baseB.coerceIn(0, 255))
                )
            }
        }
        return bmp
    }

    private fun createFoliageTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(55)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val n = rng.nextInt(-22, 23)
                val r = (40 + n).coerceIn(10, 100)
                val g = (68 + n).coerceIn(22, 130)
                val b = (34 + n).coerceIn(10, 80)
                bmp.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bmp
    }

    private fun createTacticalWeaveTexture(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(88)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val weave = if (((x / 3) + (y / 3)) % 2 == 0) 10 else -10
                val n = rng.nextInt(-6, 7)
                val v = (204 + weave + n).coerceIn(140, 245)
                bmp.setPixel(x, y, Color.rgb(v, v, v))
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
