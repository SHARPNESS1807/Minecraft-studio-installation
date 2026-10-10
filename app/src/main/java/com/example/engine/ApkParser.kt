package com.example.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.example.model.AppItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object ApkParser {

    fun parseApkFile(context: Context, apkFile: File): AppItem? {
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
            val packageInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)

            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo
                // Trick to make PackageManager load resources from the archive file:
                appInfo?.sourceDir = apkFile.absolutePath
                appInfo?.publicSourceDir = apkFile.absolutePath

                val appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: apkFile.nameWithoutExtension
                val iconDrawable = appInfo?.let { pm.getApplicationIcon(it) }
                val iconBitmap = iconDrawable?.let { drawableToBitmap(it) }

                val launchActivity = packageInfo.activities?.firstOrNull()?.name

                val sizeMb = apkFile.length() / (1024.0 * 1024.0)
                val sizeFormatted = String.format("%.2f MB", sizeMb)

                val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                AppItem(
                    name = appLabel,
                    packageName = packageInfo.packageName ?: "com.unknown.app",
                    iconBitmap = iconBitmap,
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = versionCode,
                    sizeFormatted = sizeFormatted,
                    isImportedApk = true,
                    sourceApkPath = apkFile.absolutePath,
                    launchActivity = launchActivity,
                    permissions = permissions
                )
            } else {
                // Fallback for APK where manifest couldn't be parsed directly
                AppItem(
                    name = apkFile.nameWithoutExtension.replace("_", " ").capitalizeWords(),
                    packageName = "com.imported.${apkFile.nameWithoutExtension.lowercase().replace("[^a-z0-9]".toRegex(), "")}",
                    sizeFormatted = String.format("%.2f MB", apkFile.length() / (1024.0 * 1024.0)),
                    isImportedApk = true,
                    sourceApkPath = apkFile.absolutePath
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val tempDir = File(context.cacheDir, "imported_apks")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "imported_${System.currentTimeMillis()}.apk")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateSha256(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            val digest = md.digest()
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
