package com.intervigil.micdroid.dsp

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

enum class MusicalKey(val displayName: String, val semitoneOffset: Int) {
    C("C", 0),
    C_SHARP("C#", 1),
    D("D", 2),
    D_SHARP("D#", 3),
    E("E", 4),
    F("F", 5),
    F_SHARP("F#", 6),
    G("G", 7),
    G_SHARP("G#", 8),
    A("A", 9),
    A_SHARP("A#", 10),
    B("B", 11)
}

enum class ScaleType(val displayName: String, val intervals: List<Int>) {
    MAJOR("Major", listOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR("Minor (Natural)", listOf(0, 2, 3, 5, 7, 8, 10)),
    PENTATONIC_MAJOR("Pentatonic Major", listOf(0, 2, 4, 7, 9)),
    PENTATONIC_MINOR("Pentatonic Minor", listOf(0, 3, 5, 7, 10)),
    CHROMATIC("Chromatic", (0..11).toList())
}

data class TargetNote(
    val noteName: String,
    val octave: Int,
    val targetFrequency: Float,
    val centsOffset: Float, // -50 to +50
    val midiNote: Int
)

object MusicalScale {
    private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun findTargetNote(
        frequencyHz: Float,
        key: MusicalKey,
        scale: ScaleType
    ): TargetNote {
        if (frequencyHz < 50.0f || frequencyHz > 1500.0f) {
            return TargetNote(
                noteName = "--",
                octave = 0,
                targetFrequency = 0f,
                centsOffset = 0f,
                midiNote = 0
            )
        }

        // Convert Hz to continuous MIDI note number (A4 = 440 Hz = MIDI 69)
        val continuousMidi = 69.0 + 12.0 * log2(frequencyHz / 440.0)

        // Allowed pitch classes (0 = C, 1 = C#, ...) for current key and scale
        val allowedPitchClasses = scale.intervals.map { interval ->
            (key.semitoneOffset + interval) % 12
        }.toSet()

        // Find nearest allowed MIDI note
        val roundedMidi = continuousMidi.roundToInt()
        var bestMidi = roundedMidi
        var minDistance = Double.MAX_VALUE

        // Search within +/- 6 semitones
        for (candidate in (roundedMidi - 6)..(roundedMidi + 6)) {
            val pitchClass = ((candidate % 12) + 12) % 12
            if (allowedPitchClasses.contains(pitchClass)) {
                val dist = kotlin.math.abs(continuousMidi - candidate)
                if (dist < minDistance) {
                    minDistance = dist
                    bestMidi = candidate
                }
            }
        }

        val targetFreq = (440.0 * 2.0.pow((bestMidi - 69).toDouble() / 12.0)).toFloat()
        val cents = (100.0 * (continuousMidi - bestMidi)).toFloat().coerceIn(-50f, 50f)

        val pitchClass = ((bestMidi % 12) + 12) % 12
        val octave = (bestMidi / 12) - 1
        val noteName = NOTE_NAMES[pitchClass]

        return TargetNote(
            noteName = noteName,
            octave = octave,
            targetFrequency = targetFreq,
            centsOffset = cents,
            midiNote = bestMidi
        )
    }

    fun midiToFrequency(midiNote: Int): Float {
        return (440.0 * 2.0.pow((midiNote - 69).toDouble() / 12.0)).toFloat()
    }
}
