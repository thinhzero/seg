package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM scanned_tags ORDER BY timestamp DESC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM scanned_tags WHERE category = :category ORDER BY timestamp DESC")
    fun getTagsByCategoryFlow(category: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM scanned_tags WHERE title LIKE '%' || :query || '%' OR subtitle LIKE '%' || :query || '%' OR tagIdHex LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTagsFlow(query: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM scanned_tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)

    @Query("DELETE FROM scanned_tags")
    suspend fun clearAll()
}
