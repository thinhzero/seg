package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "EMV_CARD", "NDEF_TAG", "RAW_TAG", "WRITTEN_PAYLOAD"
    val title: String,
    val subtitle: String,
    val tagIdHex: String,
    val techList: String,
    val payloadJson: String,
    val isFavorite: Boolean = false
)
