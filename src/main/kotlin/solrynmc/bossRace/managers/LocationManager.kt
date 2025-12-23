package solrynmc.bossRace.managers

import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import solrynmc.bossRace.BossRace

class LocationManager(private val plugin: BossRace) {
    private val config: FileConfiguration = plugin.config

    private var startPos1: Location? = null
    private var startPos2: Location? = null

    private var finishPos1: Location? = null
    private var finishPos2: Location? = null

    private val spawnPositions = mutableListOf<Location>()

    private val checkpoints = mutableListOf<Pair<Location, Location>>()

    init {
        loadLocations()
    }

    fun loadLocations() {
        spawnPositions.clear()
        checkpoints.clear()

        startPos1 = config.getLocation("locations.startLine.pos1")
        startPos2 = config.getLocation("locations.startLine.pos2")

        finishPos1 = config.getLocation("locations.finishLine.pos1")
        finishPos2 = config.getLocation("locations.finishLine.pos2")

        val spawnSection = config.getConfigurationSection("locations.spawns")
        spawnSection?.getKeys(false)?.forEach { key ->
            val loc = config.getLocation("locations.spawns.$key")
            loc?.let { spawnPositions.add(it) }
        }

        val checkpointSection = config.getConfigurationSection("locations.checkpoints")
        checkpointSection?.getKeys(false)?.forEach { key ->
            val pos1 = config.getLocation("locations.checkpoints.$key.pos1")
            val pos2 = config.getLocation("locations.checkpoints.$key.pos2")
            if (pos1 != null && pos2 != null) {
                checkpoints.add(Pair(pos1, pos2))
            }
        }
    }

    fun setStartPos1(location: Location) {
        startPos1 = location
        config.set("locations.startLine.pos1", location)
        plugin.saveConfig()
    }

    fun setStartPos2(location: Location) {
        startPos2 = location
        config.set("locations.startLine.pos2", location)
        plugin.saveConfig()
    }

    fun getStartLine(): Pair<Location, Location>? {
        val p1 = startPos1 ?: return null
        val p2 = startPos2 ?: return null
        return Pair(p1, p2)
    }

    fun setFinishPos1(location: Location) {
        finishPos1 = location
        config.set("locations.finishLine.pos1", location)
        plugin.saveConfig()
    }

    fun setFinishPos2(location: Location) {
        finishPos2 = location
        config.set("locations.finishLine.pos2", location)
        plugin.saveConfig()
    }

    fun getFinishLine(): Pair<Location, Location>? {
        val p1 = finishPos1 ?: return null
        val p2 = finishPos2 ?: return null
        return Pair(p1, p2)
    }

    fun addSpawnPosition(location: Location) {
        val index = spawnPositions.size
        spawnPositions.add(location)
        config.set("locations.spawns.$index", location)
        plugin.saveConfig()
    }

    fun clearSpawnPositions() {
        spawnPositions.clear()
        config.set("locations.spawns", null)
        plugin.saveConfig()
    }

    fun getSpawnPositions(): List<Location> = spawnPositions.toList()

    private var tempCheckpointPos1: Location? = null

    fun setCheckpointPos1(location: Location) {
        tempCheckpointPos1 = location
    }

    fun setCheckpointPos2(location: Location): Boolean {
        val pos1 = tempCheckpointPos1 ?: return false
        val index = checkpoints.size
        checkpoints.add(Pair(pos1, location))
        config.set("locations.checkpoints.$index.pos1", pos1)
        config.set("locations.checkpoints.$index.pos2", location)
        plugin.saveConfig()
        tempCheckpointPos1 = null
        return true
    }

    fun removeCheckpoint(index: Int): Boolean {
        if (index < 0 || index >= checkpoints.size) return false
        checkpoints.removeAt(index)

        config.set("locations.checkpoints", null)
        checkpoints.forEachIndexed { i, (pos1, pos2) ->
            config.set("locations.checkpoints.$i.pos1", pos1)
            config.set("locations.checkpoints.$i.pos2", pos2)
        }
        plugin.saveConfig()
        return true
    }

    fun getCheckpoints(): List<Pair<Location, Location>> = checkpoints.toList()

    fun hasRequiredLocations(): Boolean {
        return finishPos1 != null && finishPos2 != null &&
               spawnPositions.isNotEmpty() &&
               checkpoints.isNotEmpty()
    }

    fun getSpawnPosition(index: Int): Location? {
        return spawnPositions.getOrNull(index)
    }

    fun isPlayerCrossingLine(playerLoc: Location, prevLoc: Location, lineStart: Location, lineEnd: Location): Boolean {
        if (playerLoc.world != lineStart.world) return false

        val p1x = prevLoc.x
        val p1z = prevLoc.z
        val p2x = playerLoc.x
        val p2z = playerLoc.z
        
        val p3x = lineStart.x
        val p3z = lineStart.z
        val p4x = lineEnd.x
        val p4z = lineEnd.z
        
        val d = (p4z - p3z) * (p2x - p1x) - (p4x - p3x) * (p2z - p1z)
        if (d == 0.0) return false

        val ua = ((p4x - p3x) * (p1z - p3z) - (p4z - p3z) * (p1x - p3x)) / d
        val ub = ((p2x - p1x) * (p1z - p3z) - (p2z - p1z) * (p1x - p3x)) / d

        return ua in 0.0..1.0 && ub in 0.0..1.0
    }

    fun getStartLocations(): List<Location> = spawnPositions.toList()
    fun getFinishLocation(): Location? = finishPos1
}
