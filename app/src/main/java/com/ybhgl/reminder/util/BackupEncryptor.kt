package com.ybhgl.reminder.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object BackupEncryptor {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"
    
    // 生成 AES 256 位密钥
    private val secretKey: SecretKeySpec by lazy {
        // 使用异或混淆字节数组，避免敏感的明文 Seed 直接存在于静态常量池中
        val obfuscated = byteArrayOf(
            40, 63, 55, 51, 52, 62, 63, 40, 41, 63, 57, 47, 40, 63, 124, 59, 57, 49, 47, 34, 41, 63, 63, 62, 44, 59, 54, 47, 63, 37, 104, 106, 104, 110, 37, 35, 56, 62, 61, 54
        )
        val seedBytes = ByteArray(obfuscated.size)
        for (i in obfuscated.indices) {
            seedBytes[i] = (obfuscated[i].toInt() xor 90).toByte()
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(seedBytes)
        SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * 加密文本。
     * 返回的格式是：Base64(IV) + ":" + Base64(密文)
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            
            "$ivBase64:$encryptedBase64"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText // 如果加密失败，保留明文
        }
    }

    /**
     * 解密文本。
     * 如果解密成功，返回解密后的明文。
     * 如果解密失败（格式不对，或密钥不对，说明原数据可能是明文），返回 null。
     */
    fun decrypt(encryptedText: String): String? {
        if (!encryptedText.contains(":")) {
            return null
        }
        return try {
            val parts = encryptedText.split(":")
            if (parts.size != 2) return null
            
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // 解密失败（大概率是旧版明文格式）
            null
        }
    }
}
