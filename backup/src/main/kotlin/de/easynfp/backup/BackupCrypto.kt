package de.easynfp.backup

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable

/** Encrypted backup file. All binary values are Base64 encoded. */
@Serializable
public data class EncryptedEnvelope(
    val app: String = BackupFormat.APP_ID,
    val encrypted: Boolean = true,
    val envelopeVersion: Int = 1,
    val kdf: String = BackupCrypto.KDF,
    val iterations: Int,
    val salt: String,
    val cipher: String = BackupCrypto.CIPHER,
    val iv: String,
    val ciphertext: String,
)

/** Password based encryption with AES-256-GCM; GCM also detects wrong passwords and tampering. */
internal object BackupCrypto {
    const val KDF = "PBKDF2WithHmacSHA256"
    const val CIPHER = "AES/GCM/NoPadding"
    const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private val random = SecureRandom()

    fun encrypt(plain: ByteArray, password: CharArray, iterations: Int = ITERATIONS): EncryptedEnvelope {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        val encoder = Base64.getEncoder()
        return EncryptedEnvelope(
            iterations = iterations,
            salt = encoder.encodeToString(salt),
            iv = encoder.encodeToString(iv),
            ciphertext = encoder.encodeToString(cipher.doFinal(plain)),
        )
    }

    fun decrypt(envelope: EncryptedEnvelope, password: CharArray): ByteArray {
        if (envelope.kdf != KDF || envelope.cipher != CIPHER) throw BackupException(BackupError.UNSUPPORTED_VERSION)
        val decoder = Base64.getDecoder()
        val (salt, iv, data) = try {
            Triple(decoder.decode(envelope.salt), decoder.decode(envelope.iv), decoder.decode(envelope.ciphertext))
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupError.CORRUPT, e)
        }
        return try {
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(
                Cipher.DECRYPT_MODE,
                deriveKey(password, salt, envelope.iterations),
                GCMParameterSpec(TAG_BITS, iv),
            )
            cipher.doFinal(data)
        } catch (e: GeneralSecurityException) {
            throw BackupException(BackupError.WRONG_PASSWORD, e)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val key = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            return SecretKeySpec(key, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
