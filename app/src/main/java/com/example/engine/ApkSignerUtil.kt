package com.example.engine

import android.content.Context
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Collections

data class SigningResult(
    val isSuccess: Boolean,
    val message: String,
    val signedApkFile: File? = null,
    val v1Verified: Boolean = false,
    val v2Verified: Boolean = false,
    val v3Verified: Boolean = false,
    val certSubject: String = "",
    val sha256Fingerprint: String = ""
)

object ApkSignerUtil {

    suspend fun signApk(
        context: Context,
        inputApk: File,
        outputApk: File,
        keystorePassword: String = "android",
        keyAlias: String = "androiddebugkey"
    ): SigningResult = withContext(Dispatchers.IO) {
        try {
            if (!inputApk.exists()) {
                return@withContext SigningResult(false, "Source APK file not found at ${inputApk.path}")
            }

            // Ensure parent directory of output exists
            outputApk.parentFile?.mkdirs()

            // Prepare keystore from assets
            val keystoreFile = File(context.filesDir, "aura_signing.keystore")
            if (!keystoreFile.exists() || keystoreFile.length() == 0L) {
                context.assets.open("aura_signing.keystore").use { input ->
                    FileOutputStream(keystoreFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val keyStore = KeyStore.getInstance("JKS")
            FileInputStream(keystoreFile).use { fis ->
                keyStore.load(fis, keystorePassword.toCharArray())
            }

            val privateKey = keyStore.getKey(keyAlias, keystorePassword.toCharArray()) as? PrivateKey
                ?: return@withContext SigningResult(false, "Could not locate private key for alias: $keyAlias")

            val cert = keyStore.getCertificate(keyAlias) as? X509Certificate
                ?: return@withContext SigningResult(false, "Could not locate certificate for alias: $keyAlias")

            // Setup official Google Android ApkSigner
            val signerConfig = ApkSigner.SignerConfig.Builder(
                keyAlias,
                privateKey,
                Collections.singletonList(cert)
            ).build()

            val tempOutput = File(context.cacheDir, "temp_signing_${System.currentTimeMillis()}.apk")

            val apkSigner = ApkSigner.Builder(Collections.singletonList(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(tempOutput)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()

            apkSigner.sign()

            // Move temp to destination
            if (outputApk.exists()) outputApk.delete()
            tempOutput.copyTo(outputApk, overwrite = true)
            tempOutput.delete()

            // Verify with ApkVerifier
            var v1Ok = false
            var v2Ok = false
            var v3Ok = false
            try {
                val verifier = ApkVerifier.Builder(outputApk).build()
                val result = verifier.verify()
                v1Ok = result.isVerifiedUsingV1Scheme
                v2Ok = result.isVerifiedUsingV2Scheme
                v3Ok = result.isVerifiedUsingV3Scheme
            } catch (ignored: Exception) {}

            val fingerprint = calculateCertFingerprint(cert)

            SigningResult(
                isSuccess = true,
                message = "APK successfully signed with V1 (JAR) and V2 (APK Block) signature schemes!",
                signedApkFile = outputApk,
                v1Verified = v1Ok,
                v2Verified = v2Ok,
                v3Verified = v3Ok,
                certSubject = cert.subjectX500Principal.name,
                sha256Fingerprint = fingerprint
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SigningResult(
                isSuccess = false,
                message = "Signing failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun calculateCertFingerprint(cert: X509Certificate): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert.encoded)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "N/A"
        }
    }
}
