package com.example.model

import android.graphics.Bitmap

data class AppItem(
    val name: String,
    val packageName: String,
    val iconBitmap: Bitmap? = null,
    val iconResId: Int? = null,
    val versionName: String = "1.0",
    val versionCode: Long = 1,
    val sizeFormatted: String = "",
    val isImportedApk: Boolean = false,
    val sourceApkPath: String? = null,
    val launchActivity: String? = null,
    val permissions: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val installDate: Long = System.currentTimeMillis()
)
