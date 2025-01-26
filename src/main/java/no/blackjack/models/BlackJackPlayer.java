package no.blackjack.models;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class BlackJackPlayer {

    private UUID player;
    private List<ItemStack> playerDeck;
    private Inventory table;
    private boolean finish;
    private int bet;
    private boolean doubleBet;

    public BlackJackPlayer(UUID player) {
        this.player = player;
    }

    // Getter and Setter for player
    public UUID getPlayer() {
        return player;
    }

    // Getter and Setter for playerDeck
    public List<ItemStack> getPlayerDeck() {
        return playerDeck;
    }

    public void setPlayerDeck(List<ItemStack> playerDeck) {
        this.playerDeck = playerDeck;
    }

    // Getter and Setter for table
    public Inventory getTable() {
        return table;
    }

    public void setTable(Inventory table) {
        this.table = table;
    }

    // Getter and Setter for finish
    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    // Getter and Setter for bet
    public int getBet() {
        return bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }

    // Getter and Setter for doubleBet
    public boolean isDoubleBet() {
        return doubleBet;
    }

    public void setDoubleBet(boolean doubleBet) {
        this.doubleBet = doubleBet;
    }
}
