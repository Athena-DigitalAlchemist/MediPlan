package com.example.mediplan.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mediplan.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val historyDao: MedicationHistoryDao
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    private val encryptionKey = generateEncryptionKey()
    
    companion object {
        private const val CURRENT_VERSION = 1
        private const val BUFFER_SIZE = 8192 // 8KB buffer
    }

    suspend fun createBackup(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val backupData = BackupData(
                version = CURRENT_VERSION,
                medications = medicationDao.getAllMedications().first(),
                history = historyDao.getMedicationHistoryBetween(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now()
                ).first()
            )

            val backupJson = gson.toJson(backupData)
            val compressedData = compress(backupJson)
            val encryptedData = encrypt(compressedData)
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val filename = "mediplan_backup_v${CURRENT_VERSION}_$timestamp.enc"
            val file = File(context.getExternalFilesDir("backups"), filename)

            file.parentFile?.mkdirs()
            writeDataToFile(file, encryptedData)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            success(uri)
        } catch (e: Exception) {
            failure(BackupException("Σφάλμα κατά τη δημιουργία αντιγράφου", e))
        }
    }

    suspend fun restoreFromBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encryptedData = readDataFromUri(uri)
                ?: throw BackupException("Δεν ήταν δυνατή η ανάγνωση του αρχείου αντιγράφου")

            val decryptedData = decrypt(encryptedData)
            val decompressedJson = decompress(decryptedData)
            val backupData = gson.fromJson(decompressedJson, BackupData::class.java)

            // Έλεγχος συμβατότητας έκδοσης
            if (backupData.version > CURRENT_VERSION) {
                throw BackupException("Το αντίγραφο ασφαλείας δημιουργήθηκε από νεότερη έκδοση της εφαρμογής")
            }

            // Επαλήθευση δεδομένων
            validateBackupData(backupData)

            // Επαναφορά δεδομένων σε μία συναλλαγή
            AppDatabase.getDatabase(context).runInTransaction {
                try {
                    // Διαγραφή υπαρχόντων δεδομένων
                    medicationDao.deleteAll()
                    historyDao.deleteAll()

                    // Εισαγωγή δεδομένων από το αντίγραφο
                    backupData.medications.forEach { medication ->
                        medicationDao.insert(medication)
                    }
                    backupData.history.forEach { history ->
                        historyDao.insertMedicationHistory(history)
                    }
                } catch (e: Exception) {
                    throw BackupException("Σφάλμα κατά την επαναφορά των δεδομένων", e)
                }
            }

            success(Unit)
        } catch (e: Exception) {
            when (e) {
                is BackupException -> failure(e)
                else -> failure(BackupException("Απρόσμενο σφάλμα κατά την επαναφορά", e))
            }
        }
    }

    fun getBackupFiles(): List<BackupFile> {
        val backupDir = context.getExternalFilesDir("backups")
        return backupDir?.listFiles { file -> 
            file.name.startsWith("mediplan_backup_") && file.extension == "enc" 
        }?.map { file ->
            val version = extractVersionFromFilename(file.name)
            BackupFile(
                name = file.name,
                version = version,
                date = LocalDateTime.parse(
                    file.nameWithoutExtension.substringAfterLast("_"),
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                ),
                size = file.length(),
                uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            )
        }?.sortedByDescending { it.date } ?: emptyList()
    }

    private fun extractVersionFromFilename(filename: String): Int {
        return try {
            val versionPart = filename.substringAfter("_v").substringBefore("_")
            versionPart.toInt()
        } catch (e: Exception) {
            1 // Για συμβατότητα με παλαιότερα αντίγραφα
        }
    }

    fun deleteBackup(uri: Uri): Result<Boolean> {
        return try {
            val file = File(uri.path!!)
            success(file.delete())
        } catch (e: Exception) {
            failure(BackupException("Σφάλμα κατά τη διαγραφή του αντιγράφου", e))
        }
    }

    private fun validateBackupData(backupData: BackupData) {
        if (backupData.medications.isEmpty() && backupData.history.isEmpty()) {
            throw BackupException("Το αντίγραφο ασφαλείας είναι κενό")
        }
    }

    private fun generateEncryptionKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = context.packageName.toByteArray()
        return SecretKeySpec(digest.digest(bytes).copyOf(16), "AES")
    }

    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        return cipher.doFinal(data)
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
        return cipher.doFinal(data)
    }

    private fun compress(data: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            gzip.write(data.toByteArray())
        }
        return output.toByteArray()
    }

    private fun decompress(data: ByteArray): String {
        return GZIPInputStream(ByteArrayInputStream(data)).use { gzip ->
            gzip.bufferedReader().readText()
        }
    }

    private fun writeDataToFile(file: File, data: ByteArray) {
        FileOutputStream(file).use { fos ->
            var offset = 0
            while (offset < data.size) {
                val length = minOf(BUFFER_SIZE, data.size - offset)
                fos.write(data, offset, length)
                offset += length
            }
            fos.flush()
        }
    }

    private fun readDataFromUri(uri: Uri): ByteArray? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.toByteArray()
        }
    }

    private data class BackupData(
        val version: Int = CURRENT_VERSION,
        val medications: List<Medication>,
        val history: List<MedicationHistory>
    )
}

data class BackupFile(
    val name: String,
    val version: Int,
    val date: LocalDateTime,
    val size: Long,
    val uri: Uri
)

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) 