package solrynmc.bossRace.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import solrynmc.bossRace.managers.LocationManager
import solrynmc.bossRace.managers.RaceManager

class RaceCommand (
    private val raceManager: RaceManager,
    private val locationManager: LocationManager
): CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) : Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "join" -> handleJoin(sender)
            "leave" -> handleLeave(sender)
            "start" -> handleStart(sender, false)
            "forcestart" -> handleStart(sender, true)
            "create" -> handleCreate(sender, args)
            "end" -> handleEnd(sender)
            "status" -> handleStatus(sender)
            "start1" -> handleStartPos1(sender)
            "start2" -> handleStartPos2(sender)
            "finish1" -> handleFinishPos1(sender)
            "finish2" -> handleFinishPos2(sender)
            "addspawn" -> handleAddSpawn(sender)
            "clearspawns" -> handleClearSpawns(sender)
            "checkpoint1" -> handleCheckpointPos1(sender)
            "checkpoint2" -> handleCheckpointPos2(sender)
            "removecheckpoint" -> handleRemoveCheckpoint(sender, args)
            "list" -> handleListLocations(sender)
            "reload" -> handleReload(sender)
            else -> showHelp(sender)
        }

        return true
    }

    private fun showHelp(player: Player) {
        player.sendMessage("§6§l========== BOAT RACE ==========")
        player.sendMessage("§eClick the §6[BossRace] §esign to join!")
        player.sendMessage("§e/race leave §7- Leave the race")
        player.sendMessage("§e/race status §7- View race status")
        
        if (player.hasPermission("bossrace.admin")) {
            player.sendMessage("")
            player.sendMessage("§6§lAdmin - Race Control:")
            player.sendMessage("§e/race create [laps] §7- Create a race (default 3 laps)")
            player.sendMessage("§e/race start §7- Start the race")
            player.sendMessage("§e/race forcestart §7- Force start (ignore min players)")
            player.sendMessage("§e/race end §7- End the current race")
            player.sendMessage("")
            player.sendMessage("§6§lAdmin - Configure Lines:")
            player.sendMessage("§e/race start1 §7- Start line point 1")
            player.sendMessage("§e/race start2 §7- Start line point 2")
            player.sendMessage("§e/race finish1 §7- Finish line point 1")
            player.sendMessage("§e/race finish2 §7- Finish line point 2")
            player.sendMessage("")
            player.sendMessage("§6§lAdmin - Spawn Positions:")
            player.sendMessage("§e/race addspawn §7- Add spawn position")
            player.sendMessage("§e/race clearspawns §7- Clear all spawn positions")
            player.sendMessage("")
            player.sendMessage("§6§lAdmin - Checkpoints:")
            player.sendMessage("§e/race checkpoint1 §7- Checkpoint point 1")
            player.sendMessage("§e/race checkpoint2 §7- Checkpoint point 2")
            player.sendMessage("§e/race removecheckpoint <id> §7- Remove checkpoint")
            player.sendMessage("")
            player.sendMessage("§e/race list §7- View all locations")
            player.sendMessage("§e/race reload §7- Reload config")
        }
    }

    private fun handleJoin(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cUse the race sign to join!")
            return
        }
        if (raceManager.addPlayer(player)) {
            player.sendMessage("§aYou joined the race!")
        }
    }

    private fun handleLeave(player: Player) {
        if (raceManager.removePlayer(player)) {
            player.sendMessage("§eYou left the race.")
        } else {
            player.sendMessage("§cYou're not in a race!")
        }
    }

    private fun handleStart(player: Player, force: Boolean) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission to start races!")
            return
        }

        try {
            raceManager.startRace(force)
            if (force) {
                player.sendMessage("§eForce starting race...")
            }
        } catch (e: Exception) {
            player.sendMessage("§c${e.message}")
        }
    }

    private fun handleStatus(player: Player) {
        val race = raceManager.getCurrentRace()

        if (race == null) {
            player.sendMessage("§cNo active race!")
            return
        }

        player.sendMessage("§6§l===== RACE STATUS =====")
        player.sendMessage("§eState: §f${race.state}")
        player.sendMessage("§eLaps: §f${race.totalLaps}")
        player.sendMessage("§ePlayers: §f${race.players.size}/${race.maxPlayers}")
        player.sendMessage("§eSpectators: §f${race.spectators.size}")

        if (race.players.isNotEmpty()) {
            player.sendMessage("§6Players:")
            race.players.values.forEach { racePlayer ->
                val status = if (racePlayer.isFinished()) {
                    "§aFinished"
                } else {
                    "§eLap ${racePlayer.currentLap}/${race.totalLaps}"
                }
                player.sendMessage("  §f${racePlayer.name} $status")
            }
        }
    }

    private fun handleCreate(player: Player, args: Array<out String>) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        val laps = args.getOrNull(1)?.toIntOrNull() ?: 3
        if (laps < 1 || laps > 10) {
            player.sendMessage("§cLaps must be between 1 and 10!")
            return
        }
        try {
            raceManager.createRace(totalLaps = laps)
            player.sendMessage("§aRace created with §6$laps laps§a! Players can now join with /race join")
        } catch (e: Exception) {
            player.sendMessage("§c${e.message}")
        }
    }

    private fun handleEnd(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        if (raceManager.getCurrentRace() == null) {
            player.sendMessage("§cNo active race to end!")
            return
        }
        raceManager.endRace()
        player.sendMessage("§aRace ended!")
    }

    private fun handleStartPos1(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.setStartPos1(player.location)
        player.sendMessage("§a✓ Start line point 1 set")
        player.sendMessage("§7Now use §e/race start2 §7for the second point")
    }

    private fun handleStartPos2(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.setStartPos2(player.location)
        player.sendMessage("§a✓ Start line point 2 set")
        player.sendMessage("§aStart line configured!")
    }

    private fun handleFinishPos1(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.setFinishPos1(player.location)
        player.sendMessage("§a✓ Finish line point 1 set")
        player.sendMessage("§7Now use §e/race finish2 §7for the second point")
    }

    private fun handleFinishPos2(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.setFinishPos2(player.location)
        player.sendMessage("§a✓ Finish line point 2 set")
        player.sendMessage("§aFinish line configured!")
    }

    private fun handleAddSpawn(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.addSpawnPosition(player.location)
        val count = locationManager.getSpawnPositions().size
        player.sendMessage("§a✓ Spawn position #$count added")
    }

    private fun handleClearSpawns(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.clearSpawnPositions()
        player.sendMessage("§a✓ All spawn positions cleared")
    }

    private fun handleCheckpointPos1(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        locationManager.setCheckpointPos1(player.location)
        player.sendMessage("§a✓ Checkpoint point 1 set")
        player.sendMessage("§7Now use §e/race checkpoint2 §7to complete the checkpoint")
    }

    private fun handleCheckpointPos2(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        if (locationManager.setCheckpointPos2(player.location)) {
            val count = locationManager.getCheckpoints().size
            player.sendMessage("§a✓ Checkpoint #$count created")
        } else {
            player.sendMessage("§cFirst set point 1 with /race checkpoint1!")
        }
    }

    private fun handleRemoveCheckpoint(player: Player, args: Array<out String>) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        val index = args.getOrNull(1)?.toIntOrNull()
        if (index == null) {
            player.sendMessage("§cUsage: /race removecheckpoint <id>")
            return
        }
        if (locationManager.removeCheckpoint(index - 1)) {
            player.sendMessage("§a✓ Checkpoint #$index removed")
        } else {
            player.sendMessage("§cInvalid checkpoint ID!")
        }
    }

    private fun handleReload(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }

        val plugin = player.server.pluginManager.getPlugin("BossRace") as? solrynmc.bossRace.BossRace
        plugin?.reloadConfig()
        locationManager.loadLocations()

        player.sendMessage("§a§lBossRace config reloaded!")
    }

    private fun handleListLocations(player: Player) {
        if (!player.hasPermission("bossrace.admin")) {
            player.sendMessage("§cYou don't have permission!")
            return
        }
        player.sendMessage("§6§l===== RACE CONFIGURATION =====")

        val startLine = locationManager.getStartLine()
        if (startLine != null) {
            player.sendMessage("§aStart Line: §f✓ Configured")
            player.sendMessage("  §7Pos1: ${startLine.first.blockX}, ${startLine.first.blockY}, ${startLine.first.blockZ}")
            player.sendMessage("  §7Pos2: ${startLine.second.blockX}, ${startLine.second.blockY}, ${startLine.second.blockZ}")
        } else {
            player.sendMessage("§cStart Line: §f✗ Not configured")
        }

        val finishLine = locationManager.getFinishLine()
        if (finishLine != null) {
            player.sendMessage("§aFinish Line: §f✓ Configured")
            player.sendMessage("  §7Pos1: ${finishLine.first.blockX}, ${finishLine.first.blockY}, ${finishLine.first.blockZ}")
            player.sendMessage("  §7Pos2: ${finishLine.second.blockX}, ${finishLine.second.blockY}, ${finishLine.second.blockZ}")
        } else {
            player.sendMessage("§cFinish Line: §f✗ Not configured")
        }

        val spawns = locationManager.getSpawnPositions()
        player.sendMessage("§eSpawn Positions: §f${spawns.size}")
        spawns.forEachIndexed { i, loc ->
            player.sendMessage("  §7#${i+1}: ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}")
        }

        val checkpoints = locationManager.getCheckpoints()
        player.sendMessage("§eCheckpoints: §f${checkpoints.size}")
        checkpoints.forEachIndexed { i, (pos1, pos2) ->
            player.sendMessage("  §7#${i+1}: (${pos1.blockX},${pos1.blockZ}) → (${pos2.blockX},${pos2.blockZ})")
        }

        player.sendMessage("")
        if (locationManager.hasRequiredLocations()) {
            player.sendMessage("§a✓ Configuration complete! Ready for races.")
        } else {
            player.sendMessage("§c✗ Incomplete configuration. You need:")
            if (finishLine == null) player.sendMessage("  §7- Finish line (finish1, finish2)")
            if (spawns.isEmpty()) player.sendMessage("  §7- At least 1 spawn position (addspawn)")
            if (checkpoints.isEmpty()) player.sendMessage("  §7- At least 1 checkpoint (checkpoint1, checkpoint2)")
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val subcommands = mutableListOf("leave", "status")
            if (sender.hasPermission("bossrace.admin")) {
                subcommands.addAll(listOf(
                    "join", "start", "forcestart", "create", "end",
                    "finish1", "finish2",
                    "addspawn", "clearspawns",
                    "checkpoint1", "checkpoint2", "removecheckpoint",
                    "list", "reload"
                ))
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
