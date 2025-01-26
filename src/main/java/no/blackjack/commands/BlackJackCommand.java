package no.blackjack.commands;

import net.kyori.adventure.text.JoinConfiguration;
import no.blackjack.handlers.BlackJackHandler;
import no.blackjack.models.BlackJackPlayer;
import no.blackjack.models.BlackJackTable;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlackJackCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (sender instanceof Player p) {
            if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
                if (BlackJackHandler.getPlayer(p.getUniqueId()) == null) {
                    BlackJackHandler.createTable(p.getUniqueId());
                    return true;
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                Player gameHost = Bukkit.getPlayer(args[1]);

                if (gameHost == null || !gameHost.isOnline()) {
                    Component errorMessage = Component.text("Spilleren er ikke pålogget!", NamedTextColor.RED);
                    p.sendMessage(errorMessage);
                    return true;
                }
                if (gameHost == p) {
                    p.sendMessage(
                            Component.text("Du kan ikke bli med deg selv. Lag et eget bord.", NamedTextColor.RED)
                                    .append(Component.text(" Bruk: ", NamedTextColor.GRAY))
                                    .append(Component.text("/blackjack start", NamedTextColor.YELLOW))
                    );
                    return true;
                }

                BlackJackTable joinTable = BlackJackHandler.getTable(BlackJackHandler.getPlayer(gameHost.getUniqueId()));
                if (joinTable == null) {
                    Component errorMessage = Component.text("Spilleren spiller ikke blackjack.", NamedTextColor.RED);
                    p.sendMessage(errorMessage);
                    return true;
                }

                if (joinTable.getPlayers().size() >= 4) {
                    Component errorMessage = Component.text("Bordet er fullt!", NamedTextColor.RED);
                    p.sendMessage(errorMessage);
                    return true;
                }

                BlackJackPlayer tablePlayer = new BlackJackPlayer(p.getUniqueId());
                List<BlackJackPlayer> tablePlayers = joinTable.getPlayers();

                if (!BlackJackHandler.isFinish(joinTable)) {
                    BlackJackHandler.openBet(p.getUniqueId(), joinTable);
                    List<ItemStack> newDeck = new ArrayList<>();
                    newDeck.add(BlackJackHandler.getNextCard(joinTable));
                    newDeck.add(BlackJackHandler.getNextCard(joinTable));
                    tablePlayer.setPlayerDeck(newDeck);
                }
                tablePlayers.add(tablePlayer);
                joinTable.setPlayers(tablePlayers);
                BlackJackHandler.updateTable(joinTable);

                return true;
                
                
                
            } else if (args.length == 1 && args[0].equalsIgnoreCase("liste")) {
                if (!BlackJackHandler.tables.isEmpty()) {
                    // Collect all player names from all tables
                    List<Component> playerNames = BlackJackHandler.tables.stream()
                            .flatMap(table -> table.getPlayers().stream())
                            .map(player -> Bukkit.getPlayer(player.getPlayer()))
                            .filter(Objects::nonNull)
                            .map(Player::displayName)
                            .toList();

                    if (!playerNames.isEmpty()) {
                        // Create the message with player names joined by ", "
                        Component message = Component.text("Spillere: ", NamedTextColor.GREEN)
                                .append(Component.join(JoinConfiguration.separator(Component.text(", ")), playerNames))
                                .append(Component.text("."));
                        p.sendMessage(message);
                    } else {
                        // No players found
                        p.sendMessage(Component.text("Det eksisterer ingen spillere på bordene.", NamedTextColor.RED));
                    }
                } else {
                    // No tables found
                    p.sendMessage(Component.text("Det eksisterer ingen bord.", NamedTextColor.RED)
                            .append(Component.text(" Lag et eget med /blackjack start.", NamedTextColor.GRAY)));
                }
            } else {
                p.sendMessage(
                        Component.text("Bruk: ", NamedTextColor.GRAY)
                                .append(Component.text("/blackjack <start/liste/join> [spiller]", NamedTextColor.YELLOW))
                );
            }
        }
        return true;
    }
}