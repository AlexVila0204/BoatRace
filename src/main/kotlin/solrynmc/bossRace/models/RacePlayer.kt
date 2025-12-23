package solrynmc.bossRace.models

import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import java.util.UUID

data class RacePlayer (
    val uuid: UUID,
    val name: String,
    var boat: Boat? = null,
    var position: Int = 0,
    var checkpointsPassed: Int = 0,
    var currentLap: Int = 0,
    var finishTime: Long? = null,
    var startTime: Long = System.currentTimeMillis()
) {
    companion object {
        fun from(player: Player) : RacePlayer {
            return RacePlayer(
                uuid = player.uniqueId,
                name = player.name
            )
        }
    }

    fun isFinished() : Boolean {
        return finishTime != null
    }

    fun getElapsedTime() : Long {
        return (finishTime ?: System.currentTimeMillis()) - (startTime)
    }
}