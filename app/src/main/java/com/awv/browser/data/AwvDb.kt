package com.awv.browser.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val time: Long
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val url: String,
    val title: String,
    val time: Long
)

@Dao
interface HistoryDao {
    @Insert suspend fun insert(entry: HistoryEntry)
    @Query("SELECT * FROM history ORDER BY time DESC LIMIT 500") fun all(): Flow<List<HistoryEntry>>
    @Query("DELETE FROM history WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM history") suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(bookmark: Bookmark)
    @Query("DELETE FROM bookmarks WHERE url = :url") suspend fun remove(url: String)
    @Query("SELECT * FROM bookmarks ORDER BY time DESC") fun all(): Flow<List<Bookmark>>
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)") fun isBookmarked(url: String): Flow<Boolean>
}

@Database(entities = [HistoryEntry::class, Bookmark::class], version = 1, exportSchema = false)
abstract class AwvDb : RoomDatabase() {
    abstract fun history(): HistoryDao
    abstract fun bookmarks(): BookmarkDao

    companion object {
        @Volatile private var instance: AwvDb? = null

        fun get(context: Context): AwvDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AwvDb::class.java, "awv.db")
                .build().also { instance = it }
        }
    }
}
