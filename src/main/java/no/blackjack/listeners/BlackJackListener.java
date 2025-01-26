package no.blackjack.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import no.blackjack.BlackJackPlugin;
import no.blackjack.handlers.BlackJackHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class BlackJackListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (isTitleMatching(e.getView().title(), "BlackJack")) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            UUID uuid = e.getWhoClicked().getUniqueId();

            if (BlackJackHandler.getPlayer(uuid) == null || BlackJackHandler.getTable(BlackJackHandler.getPlayer(uuid)) == null) {
                e.getWhoClicked().closeInventory(Reason.CANT_USE);
                return;
            }
            if (item == null || !item.hasItemMeta()) {
                return;
            }

            ItemMeta meta = item.getItemMeta();
            String itemName = getPlainText(meta.itemName());

            if (itemName.equals("Trekk") && item.getType() != Material.BARRIER) {
                BlackJackHandler.hit(uuid);
            } else if (itemName.equals("Stå")) {
                BlackJackHandler.stand(uuid);
            } else if (itemName.equals("Sats") ) {
                BlackJackHandler.openBet(uuid, BlackJackHandler.getTable(BlackJackHandler.getPlayer(uuid)));
            } else if (itemName.equals("Doble")) {
                BlackJackHandler.doubleBet(uuid);
            } else if (itemName.equals("Forlat spillet")) {
                BlackJackHandler.kickPlayer(uuid);
                e.getWhoClicked().closeInventory(Reason.CANT_USE);
            }
        }
        if (isTitleMatching(e.getView().title(),"Sats")) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            UUID uuid = e.getWhoClicked().getUniqueId();
            if (BlackJackHandler.getPlayer(uuid) == null || BlackJackHandler.getTable(BlackJackHandler.getPlayer(uuid)) == null) {
                e.getWhoClicked().closeInventory(Reason.CANT_USE);
                return;
            }
            if (item == null || !item.hasItemMeta()) {
                return;
            }

            ItemMeta meta = item.getItemMeta();
            String itemName = getPlainText(meta.itemName());

            if (itemName.equals("Sats 10 gull") && item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                BlackJackHandler.bet(uuid, 10);
            } else if (itemName.equals("Sats 20 gull") && item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                BlackJackHandler.bet(uuid, 20);
            } else if (itemName.equals("Sats 50 gull") && item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                BlackJackHandler.bet(uuid, 50);
            } else if (itemName.equals("Sats 100 gull") && item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                BlackJackHandler.bet(uuid, 100);
            } else if (itemName.equals("Sats 250 gull") && item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                BlackJackHandler.bet(uuid, 250);
            } else if (itemName.equals("Forlat spillet")) {
                BlackJackHandler.kickPlayer(uuid);
                e.getWhoClicked().closeInventory(Reason.CANT_USE);
            }

        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (isTitleMatching(e.getView().title(),"BlackJack") || isTitleMatching(e.getView().title(),"Sats")) {
            if (e.getReason() != Reason.CANT_USE && e.getReason() != Reason.OPEN_NEW) {
                Player p = (Player) e.getPlayer();
                if (BlackJackHandler.getPlayer(p.getUniqueId()) != null && BlackJackHandler.getTable(BlackJackHandler.getPlayer(p.getUniqueId())) != null) {
                    Bukkit.getScheduler().runTaskLater(BlackJackPlugin.instance, () -> p.openInventory(e.getInventory()), 1L);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (BlackJackHandler.getPlayer(uuid) != null && BlackJackHandler.getTable(BlackJackHandler.getPlayer(uuid)) != null) {
            BlackJackHandler.kickPlayer(uuid);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        UUID uuid = e.getEntity().getUniqueId();
        if (BlackJackHandler.getPlayer(uuid) != null && BlackJackHandler.getTable(BlackJackHandler.getPlayer(uuid)) != null) {
            BlackJackHandler.kickPlayer(uuid);
        }
    }

    private boolean isTitleMatching(Component title, String expected) {
        return PlainTextComponentSerializer.plainText().serialize(title).equals(expected);
    }

    private String getPlainText(Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }
}