package solrynmc.bossRace.listeners

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import solrynmc.bossRace.managers.LocationManager
import solrynmc.bossRace.managers.RaceManager
import java.util.UUID

class BoatListener(
    private val raceManager: RaceManager,
    private val locationManager: LocationManager
) : Listener {

    private val previousLocations = mutableMapOf<UUID, Location>()

    @EventHandler
    fun onPlayerExitBoat(event: VehicleExitEvent) {
        val player = event.exited as? Player ?: return
        event.vehicle as? Boat ?: return

        val race = raceManager.getCurrentRace() ?: return
        if (race.players.containsKey(player.uniqueId)) {
            event.isCancelled = true
            player.sendMessage("§cYou cannot exit the boat during the race!")
        }
    }


    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!raceManager.isPlayerRacing(player)) {
            previousLocations.remove(player.uniqueId)
            return
        }

        val currentLoc = player.location
        val prevLoc = previousLocations[player.uniqueId]

        previousLocations[player.uniqueId] = currentLoc.clone()

        if (prevLoc == null) return
        
        val racePlayer = raceManager.getRacePlayer(player) ?: return
        val checkpoints = locationManager.getCheckpoints()
        val totalLaps = raceManager.getTotalLaps()

        checkpoints.forEachIndexed { index, (pos1, pos2) ->
            val checkpointIndex = index % checkpoints.size
            val expectedCheckpoint = racePlayer.checkpointsPassed % checkpoints.size
            
            if (checkpointIndex == expectedCheckpoint && racePlayer.checkpointsPassed < (racePlayer.currentLap + 1) * checkpoints.size) {
                if (locationManager.isPlayerCrossingLine(currentLoc, prevLoc, pos1, pos2)) {
                    racePlayer.checkpointsPassed++
                    val checkpointInLap = (racePlayer.checkpointsPassed - 1) % checkpoints.size + 1
                    player.sendMessage("§aCheckpoint $checkpointInLap/${checkpoints.size}")
                }
            }
        }

        val finishLine = locationManager.getFinishLine()
        if (finishLine != null) {
            if (locationManager.isPlayerCrossingLine(currentLoc, prevLoc, finishLine.first, finishLine.second)) {
                if (checkpoints.isEmpty()) {
                    player.sendMessage("§c§lERROR: §cNo checkpoints configured! Ask admin to add checkpoints.")
                    return
                }

                val requiredCheckpoints = (racePlayer.currentLap + 1) * checkpoints.size

                if (racePlayer.checkpointsPassed >= requiredCheckpoints) {
                    racePlayer.currentLap++

                    if (racePlayer.currentLap >= totalLaps) {
                        raceManager.finishPlayer(player)
                        previousLocations.remove(player.uniqueId)
                    } else {
                        player.sendTitle("§6§lLAP ${racePlayer.currentLap}/$totalLaps", "§aComplete!", 5, 30, 10)
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)
                    }
                } else {
                    val passedInLap = racePlayer.checkpointsPassed % checkpoints.size
                    player.sendMessage("§cPass all checkpoints first! ($passedInLap/${checkpoints.size})")
                }
            }
        }
    }
}
