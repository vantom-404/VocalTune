package com.intervigil.micdroid.model

import java.io.File

data class SavedRecording(
    val file: File,
    val name: String,
    val durationSeconds: Int,
    val sizeBytes: Long,
    val timestamp: Long,
    val rawFile: File? = null,
    val isProcessed: Boolean = true
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val sizeFormatted: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }

    val hasRawVersion: Boolean
        get() = rawFile != null && rawFile.exists() && rawFile.length() > 44
}
