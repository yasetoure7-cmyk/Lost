package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.WeaponType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class TacticalSoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    @Volatile
    var enabled: Boolean = true

    fun playGunshot(weapon: WeaponType) {
        if (!enabled) return
        val durationMs = when (weapon) {
            WeaponType.ASSAULT_RIFLE -> 115
            WeaponType.TACTICAL_SHOTGUN -> 210
            WeaponType.MARKSMAN_SNIPER -> 260
            WeaponType.COMBAT_PISTOL -> 100
        }
        val punchFreq = when (weapon) {
            WeaponType.ASSAULT_RIFLE -> 145f
            WeaponType.TACTICAL_SHOTGUN -> 85f
            WeaponType.MARKSMAN_SNIPER -> 110f
            WeaponType.COMBAT_PISTOL -> 185f
        }
        playSynthesized(durationMs) { t, norm ->
            val env = exp(-norm * 8.5f)
            val crack = sin(2.0 * PI * punchFreq * (1.0 - norm * 0.5) * t).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-norm * 6.0f)
            (crack * 0.55f + noise * 0.65f) * env * 0.45f
        }
    }

    fun playHitMarker(headshot: Boolean = false) {
        if (!enabled) return
        val freq = if (headshot) 1680f else 1120f
        playSynthesized(55) { t, norm ->
            val env = exp(-norm * 10f)
            sin(2.0 * PI * freq * t).toFloat() * env * 0.3f
        }
    }

    fun playReload() {
        if (!enabled) return
        playSynthesized(160) { t, norm ->
            val click1 = if (norm < 0.25f) sin(2.0 * PI * 680.0 * t).toFloat() * exp(-norm * 20f) else 0f
            val click2 = if (norm > 0.65f) sin(2.0 * PI * 940.0 * t).toFloat() * exp(-(norm - 0.65f) * 20f) else 0f
            (click1 + click2) * 0.35f
        }
    }

    fun playExplosion() {
        if (!enabled) return
        playSynthesized(460) { t, norm ->
            val env = exp(-norm * 4.2f)
            val rumble = sin(2.0 * PI * (62.0 - norm * 30.0) * t).toFloat()
            val blast = (Random.nextFloat() * 2f - 1f)
            (rumble * 0.6f + blast * 0.55f) * env * 0.55f
        }
    }

    fun playZombieGrowl() {
        if (!enabled) return
        playSynthesized(240) { t, norm ->
            val env = sin(norm * PI).toFloat()
            val mod = sin(2.0 * PI * 18.0 * t).toFloat()
            val throat = sin(2.0 * PI * (92.0 + mod * 16.0) * t).toFloat()
            val rasp = (Random.nextFloat() * 2f - 1f) * 0.22f
            (throat * 0.65f + rasp) * env * 0.25f
        }
    }

    fun playSuccessChime() {
        if (!enabled) return
        playSynthesized(320) { t, norm ->
            val freq = if (norm < 0.45f) 523.25 else 783.99
            val env = exp(-norm * 4.5f)
            sin(2.0 * PI * freq * t).toFloat() * env * 0.32f
        }
    }

    fun playMedStim() {
        if (!enabled) return
        playSynthesized(220) { t, norm ->
            val env = (1f - norm)
            val hiss = (Random.nextFloat() * 2f - 1f) * 0.25f
            val tone = sin(2.0 * PI * (440.0 + norm * 320.0) * t).toFloat() * 0.25f
            (hiss + tone) * env * 0.4f
        }
    }

    private fun playSynthesized(durationMs: Int, generator: (Double, Float) -> Float) {
        scope.launch {
            try {
                val count = (sampleRate * durationMs) / 1000
                if (count <= 0) return@launch
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val norm = i.toFloat() / count.toFloat()
                    val sample = generator(t, norm).coerceIn(-1f, 1f)
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 40L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore audio device errors on headless/test environments
            }
        }
    }
}
