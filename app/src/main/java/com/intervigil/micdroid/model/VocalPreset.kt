package com.intervigil.micdroid.model

import android.content.Context
import com.intervigil.micdroid.dsp.MusicalKey
import com.intervigil.micdroid.dsp.ScaleType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Professional Vocal Preset configuring real DSP parameters:
 * Pitch correction, Noise Gate, Parametric EQ, Compressor, Reverb, Delay, and Gains.
 */
data class VocalPreset(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean = false,
    val settings: TuningSettings
) {
    companion object {
        val NATURAL_VOCAL = VocalPreset(
            id = "preset_natural",
            name = "Natural Vocal",
            description = "Subtle transparent correction, warm vocal body, light room acoustics",
            settings = TuningSettings(
                strength = 0.45f,
                retuneSpeed = 0.55f,
                noiseGateEnabled = true,
                noiseGateThresholdDb = -46f,
                eqLowDb = -1.0f,
                eqMidDb = 1.5f,
                eqHighDb = 2.0f,
                compressorEnabled = true,
                compressorThresholdDb = -18f,
                compressorRatio = 2.5f,
                compressorMakeupDb = 2.0f,
                reverbWet = 0.15f,
                reverbRoomSize = 0.5f,
                reverbDamping = 0.4f,
                delayWet = 0.05f,
                delayTimeMs = 180f,
                delayFeedback = 0.2f,
                inputGainDb = 0f,
                masterGainDb = 0f
            )
        )

        val HARD_AUTOTUNE = VocalPreset(
            id = "preset_hard_tune",
            name = "Hard Auto-Tune",
            description = "Instant pitch snapping, crisp vocal sheen, iconic modern hip-hop sound",
            settings = TuningSettings(
                strength = 1.0f,
                retuneSpeed = 0.0f, // Instant snap
                noiseGateEnabled = true,
                noiseGateThresholdDb = -42f,
                eqLowDb = -2.5f,
                eqMidDb = 0.0f,
                eqHighDb = 5.0f, // Bright modern presence
                compressorEnabled = true,
                compressorThresholdDb = -22f,
                compressorRatio = 5.0f,
                compressorMakeupDb = 4.0f,
                reverbWet = 0.22f,
                reverbRoomSize = 0.7f,
                reverbDamping = 0.2f,
                delayWet = 0.20f,
                delayTimeMs = 240f,
                delayFeedback = 0.40f,
                inputGainDb = 2.0f,
                masterGainDb = 0f
            )
        )

        val RAP_VOCAL = VocalPreset(
            id = "preset_rap",
            name = "Rap Vocal",
            description = "Punchy upfront dynamics, low-cut gate, fast pitch snapping, short slap delay",
            settings = TuningSettings(
                strength = 0.90f,
                retuneSpeed = 0.08f,
                noiseGateEnabled = true,
                noiseGateThresholdDb = -40f,
                eqLowDb = -3.0f,
                eqMidDb = 3.0f,
                eqHighDb = 4.0f,
                compressorEnabled = true,
                compressorThresholdDb = -24f,
                compressorRatio = 6.0f,
                compressorMakeupDb = 5.0f,
                reverbWet = 0.12f,
                reverbRoomSize = 0.4f,
                reverbDamping = 0.5f,
                delayWet = 0.16f,
                delayTimeMs = 120f, // Fast slap
                delayFeedback = 0.25f,
                inputGainDb = 3.0f,
                masterGainDb = 0f
            )
        )

        val POP_VOCAL = VocalPreset(
            id = "preset_pop",
            name = "Pop Vocal",
            description = "Sparkling highs, lush plate reverb, polished melodic correction",
            settings = TuningSettings(
                strength = 0.80f,
                retuneSpeed = 0.22f,
                noiseGateEnabled = true,
                noiseGateThresholdDb = -45f,
                eqLowDb = -1.5f,
                eqMidDb = 1.0f,
                eqHighDb = 6.0f, // Pop shimmer
                compressorEnabled = true,
                compressorThresholdDb = -20f,
                compressorRatio = 3.5f,
                compressorMakeupDb = 3.0f,
                reverbWet = 0.28f,
                reverbRoomSize = 0.8f,
                reverbDamping = 0.3f,
                delayWet = 0.18f,
                delayTimeMs = 300f,
                delayFeedback = 0.35f,
                inputGainDb = 1.5f,
                masterGainDb = 0f
            )
        )

        val SOFT_CORRECTION = VocalPreset(
            id = "preset_soft",
            name = "Soft Correction",
            description = "Gentle acoustic touch, relaxed retune speed, subtle enhancement",
            settings = TuningSettings(
                strength = 0.35f,
                retuneSpeed = 0.65f,
                noiseGateEnabled = false,
                noiseGateThresholdDb = -50f,
                eqLowDb = 0.0f,
                eqMidDb = 0.5f,
                eqHighDb = 1.5f,
                compressorEnabled = true,
                compressorThresholdDb = -14f,
                compressorRatio = 2.0f,
                compressorMakeupDb = 1.0f,
                reverbWet = 0.10f,
                reverbRoomSize = 0.5f,
                reverbDamping = 0.4f,
                delayWet = 0.0f,
                delayTimeMs = 200f,
                delayFeedback = 0.2f,
                inputGainDb = 0f,
                masterGainDb = 0f
            )
        )

        val ROBOTIC_VOICE = VocalPreset(
            id = "preset_robotic",
            name = "Robotic Voice",
            description = "Absolute pitch quantization, zero vibrato, synthetic metallic ambiance",
            settings = TuningSettings(
                strength = 1.0f,
                retuneSpeed = 0.0f,
                noiseGateEnabled = true,
                noiseGateThresholdDb = -38f,
                eqLowDb = -4.0f,
                eqMidDb = 4.5f, // Resonant mid peak
                eqHighDb = 3.0f,
                compressorEnabled = true,
                compressorThresholdDb = -28f,
                compressorRatio = 8.0f,
                compressorMakeupDb = 5.0f,
                reverbWet = 0.30f,
                reverbRoomSize = 0.9f,
                reverbDamping = 0.1f, // Metallic ring
                delayWet = 0.25f,
                delayTimeMs = 90f,
                delayFeedback = 0.50f,
                inputGainDb = 2.0f,
                masterGainDb = 0f
            )
        )

        val DEEP_VOICE = VocalPreset(
            id = "preset_deep",
            name = "Deep Voice",
            description = "Enhanced low-end warmth, thick compression, authoritative chest resonance",
            settings = TuningSettings(
                strength = 0.70f,
                retuneSpeed = 0.25f,
                noiseGateEnabled = true,
                noiseGateThresholdDb = -45f,
                eqLowDb = 6.0f, // Bass boost
                eqMidDb = -2.0f,
                eqHighDb = 0.0f,
                compressorEnabled = true,
                compressorThresholdDb = -22f,
                compressorRatio = 4.0f,
                compressorMakeupDb = 3.5f,
                reverbWet = 0.18f,
                reverbRoomSize = 0.6f,
                reverbDamping = 0.5f,
                delayWet = 0.10f,
                delayTimeMs = 200f,
                delayFeedback = 0.25f,
                inputGainDb = 1.0f,
                masterGainDb = 0f
            )
        )

        val BRIGHT_VOICE = VocalPreset(
            id = "preset_bright",
            name = "Bright Voice",
            description = "Crystal clear air frequencies, high-shelf saturation, sparkling presence",
            settings = TuningSettings(
                strength = 0.85f,
                retuneSpeed = 0.18f,
                noiseGateEnabled = true,
                noiseGateThresholdDb = -45f,
                eqLowDb = -2.0f,
                eqMidDb = 2.0f,
                eqHighDb = 7.0f, // Maximum air
                compressorEnabled = true,
                compressorThresholdDb = -20f,
                compressorRatio = 3.5f,
                compressorMakeupDb = 3.0f,
                reverbWet = 0.24f,
                reverbRoomSize = 0.75f,
                reverbDamping = 0.2f,
                delayWet = 0.15f,
                delayTimeMs = 250f,
                delayFeedback = 0.30f,
                inputGainDb = 1.0f,
                masterGainDb = 0f
            )
        )

        val BUILT_IN_PRESETS = listOf(
            NATURAL_VOCAL,
            HARD_AUTOTUNE,
            RAP_VOCAL,
            POP_VOCAL,
            SOFT_CORRECTION,
            ROBOTIC_VOICE,
            DEEP_VOICE,
            BRIGHT_VOICE
        )
    }
}

