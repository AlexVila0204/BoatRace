package solrynmc.bossRace

import org.bukkit.plugin.java.JavaPlugin
import solrynmc.bossRace.commands.RaceCommand
import solrynmc.bossRace.listeners.BoatListener
import solrynmc.bossRace.listeners.SignListener
import solrynmc.bossRace.managers.LocationManager
import solrynmc.bossRace.managers.RaceManager

class BossRace : JavaPlugin() {

    private lateinit var raceManager: RaceManager
    private lateinit var locationManager: LocationManager

    override fun onEnable() {
        logger.info("BossRace plugin is enabling...")

        saveDefaultConfig()

        locationManager = LocationManager(this)
        raceManager = RaceManager(this, locationManager)

        server.pluginManager.registerEvents(BoatListener(raceManager, locationManager), this)
        server.pluginManager.registerEvents(SignListener(raceManager), this)

        val raceCommand = RaceCommand(raceManager, locationManager)
        getCommand("race")?.setExecutor(raceCommand)
        getCommand("race")?.tabCompleter = raceCommand

        val defaultLaps = config.getInt("race.default-laps", 3)
        raceManager.createRace(totalLaps = defaultLaps)
        
        logger.info("BossRace plugin enabled successfully!")
    }

    override fun onDisable() {
        logger.info("BossRace plugin is disabling...")

        raceManager.cleanupRace()

        logger.info("BossRace plugin disabled successfully!")
    }

    fun getLocationManager(): LocationManager = locationManager
    fun getRaceManager(): RaceManager = raceManager
}
