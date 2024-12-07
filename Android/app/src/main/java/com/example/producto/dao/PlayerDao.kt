package com.example.producto2.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.producto2.model.Player

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players")
    fun getAllPlayers(): List<Player>

    @Query("SELECT * FROM players WHERE name = :name LIMIT 1")
    fun findPlayerByName(name: String): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlayer(player: Player)

    @Update
    fun updatePlayer(player: Player)

    @Delete
    fun deletePlayer(player: Player)

}