/**
 * Storage manager for custom user presets.
 */
object PresetManager {
    private const val PREFS_NAME = "vocaltune_presets"
    private const val KEY_CUSTOM_PRESETS = "custom_presets_json"

    fun loadCustomPresets(context: Context): List<VocalPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()

        val list = mutableListOf<VocalPreset>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val preset = fromJson(obj)
                list.add(preset)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomPreset(context: Context, name: String, settings: TuningSettings): VocalPreset {
        val existing = loadCustomPresets(context).toMutableList()
        val id = "custom_${System.currentTimeMillis()}"
        val newPreset = VocalPreset(
            id = id,
            name = name,
            description = "Custom User Preset",
            isCustom = true,
            settings = settings
        )
        existing.add(newPreset)
        saveCustomPresetsList(context, existing)
        return newPreset
    }

    fun deleteCustomPreset(context: Context, presetId: String) {
        val existing = loadCustomPresets(context).toMutableList()
        existing.removeAll { it.id == presetId }
        saveCustomPresetsList(context, existing)
    }

    private fun saveCustomPresetsList(context: Context, presets: List<VocalPreset>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (p in presets) {
            array.put(toJson(p))
        }
        prefs.edit().putString(KEY_CUSTOM_PRESETS, array.toString()).apply()
    }

