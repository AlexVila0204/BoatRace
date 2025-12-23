package solrynmc.bossRace.managers

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Boat
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import solrynmc.bossRace.BossRace
import solrynmc.bossRace.models.Race
import solrynmc.bossRace.models.RacePlayer
import solrynmc.bossRace.models.RaceState

class RaceManager(
    private val plugin: BossRace,
    private val locationManager: LocationManager
) {
    private var currentRace: Race? = null
    private var countdownTask: BukkitTask? = null
    private var freezeTask: BukkitTask? = null
    private var lobbyCountdownTask: BukkitTask? = null
    private var lobbyTimeLeft: Int = 0
    private val barrierBlocks = mutableMapOf<Location, BlockData>()
    private var finishTimerTask: BukkitTask? = null
    private var finishTimeLeft: Int = 0

    fun createRace(maxPlayers: Int = 6, totalLaps: Int = 3): Race {
        if (currentRace != null) {
            throw IllegalStateException("There is already a race running!")
        }

        val race = Race(
            id = "race_${System.currentTimeMillis()}",
            maxPlayers = maxPlayers,
            totalLaps = totalLaps
        )
        currentRace = race
        return race
    }

    fun getTotalLaps(): Int {
        return currentRace?.totalLaps ?: 3
    }

    fun getCurrentRace() : Race? = currentRace

    fun addPlayer(player: Player): Boolean {
        val race = currentRace ?: return false
        if (race.state != RaceState.WAITING){
            race.addSpectator(player)
            player.sendMessage("§eThe race has already started! You've been added as a spectator.")
            return false
        }

        if (race.addPlayer(player)) {
            broadcastToRace("§a${player.name} joined the race! (${race.players.size}/${race.maxPlayers})")
            
            // Handle lobby countdown based on player count
            updateLobbyCountdown()
            
            return true
        }
        player.sendMessage("§cCouldn't join the race. It might be full.")
        return false
    }
    
    private fun updateLobbyCountdown() {
        val race = currentRace ?: return
        if (race.state != RaceState.WAITING) return

        if (!locationManager.hasRequiredLocations()) return

        val playerCount = race.players.size

        when {
            playerCount >= race.maxPlayers -> {
                setLobbyTime(5)
                broadcastToRace("§a§lLobby full! Race starting in §6§l5 seconds§a§l!")
            }
            playerCount >= 3 -> {
                if (lobbyTimeLeft > 60 || lobbyTimeLeft == 0) {
                    setLobbyTime(60)
                    broadcastToRace("§eRace starting in §6§l1 minute§e! (${playerCount}/${race.maxPlayers} players)")
                }
            }
            playerCount >= race.minPlayers -> {
                if (lobbyCountdownTask == null) {
                    setLobbyTime(180)
                    broadcastToRace("§eRace starting in §6§l3 minutes§e! (${playerCount}/${race.maxPlayers} players)")
                }
            }
        }
    }
    
    private fun setLobbyTime(seconds: Int) {
        lobbyTimeLeft = seconds

        lobbyCountdownTask?.cancel()

        lobbyCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val race = currentRace ?: run {
                lobbyCountdownTask?.cancel()
                return@Runnable
            }
            
            if (race.state != RaceState.WAITING) {
                lobbyCountdownTask?.cancel()
                return@Runnable
            }

            when (lobbyTimeLeft) {
                120 -> broadcastToRace("§eRace starting in §6§l2 minutes§e!")
                60 -> broadcastToRace("§eRace starting in §6§l1 minute§e!")
                30 -> broadcastToRace("§eRace starting in §6§l30 seconds§e!")
                10 -> broadcastToRace("§eRace starting in §6§l10 seconds§e!")
                5, 4, 3, 2, 1 -> broadcastToRace("§e§lStarting in §6§l$lobbyTimeLeft§e§l...")
            }
            
            if (lobbyTimeLeft <= 0) {
                lobbyCountdownTask?.cancel()
                lobbyCountdownTask = null

                try {
                    startRace(force = true)
                } catch (e: Exception) {
                    broadcastToRace("§cCouldn't start race: ${e.message}")
                }
                return@Runnable
            }
            
            lobbyTimeLeft--
        }, 0L, 20L)
    }

    fun removePlayer(player: Player): Boolean {
        val race = currentRace ?: return false

        val racePlayer = race.players[player.uniqueId]

        racePlayer?.boat?.let { boat ->
            boat.eject()
            boat.remove()
        }
        
        val removed = race.removePlayer(player.uniqueId)

        if (removed) {
            broadcastToRace("§c${player.name} left the race. (${race.players.size}/${race.maxPlayers})")

            if (race.state == RaceState.WAITING && race.players.size < race.minPlayers) {
                lobbyCountdownTask?.cancel()
                lobbyCountdownTask = null
                lobbyTimeLeft = 0
                broadcastToRace("§cNot enough players. Countdown cancelled.")
            }
        }
        race.removeSpectator(player.uniqueId)
        return removed
    }

    fun startRace(force: Boolean = false) {
        val race = currentRace ?: throw IllegalStateException("No race to start")
        if (!force && !race.canStart()) {
            throw IllegalStateException("Cannot start race: Not enough players (${race.players.size}/${race.minPlayers})")
        }
        if (race.players.isEmpty()) {
            throw IllegalStateException("Cannot start race: No players!")
        }
        if (!locationManager.hasRequiredLocations()) {
            val missing = mutableListOf<String>()
            if (locationManager.getFinishLine() == null) missing.add("finish line")
            if (locationManager.getSpawnPositions().isEmpty()) missing.add("spawn positions")
            if (locationManager.getCheckpoints().isEmpty()) missing.add("checkpoints")
            throw IllegalStateException("Race setup incomplete! Missing: ${missing.joinToString(", ")}. Use /race list to check.")
        }
        if (locationManager.getSpawnPositions().size < race.players.size) {
            throw IllegalStateException("Not enough spawn positions! Need ${race.players.size}, have ${locationManager.getSpawnPositions().size}")
        }

        race.state = RaceState.STARTING
        teleportPlayersToStart()
        spawnBoats()
        startCountdown()
    }

    private fun teleportPlayersToStart() {
        val race = currentRace ?: return
        val spawnPositions = locationManager.getSpawnPositions()

        race.players.values.forEachIndexed { index, racePlayer ->
            val player = Bukkit.getPlayer(racePlayer.uuid) ?: return@forEachIndexed
            val spawnLoc = spawnPositions.getOrNull(index) ?: return@forEachIndexed
            player.teleport(spawnLoc)
        }
    }

    private fun spawnBoats() {
        val race = currentRace ?: return

        race.players.values.forEach { racePlayer ->
            val player = Bukkit.getPlayer(racePlayer.uuid) ?: return@forEach
            val boat = player.world.spawnEntity(player.location, EntityType.OAK_BOAT) as Boat
            boat.addPassenger(player)
            racePlayer.boat = boat
        }
    }

    private fun placeBarriers() {
        barrierBlocks.clear()
        
        val finishLine = locationManager.getFinishLine() ?: return
        val pos1 = finishLine.first
        val pos2 = finishLine.second
        val world = pos1.world ?: return
        
        val minX = minOf(pos1.blockX, pos2.blockX)
        val maxX = maxOf(pos1.blockX, pos2.blockX)
        val minZ = minOf(pos1.blockZ, pos2.blockZ)
        val maxZ = maxOf(pos1.blockZ, pos2.blockZ)
        val y = pos1.blockY

        val xLength = maxX - minX
        val zLength = maxZ - minZ

        val isXAligned = xLength >= zLength

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (yOffset in 0..2) {
                    for (offset in listOf(2, -2)) {
                        val barrierLoc = if (isXAligned) {
                            Location(world, x.toDouble(), (y + yOffset).toDouble(), (z + offset).toDouble())
                        } else {
                            Location(world, (x + offset).toDouble(), (y + yOffset).toDouble(), z.toDouble())
                        }
                        
                        val block = barrierLoc.block
                        val replaceable = setOf(
                            Material.AIR,
                            Material.CAVE_AIR,
                            Material.WATER,
                            Material.ICE,
                            Material.PACKED_ICE,
                            Material.BLUE_ICE,
                            Material.LIGHT
                        )
                        if (block.type in replaceable) {
                            barrierBlocks[barrierLoc.clone()] = block.blockData.clone()
                            block.type = Material.RED_STAINED_GLASS
                        }
                    }
                }
            }
        }
        
        if (barrierBlocks.isNotEmpty()) {
            broadcastToRace("§c§lBarriers up! Wait for GO!")
        }
    }

    private fun removeBarriers() {
        barrierBlocks.forEach { (loc, originalBlockData) ->
            if (loc.block.type == Material.RED_STAINED_GLASS) {
                loc.block.blockData = originalBlockData
            }
        }
        barrierBlocks.clear()
    }

    private fun startCountdown() {
        val race = currentRace ?: return
        var countdown = 5

        placeBarriers()

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            when (countdown) {
                in 1..5 -> {
                    val color = when (countdown) {
                        5, 4 -> "§c"
                        3, 2 -> "§e"
                        else -> "§a"
                    }
                    showTitle("$color§l$countdown", "§7Get ready!")
                    playSound(Sound.BLOCK_NOTE_BLOCK_HAT)
                }

                0 -> {
                    removeBarriers()
                    showTitle("§a§lGO!", "§7Race started!")
                    playSound(Sound.BLOCK_NOTE_BLOCK_BELL)
                    race.state = RaceState.RACING
                    race.startTime = System.currentTimeMillis()
                    countdownTask?.cancel()
                }
            }
            countdown--
        }, 0L, 20L)
    }

    private fun showTitle(title: String, subtitle: String) {
        val race = currentRace ?: return
        race.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendTitle(title, subtitle, 0, 25, 10)
        }
        race.spectators.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendTitle(title, subtitle, 0, 25, 10)
        }
    }

    fun finishPlayer(player: Player): Boolean {
        val race = currentRace ?: return false
        if (race.state != RaceState.RACING) return false

        val racePlayer = race.players[player.uniqueId] ?: return false
        if (racePlayer.isFinished()) return false

        racePlayer.finishTime = System.currentTimeMillis()
        racePlayer.position = race.getFinishedPlayers().size

        val timeSeconds = racePlayer.getElapsedTime() / 1000.0

        player.sendTitle("§a§lFINISHED!", "§ePosition #${racePlayer.position}", 5, 40, 10)

        broadcastToRace("§6${player.name} §efinished #${racePlayer.position}! (${race.totalLaps} laps in ${String.format("%.2f", timeSeconds)}s)")

        giveReward(player, racePlayer.position)

        if (race.getActivePlayers().isEmpty()) {
            finishTimerTask?.cancel()
            endRace()
        } else if (racePlayer.position == 1) {
            startFinishTimer()
        }
        
        return true
    }
    
    private fun startFinishTimer() {
        val race = currentRace ?: return
        finishTimeLeft = 180

        broadcastToRace("§e§l3 minutes remaining for other players to finish!")
        
        finishTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val currentRace = currentRace ?: run {
                finishTimerTask?.cancel()
                return@Runnable
            }

            if (currentRace.getActivePlayers().isEmpty()) {
                finishTimerTask?.cancel()
                return@Runnable
            }

            when (finishTimeLeft) {
                120 -> broadcastToRace("§e2 minutes remaining!")
                60 -> broadcastToRace("§e1 minute remaining!")
                30 -> broadcastToRace("§c30 seconds remaining!")
                10 -> broadcastToRace("§c§l10 seconds remaining!")
                5, 4, 3, 2, 1 -> broadcastToRace("§c§l$finishTimeLeft...")
            }
            
            if (finishTimeLeft <= 0) {
                finishTimerTask?.cancel()
                broadcastToRace("§c§lTime's up! Race ending...")

                currentRace.getActivePlayers().forEach { racePlayer ->
                    val p = Bukkit.getPlayer(racePlayer.uuid)
                    p?.sendTitle("§c§lDNF", "§7Did Not Finish", 5, 40, 10)
                }
                
                endRace()
                return@Runnable
            }
            
            finishTimeLeft--
        }, 0L, 20L)
    }

    fun endRace() {
        val race = currentRace ?: return
        race.state = RaceState.FINISHED
        race.endTime = System.currentTimeMillis()
        broadcastToRace("§a§l========== RACE RESULTS ==========")
        race.getFinishedPlayers().forEachIndexed { index, racePlayer ->
            val time = racePlayer.getElapsedTime() / 1000.0
            broadcastToRace("§e#${index + 1} §f${racePlayer.name} §7- §6${String.format("%.2f", time)}s")
        }
        broadcastToRace("§a§l==================================")
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            cleanupRace()
        }, 200L)
    }

    fun cleanupRace() {
        val race = currentRace ?: return
        val previousLaps = race.totalLaps
        
        race.players.values.forEach { racePlayer ->
            val player = Bukkit.getPlayer(racePlayer.uuid)
            player?.let {
                racePlayer.boat?.remove()
            }
        }

        currentRace = null
        countdownTask?.cancel()
        lobbyCountdownTask?.cancel()
        lobbyCountdownTask = null
        lobbyTimeLeft = 0
        finishTimerTask?.cancel()
        finishTimerTask = null
        finishTimeLeft = 0
        removeBarriers()

        createRace(totalLaps = previousLaps)
        broadcastToServer("§a§lNew race available! Click the sign or use /race join")
    }
    
    private fun broadcastToServer(message: String) {
        Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }
    }

    fun isPlayerRacing(player: Player) : Boolean {
        val race = currentRace ?: return false
        return race.isPlayerInRace(player.uniqueId) && race.state == RaceState.RACING
    }

    fun getRacePlayer(player: Player): RacePlayer? {
        return currentRace?.players?.get(player.uniqueId)
    }

    private fun broadcastToRace(message: String) {
        val race = currentRace ?: return

        race.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendMessage(message)
        }

        race.spectators.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendMessage(message)
        }
    }

    private fun playSound(sound: Sound) {
        val race = currentRace ?: return
        race.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.playSound(
                Bukkit.getPlayer(uuid)!!.location,
                sound,
                1.0f,
                1.0f
            )
        }
    }

    private fun giveReward(player: Player, position: Int) {
        val rewardPath = when (position) {
            1 -> "rewards.1st-place"
            2 -> "rewards.2nd-place"
            3 -> "rewards.3rd-place"
            else -> return
        }

        val commands = plugin.config.getStringList(rewardPath)
        if (commands.isEmpty()) return

        commands.forEach { command ->
            val parsedCommand = command.replace("{player}", player.name)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand)
        }

        player.sendMessage("§a§lReward received for finishing #$position!")
    }
}