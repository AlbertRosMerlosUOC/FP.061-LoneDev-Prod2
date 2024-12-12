package com.example.producto2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerId: Int,
    val loot: Int,
    val result1: String,
    val result2: String,
    val result3: String,
    val date: String,
    val location: String?
)
