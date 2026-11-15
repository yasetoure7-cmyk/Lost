package com.example.game

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MeshShape {
    CUBE,
    CYLINDER,
    CONE,
    SPHERE
}

class GlMeshBuffers {
    val cubeBuffer: FloatBuffer
    val cubeVertexCount = 36

    val cylinderBuffer: FloatBuffer
    val cylinderVertexCount: Int

    val coneBuffer: FloatBuffer
    val coneVertexCount: Int

    val sphereBuffer: FloatBuffer
    val sphereVertexCount: Int

    init {
        cubeBuffer = buildUnitCubeBuffer()
        val cyl = buildCylinderBuffer(12)
        cylinderBuffer = cyl.first
        cylinderVertexCount = cyl.second

        val cone = buildConeBuffer(12)
        coneBuffer = cone.first
        coneVertexCount = cone.second

        val sph = buildSphereBuffer(8, 12)
        sphereBuffer = sph.first
        sphereVertexCount = sph.second
    }

    private fun toFloatBuffer(list: List<Float>): FloatBuffer {
        val arr = FloatArray(list.size)
        for (i in list.indices) arr[i] = list[i]
        return ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(arr); position(0) }
    }

    private fun buildUnitCubeBuffer(): FloatBuffer {
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

    private fun buildCylinderBuffer(segments: Int): Pair<FloatBuffer, Int> {
        val verts = mutableListOf<Float>()
        val r = 0.5f
        val h = 0.5f
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float) {
            verts.add(x); verts.add(y); verts.add(z)
            verts.add(nx); verts.add(ny); verts.add(nz)
            verts.add(u); verts.add(v)
        }
        for (i in 0 until segments) {
            val a0 = (i.toDouble() / segments) * 2.0 * PI
            val a1 = ((i + 1).toDouble() / segments) * 2.0 * PI
            val x0 = (cos(a0) * r).toFloat(); val z0 = (sin(a0) * r).toFloat()
            val x1 = (cos(a1) * r).toFloat(); val z1 = (sin(a1) * r).toFloat()
            val nx0 = cos(a0).toFloat(); val nz0 = sin(a0).toFloat()
            val nx1 = cos(a1).toFloat(); val nz1 = sin(a1).toFloat()
            val u0 = i.toFloat() / segments
            val u1 = (i + 1).toFloat() / segments

            // Side quad (CCW outward)
            push(x0, -h, z0, nx0, 0f, nz0, u0, 1f)
            push(x0,  h, z0, nx0, 0f, nz0, u0, 0f)
            push(x1,  h, z1, nx1, 0f, nz1, u1, 0f)

            push(x0, -h, z0, nx0, 0f, nz0, u0, 1f)
            push(x1,  h, z1, nx1, 0f, nz1, u1, 0f)
            push(x1, -h, z1, nx1, 0f, nz1, u1, 1f)

            // Top cap
            push(0f, h, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            push(x1, h, z1, 0f, 1f, 0f, u1, 1f)
            push(x0, h, z0, 0f, 1f, 0f, u0, 1f)

            // Bottom cap
            push(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            push(x0, -h, z0, 0f, -1f, 0f, u0, 0f)
            push(x1, -h, z1, 0f, -1f, 0f, u1, 0f)
        }
        return toFloatBuffer(verts) to (verts.size / 8)
    }

    private fun buildConeBuffer(segments: Int): Pair<FloatBuffer, Int> {
        val verts = mutableListOf<Float>()
        val r = 0.5f
        val h = 0.5f
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float) {
            verts.add(x); verts.add(y); verts.add(z)
            verts.add(nx); verts.add(ny); verts.add(nz)
            verts.add(u); verts.add(v)
        }
        for (i in 0 until segments) {
            val a0 = (i.toDouble() / segments) * 2.0 * PI
            val a1 = ((i + 1).toDouble() / segments) * 2.0 * PI
            val x0 = (cos(a0) * r).toFloat(); val z0 = (sin(a0) * r).toFloat()
            val x1 = (cos(a1) * r).toFloat(); val z1 = (sin(a1) * r).toFloat()
            val nx0 = cos(a0).toFloat() * 0.89f; val nz0 = sin(a0).toFloat() * 0.89f
            val nx1 = cos(a1).toFloat() * 0.89f; val nz1 = sin(a1).toFloat() * 0.89f
            val u0 = i.toFloat() / segments
            val u1 = (i + 1).toFloat() / segments

            push(x0, -h, z0, nx0, 0.45f, nz0, u0, 1f)
            push(0f, h, 0f, (nx0 + nx1) * 0.5f, 0.45f, (nz0 + nz1) * 0.5f, (u0 + u1) * 0.5f, 0f)
            push(x1, -h, z1, nx1, 0.45f, nz1, u1, 1f)

            push(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            push(x0, -h, z0, 0f, -1f, 0f, u0, 0f)
            push(x1, -h, z1, 0f, -1f, 0f, u1, 0f)
        }
        return toFloatBuffer(verts) to (verts.size / 8)
    }

    private fun buildSphereBuffer(rings: Int, sectors: Int): Pair<FloatBuffer, Int> {
        val verts = mutableListOf<Float>()
        val r = 0.5f
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float) {
            verts.add(x); verts.add(y); verts.add(z)
            verts.add(nx); verts.add(ny); verts.add(nz)
            verts.add(u); verts.add(v)
        }
        for (i in 0 until rings) {
            val phi0 = (i.toDouble() / rings) * PI - PI / 2.0
            val phi1 = ((i + 1).toDouble() / rings) * PI - PI / 2.0
            val y0 = sin(phi0).toFloat(); val r0 = cos(phi0).toFloat()
            val y1 = sin(phi1).toFloat(); val r1 = cos(phi1).toFloat()
            val v0 = 1f - i.toFloat() / rings
            val v1 = 1f - (i + 1).toFloat() / rings

            for (j in 0 until sectors) {
                val th0 = (j.toDouble() / sectors) * 2.0 * PI
                val th1 = ((j + 1).toDouble() / sectors) * 2.0 * PI
                val c0 = cos(th0).toFloat(); val s0 = sin(th0).toFloat()
                val c1 = cos(th1).toFloat(); val s1 = sin(th1).toFloat()
                val u0 = j.toFloat() / sectors
                val u1 = (j + 1).toFloat() / sectors

                val x00 = c0 * r0; val z00 = s0 * r0
                val x10 = c1 * r0; val z10 = s1 * r0
                val x01 = c0 * r1; val z01 = s0 * r1
                val x11 = c1 * r1; val z11 = s1 * r1

                push(x00 * r, y0 * r, z00 * r, x00, y0, z00, u0, v0)
                push(x01 * r, y1 * r, z01 * r, x01, y1, z01, u0, v1)
                push(x11 * r, y1 * r, z11 * r, x11, y1, z11, u1, v1)

                push(x00 * r, y0 * r, z00 * r, x00, y0, z00, u0, v0)
                push(x11 * r, y1 * r, z11 * r, x11, y1, z11, u1, v1)
                push(x10 * r, y0 * r, z10 * r, x10, y0, z10, u1, v0)
            }
        }
        return toFloatBuffer(verts) to (verts.size / 8)
    }
}
