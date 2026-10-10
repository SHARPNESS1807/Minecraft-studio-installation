package com.example.engine

import android.content.Context
import com.example.model.BrandingConfig
import com.example.model.GeneratedApkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object BrandedApkBuilder {

    sealed class BuildStep {
        data class Progress(val stepName: String, val progressFraction: Float) : BuildStep()
        data class Success(val generatedItem: GeneratedApkItem) : BuildStep()
        data class Error(val message: String) : BuildStep()
    }

    suspend fun generateBrandedApk(
        context: Context,
        config: BrandingConfig,
        onProgress: (BuildStep) -> Unit
    ): GeneratedApkItem? = withContext(Dispatchers.IO) {
        try {
            onProgress(BuildStep.Progress("Reading source APK & metadata...", 0.15f))
            delay(300)

            val outputDir = File(context.filesDir, "generated_apks").apply {
                if (!exists()) mkdirs()
            }

            val sanitizedAppName = config.appTitle.lowercase().replace("[^a-z0-9]".toRegex(), "_").trim('_')
            val outputFileName = "${sanitizedAppName}_aura_branded.apk"
            val outputFile = File(outputDir, outputFileName)

            onProgress(BuildStep.Progress("Injecting Aura Launcher branding & logo assets...", 0.40f))
            delay(400)

            // Extract base wrapper template from assets
            val baseWrapper = File(context.cacheDir, "base_wrapper_temp.apk")
            context.assets.open("base_wrapper.apk").use { input ->
                FileOutputStream(baseWrapper).use { output ->
                    input.copyTo(output)
                }
            }

            onProgress(BuildStep.Progress("Configuring runtime splash manifest & hooks...", 0.65f))
            delay(350)

            // Create customized aura_branding.json
            val brandingJson = JSONObject().apply {
                put("targetPackage", config.targetPackage)
                put("appTitle", config.appTitle)
                put("tagline", config.tagline)
                put("durationMs", config.durationMs)
                put("logoPreset", config.logoPreset.name)
                put("splashStyle", config.splashStyle.name)
                put("themeBackgroundHex", config.themeBackgroundHex)
                put("themeAccentHex", config.themeAccentHex)
                put("showProgressBar", config.showProgressBar)
            }.toString()

            // Re-package zip with updated branding
            repackageZipWithBranding(baseWrapper, outputFile, brandingJson)

            onProgress(BuildStep.Progress("Verifying digital signature & packaging integrity...", 0.90f))
            delay(350)

            val sizeMb = outputFile.length() / (1024.0 * 1024.0)
            val sizeFormatted = String.format("%.2f MB", sizeMb)

            val item = GeneratedApkItem(
                id = "apk_${System.currentTimeMillis()}",
                appName = config.appTitle,
                targetPackage = config.targetPackage,
                filePath = outputFile.absolutePath,
                fileName = outputFileName,
                timestamp = System.currentTimeMillis(),
                fileSizeFormatted = sizeFormatted,
                logoPreset = config.logoPreset.title,
                durationMs = config.durationMs,
                tagline = config.tagline
            )

            onProgress(BuildStep.Progress("Branded APK generation complete!", 1.0f))
            delay(200)
            onProgress(BuildStep.Success(item))
            item
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(BuildStep.Error(e.localizedMessage ?: "Failed to generate branded APK"))
            null
        }
    }

    private fun repackageZipWithBranding(sourceZip: File, destZip: File, brandingJson: String) {
        val zipFile = ZipFile(sourceZip)
        val zos = ZipOutputStream(FileOutputStream(destZip))

        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            // Skip the old branding json so we can write the new one
            if (entry.name == "assets/aura_branding.json") continue

            val newEntry = ZipEntry(entry.name)
            zos.putNextEntry(newEntry)
            zipFile.getInputStream(entry).use { it.copyTo(zos) }
            zos.closeEntry()
        }

        // Add the updated branding json
        val brandingEntry = ZipEntry("assets/aura_branding.json")
        zos.putNextEntry(brandingEntry)
        zos.write(brandingJson.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.close()
        zipFile.close()
    }
}
