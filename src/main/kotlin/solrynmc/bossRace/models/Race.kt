package solrynmc.bossRace.models

import org.bukkit.entity.Player
import java.util.UUID

data class Race(
    val id: String,
    var state: RaceState = RaceState.WAITING,
    var players: MutableMap<UUID, RacePlayer> = mutableMapOf(),
    val spectators: MutableSet<UUID> = mutableSetOf(),
    val maxPlayers: Int = 6,
    val minPlayers: Int = 2,
    var totalLaps: Int = 3,
    var startTime: Long? = null,
    var endTime: Long? = null
) {
    fun addPlayer(player: Player): Boolean {
        if (state != RaceState.WAITING) return false
        if (players.size >= maxPlayers) return false
        if (players.containsKey(player.uniqueId)) return false

        players[player.uniqueId] = RacePlayer.from(player)
        return true
    }

    fun removePlayer(uuid: UUID): Boolean {
        return players.remove(uuid) != null
    }

    fun addSpectator(player: Player) {
        spectators.add(player.uniqueId)
    }

    fun removeSpectator(uuid: UUID) {
        spectators.remove(uuid)
    }

    fun canStart() : Boolean {
        return state == RaceState.WAITING && players.size >= minPlayers
    }

    fun getFinishedPlayers() : List<RacePlayer> {
        return players.values
            .filter { it.isFinished() }
            .sortedBy { it.finishTime }
    }

    fun getActivePlayers() : List<RacePlayer> {
        return players.values.filter { !it.isFinished() }
    }

    fun isPlayerInRace(uuid: UUID) : Boolean {
        return players.containsKey(uuid)
    }

    fun isSpectator(uuid: UUID) : Boolean {
        return spectators.contains(uuid)
    }
}