package com.safespend.app.service

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CardSecret(
    val cardholder: String,
    val number: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String
)

/** AES-GCM card vault; the non-exportable Android Keystore key requires biometric auth per use. */
class SecureCardVault {
    fun newEncryptCipher(): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    }

    fun newDecryptCipher(iv: ByteArray): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
    }

    fun encrypt(cipher: Cipher, secret: CardSecret): Pair<ByteArray, ByteArray> {
        val json = JSONObject()
            .put("cardholder", secret.cardholder)
            .put("number", secret.number)
            .put("expiryMonth", secret.expiryMonth)
            .put("expiryYear", secret.expiryYear)
            .put("cvv", secret.cvv)
            .toString()
        return cipher.doFinal(json.toByteArray(StandardCharsets.UTF_8)) to cipher.iv
    }

    fun decrypt(cipher: Cipher, encrypted: ByteArray): CardSecret {
        val json = JSONObject(String(cipher.doFinal(encrypted), StandardCharsets.UTF_8))
        return CardSecret(
            cardholder = json.getString("cardholder"),
            number = json.getString("number"),
            expiryMonth = json.getString("expiryMonth"),
            expiryYear = json.getString("expiryYear"),
            cvv = json.getString("cvv")
        )
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        try {
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        } catch (_: UnrecoverableKeyException) {
            // Biometric enrollment changed: old ciphertext cannot be recovered, but allow new cards.
            keyStore.deleteEntry(KEY_ALIAS)
        }

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(builder.build())
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "safespend_card_vault_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
