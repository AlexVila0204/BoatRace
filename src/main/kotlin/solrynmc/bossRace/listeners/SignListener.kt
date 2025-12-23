package solrynmc.bossRace.listeners

import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import solrynmc.bossRace.managers.RaceManager

class SignListener(
    private val raceManager: RaceManager
) : Listener {

    companion object {
        const val SIGN_TAG = "[BossRace]"
    }

    @EventHandler
    fun onSignCreate(event: SignChangeEvent) {
        val line0 = event.getLine(0) ?: return

        if (line0.equals(SIGN_TAG, ignoreCase = true)) {
            if (!event.player.hasPermission("bossrace.admin")) {
                event.setLine(0, "")
                event.setLine(1, "")
                event.setLine(2, "")
                event.setLine(3, "")
                event.player.sendMessage("§cYou don't have permission to create race signs!")
                return
            }

            event.setLine(0, "§6§l[BossRace]")
            event.setLine(1, "§aClick to join")
            event.setLine(2, "§7the race!")
            event.setLine(3, "")
            
            event.player.sendMessage("§a§lBossRace sign created!")
        }
    }

    @EventHandler
    fun onSignClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return
        val sign = block.state as? Sign ?: return

        val line0 = sign.getLine(0)
        if (!line0.contains("BossRace", ignoreCase = true)) return

        event.isCancelled = true

        val player = event.player
        val race = raceManager.getCurrentRace()
        
        if (race == null) {
            player.sendMessage("§cNo race available! Wait for an admin to create one.")
            return
        }

        if (raceManager.addPlayer(player)) {
            player.sendMessage("§a§lYou joined the race!")
        }
    }
}

