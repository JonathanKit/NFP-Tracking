package de.easynfp.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Why a file could not be imported. */
public enum class BackupError {
    /** Not an easyNFP backup at all. */
    NOT_A_BACKUP,

    /** The file is encrypted and no password was given. */
    PASSWORD_REQUIRED,

    /** Decryption failed: wrong password or modified file. */
    WRONG_PASSWORD,

    /** Written by a newer app version with an unknown format version. */
    UNSUPPORTED_VERSION,

    /** Structurally broken content. */
    CORRUPT,
}

public class BackupException(public val error: BackupError, cause: Throwable? = null) :
    Exception("Backup import failed: $error", cause)

/**
 * Reading and writing backup files.
 *
 * Plain backups are UTF-8 JSON ([BackupData]). Encrypted backups wrap the same JSON in an
 * [EncryptedEnvelope] (AES-256-GCM, key derived with PBKDF2).
 */
public object BackupFormat {
    public const val APP_ID: String = "easyNFP"

    /** Increase together with a migration in [BackupMigrations]. */
    public const val CURRENT_VERSION: Int = 1

    public const val FILE_EXTENSION: String = "easynfp"

    /** Generic type so that document providers do not append ".json" to the file name. */
    public const val MIME_TYPE: String = "application/octet-stream"

    internal val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    /** Serializes [data]; encrypts if a non-empty [password] is given. */
    public fun encode(data: BackupData, password: CharArray? = null): ByteArray {
        val plain = json.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8)
        if (password == null || password.isEmpty()) return plain
        val envelope = BackupCrypto.encrypt(plain, password)
        return json.encodeToString(EncryptedEnvelope.serializer(), envelope).toByteArray(Charsets.UTF_8)
    }

    /** True if [bytes] is an encrypted backup that needs a password. */
    public fun isEncrypted(bytes: ByteArray): Boolean = runCatching {
        val root = json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        root.isEnvelope()
    }.getOrDefault(false)

    /** Reads a backup, decrypting and migrating it to the current format version. */
    public fun decode(bytes: ByteArray, password: CharArray? = null): BackupData {
        val root = parseObject(bytes)
        val content = if (root.isEnvelope()) decrypt(root, password) else root

        if (content["app"]?.jsonPrimitive?.content != APP_ID) throw BackupException(BackupError.NOT_A_BACKUP)
        val version = (content["formatVersion"] as? JsonPrimitive)?.intOrNull
        when {
            version == null || version < 1 -> throw BackupException(BackupError.CORRUPT)
            version > CURRENT_VERSION -> throw BackupException(BackupError.UNSUPPORTED_VERSION)
        }
        val migrated = BackupMigrations.migrate(content, requireNotNull(version))
        return decodeOrCorrupt { json.decodeFromJsonElement<BackupData>(migrated) }
    }

    private fun decrypt(root: JsonObject, password: CharArray?): JsonObject {
        if (password == null || password.isEmpty()) throw BackupException(BackupError.PASSWORD_REQUIRED)
        val envelope = decodeOrCorrupt { json.decodeFromJsonElement<EncryptedEnvelope>(root) }
        return parseObject(BackupCrypto.decrypt(envelope, password))
    }

    private inline fun <T> decodeOrCorrupt(block: () -> T): T = try {
        block()
    } catch (e: SerializationException) {
        throw BackupException(BackupError.CORRUPT, e)
    } catch (e: IllegalArgumentException) {
        throw BackupException(BackupError.CORRUPT, e)
    }

    private fun parseObject(bytes: ByteArray): JsonObject = try {
        json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
    } catch (e: SerializationException) {
        throw BackupException(BackupError.NOT_A_BACKUP, e)
    } catch (e: IllegalArgumentException) {
        throw BackupException(BackupError.NOT_A_BACKUP, e)
    }

    private fun JsonObject.isEnvelope(): Boolean =
        (this["encrypted"] as? JsonPrimitive)?.booleanOrNull == true && this["app"]?.jsonPrimitive?.content == APP_ID
}

/** Upgrades the JSON of older backup format versions step by step to [BackupFormat.CURRENT_VERSION]. */
internal object BackupMigrations {
    /** Migration from version `key` to `key + 1`. Add an entry for every format change. */
    private val steps: Map<Int, (JsonObject) -> JsonObject> = emptyMap()

    fun migrate(content: JsonObject, fromVersion: Int): JsonObject {
        var current = content
        for (version in fromVersion until BackupFormat.CURRENT_VERSION) {
            val step = steps[version] ?: error("Missing backup migration from version $version")
            current = step(current)
        }
        return JsonObject(current + ("formatVersion" to JsonPrimitive(BackupFormat.CURRENT_VERSION)))
    }
}