    private fun toJson(p: VocalPreset): JSONObject {
        return JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("description", p.description)
            put("strength", p.settings.strength.toDouble())
            put("retuneSpeed", p.settings.retuneSpeed.toDouble())
            put("noiseGateEnabled", p.settings.noiseGateEnabled)
            put("noiseGateThresholdDb", p.settings.noiseGateThresholdDb.toDouble())
            put("eqLowDb", p.settings.eqLowDb.toDouble())
            put("eqMidDb", p.settings.eqMidDb.toDouble())
            put("eqHighDb", p.settings.eqHighDb.toDouble())
            put("compressorEnabled", p.settings.compressorEnabled)
            put("compressorThresholdDb", p.settings.compressorThresholdDb.toDouble())
            put("compressorRatio", p.settings.compressorRatio.toDouble())
            put("compressorMakeupDb", p.settings.compressorMakeupDb.toDouble())
            put("reverbWet", p.settings.reverbWet.toDouble())
            put("reverbRoomSize", p.settings.reverbRoomSize.toDouble())
            put("reverbDamping", p.settings.reverbDamping.toDouble())
            put("delayWet", p.settings.delayWet.toDouble())
            put("delayTimeMs", p.settings.delayTimeMs.toDouble())
            put("delayFeedback", p.settings.delayFeedback.toDouble())
            put("inputGainDb", p.settings.inputGainDb.toDouble())
            put("masterGainDb", p.settings.masterGainDb.toDouble())
        }
    }

    private fun fromJson(obj: JSONObject): VocalPreset {
        val s = TuningSettings(
            strength = obj.optDouble("strength", 0.85).toFloat(),
            retuneSpeed = obj.optDouble("retuneSpeed", 0.15).toFloat(),
            noiseGateEnabled = obj.optBoolean("noiseGateEnabled", true),
            noiseGateThresholdDb = obj.optDouble("noiseGateThresholdDb", -45.0).toFloat(),
            eqLowDb = obj.optDouble("eqLowDb", 0.0).toFloat(),
            eqMidDb = obj.optDouble("eqMidDb", 0.0).toFloat(),
            eqHighDb = obj.optDouble("eqHighDb", 0.0).toFloat(),
            compressorEnabled = obj.optBoolean("compressorEnabled", true),
            compressorThresholdDb = obj.optDouble("compressorThresholdDb", -20.0).toFloat(),
            compressorRatio = obj.optDouble("compressorRatio", 3.0).toFloat(),
            compressorMakeupDb = obj.optDouble("compressorMakeupDb", 2.0).toFloat(),
            reverbWet = obj.optDouble("reverbWet", 0.20).toFloat(),
            reverbRoomSize = obj.optDouble("reverbRoomSize", 0.70).toFloat(),
            reverbDamping = obj.optDouble("reverbDamping", 0.25).toFloat(),
            delayWet = obj.optDouble("delayWet", 0.15).toFloat(),
            delayTimeMs = obj.optDouble("delayTimeMs", 220.0).toFloat(),
            delayFeedback = obj.optDouble("delayFeedback", 0.35).toFloat(),
            inputGainDb = obj.optDouble("inputGainDb", 0.0).toFloat(),
            masterGainDb = obj.optDouble("masterGainDb", 0.0).toFloat()
        )
        return VocalPreset(
            id = obj.getString("id"),
            name = obj.getString("name"),
            description = obj.optString("description", "Custom Preset"),
            isCustom = true,
            settings = s
        )
    }
}
