package no.blackjack.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class BlackJackTable {

    private List<ItemStack> dealerDeck;
    private List<ItemStack> cardStack;
    private List<BlackJackPlayer> players;
    private boolean isRestarting;
    private UUID id;

    public BlackJackTable(List<BlackJackPlayer> players) {
        this.players = players;
    }

    // Getter and Setter for dealerDeck
    public List<ItemStack> getDealerDeck() {
        return dealerDeck;
    }

    public void setDealerDeck(List<ItemStack> dealerDeck) {
        this.dealerDeck = dealerDeck;
    }

    // Getter and Setter for cardStack
    public List<ItemStack> getCardStack() {
        return cardStack;
    }

    public void setCardStack(List<ItemStack> cardStack) {
        this.cardStack = cardStack;
    }

    // Getter and Setter for players
    public List<BlackJackPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<BlackJackPlayer> players) {
        this.players = players;
    }

    // Getter and Setter for isRestarting
    public boolean isRestarting() {
        return isRestarting;
    }

    public void setRestarting(boolean restarting) {
        isRestarting = restarting;
    }

    // Getter and Setter for id
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
