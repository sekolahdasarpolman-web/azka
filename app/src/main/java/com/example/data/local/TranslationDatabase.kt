package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ParagraphTranslationResult
import com.example.data.remote.GeminiRetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString

@Entity(tableName = "saved_translations")
data class SavedTranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceText: String,
    val standardEnglishText: String,
    val caraMembacaFull: String,
    val ipaFull: String,
    val styleBadge: String,
    val fullResultJson: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SavedTranslationDao {
    @Query("SELECT * FROM saved_translations ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllSavedTranslations(): Flow<List<SavedTranslationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(item: SavedTranslationEntity)

    @Query("UPDATE saved_translations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM saved_translations WHERE id = :id")
    suspend fun deleteTranslationById(id: Int)
}

@Database(entities = [SavedTranslationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedTranslationDao(): SavedTranslationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tutur_inggris_db"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TranslationRepository(private val dao: SavedTranslationDao) {
    val allSavedTranslations: Flow<List<SavedTranslationEntity>> = dao.getAllSavedTranslations()

    suspend fun saveResult(result: ParagraphTranslationResult) {
        val jsonStr = GeminiRetrofitClient.json.encodeToString(result)
        val entity = SavedTranslationEntity(
            sourceText = result.sourceText,
            standardEnglishText = result.standardEnglishText,
            caraMembacaFull = result.caraMembacaFull,
            ipaFull = result.ipaFull,
            styleBadge = result.styleBadge,
            fullResultJson = jsonStr,
            isFavorite = false,
            timestamp = System.currentTimeMillis()
        )
        dao.insertTranslation(entity)
    }

    suspend fun toggleFavorite(id: Int, currentFavorite: Boolean) {
        dao.updateFavorite(id, !currentFavorite)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteTranslationById(id)
    }

    fun decodeFullResult(entity: SavedTranslationEntity): ParagraphTranslationResult? {
        return try {
            GeminiRetrofitClient.json.decodeFromString<ParagraphTranslationResult>(entity.fullResultJson)
        } catch (_: Exception) {
            null
        }
    }
}
