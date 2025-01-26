package no.blackjack.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import no.blackjack.BlackJackPlugin;
import no.blackjack.models.BlackJackTable;
import no.blackjack.models.BlackJackPlayer;

import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.bukkit.Bukkit.getOfflinePlayer;

public class BlackJackHandler {

    private static final int decks = 4;

    public static List<BlackJackTable> tables = new ArrayList<>();

    public static void createTable(UUID uuid) {
        List<BlackJackPlayer> players = new ArrayList<>();
        BlackJackPlayer player = new BlackJackPlayer(uuid);
        players.add(player);
        BlackJackTable table = new BlackJackTable(players);
        table.setId(UUID.randomUUID());
        start(table);
    }

    public static void start(BlackJackTable table) {
        tables.removeIf(x -> x.getId().equals(table.getId()));
        table.setCardStack(generateCardStack());
        table.setDealerDeck(getDealerCards(table));
        table.setRestarting(false);
        table.setId(UUID.randomUUID());

        //Kicke alle som ikke har råd
        table.getPlayers().stream()
            .filter(x -> {
                // Check if the player's balance is less than 10 using Vault's economy
                return !BlackJackPlugin.getEconomy().has(getOfflinePlayer(x.getPlayer()), 10.0);
            })
            .toList()
            .forEach(p -> {
            table.getPlayers().remove(p);
            Player player = Bukkit.getPlayer(p.getPlayer());
            if (player != null) {
                Component component = Component.text("Du har ikke nok gull til å spille.", NamedTextColor.RED);
                player.sendMessage(component);
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
            }
        });
        if (table.getPlayers().isEmpty()) {
            return;
        }
        for (BlackJackPlayer p : table.getPlayers()) {
            p.setFinish(false);
            p.setTable(null);
            p.setDoubleBet(false);
            p.setBet(0);
            openBet(p.getPlayer(), table);
        }
        tables.add(table);
        updateTable(table);
    }

    public static void hit(UUID uuid) {
        BlackJackPlayer player = getPlayer(uuid);
        BlackJackTable table = getTable(player);
        if (player == null) return;
        List<ItemStack> playerDeck = player.getPlayerDeck();
        if (playerDeck == null || table == null) return;
        playerDeck.add(getNextCard(table));
        player.setPlayerDeck(playerDeck);
        if (deckValue(playerDeck) >= 21) {
            player.setFinish(true);
        }
        updateTable(table);
    }

    public static void stand(UUID uuid) {
        BlackJackPlayer player = getPlayer(uuid);
        if (player == null) return;
        player.setFinish(true);
        BlackJackTable table = getTable(getPlayer(uuid));
        updateTable(table);
    }

    public static void bet(UUID uuid, int bet) {
        BlackJackPlayer player = getPlayer(uuid);
        if (player == null) return;
        player.setBet(bet);
        OfflinePlayer offlinePlayer = getOfflinePlayer(uuid);
        BlackJackPlugin.getEconomy().withdrawPlayer(offlinePlayer, bet);

        BlackJackTable table = getTable(player);
        if (table == null) return;
        List<ItemStack> newDeck = new ArrayList<>();
        newDeck.add(getNextCard(table));
        newDeck.add(getNextCard(table));
        player.setPlayerDeck(newDeck);
        if (deckValue(newDeck) >= 21) {
            player.setFinish(true);
        }
        updateTable(getTable(getPlayer(uuid)));
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
            p.openInventory(player.getTable());
        }
    }

    public static void openBet(UUID uuid, BlackJackTable table) {
        Component title = Component.text("Sats");
        Inventory inv = Bukkit.createInventory(null, 9, title);

        OfflinePlayer offlinePlayer = getOfflinePlayer(uuid);
        double balance = BlackJackPlugin.getEconomy().getBalance(offlinePlayer);
        Player p = Bukkit.getPlayer(uuid);

        if (p == null) return;
        if (balance < 10) {
            kickPlayer(uuid);
            Component component = Component.text("Du har ikke nok gull til å spille.", NamedTextColor.RED);
            p.sendMessage(component);
            p.closeInventory(Reason.CANT_USE);
            return;
        }

        //Stopper en ny spiller fra å joine post game
        if (isFinish(table)) {
            BlackJackPlayer player = getPlayer(uuid);
            if (player != null) {
                player.setFinish(true);
                player.setBet(0);
                updateTable(getTable(getPlayer(uuid)));
                p.closeInventory(Reason.OPEN_NEW);
                p.openInventory(player.getTable());
            }

        } else {
            Material color = Material.GREEN_STAINED_GLASS_PANE;
            // 10
            ItemStack item = ItemStack.of(color, 1);
            ItemMeta meta = item.getItemMeta();
            Component message = Component.text("Sats 10 gull", NamedTextColor.GOLD);
            meta.itemName(message);
            item.setItemMeta(meta);
            inv.setItem(2, item);

            // 20
            if (balance < 20) {
                color = Material.RED_STAINED_GLASS_PANE;
            }
            item = ItemStack.of(color, 1);
            message = Component.text("Sats 20 gull", NamedTextColor.GOLD);
            meta.itemName(message);
            item.setItemMeta(meta);
            inv.setItem(3, item);
            // 50
            if (balance < 50) {
                color = Material.RED_STAINED_GLASS_PANE;
            }
            item = ItemStack.of(color, 1);
            message = Component.text("Sats 50 gull", NamedTextColor.GOLD);
            meta.itemName(message);
            item.setItemMeta(meta);
            inv.setItem(4, item);
            // 100
            if (balance < 100) {
                color = Material.RED_STAINED_GLASS_PANE;
            }
            item = ItemStack.of(color, 1);
            message = Component.text("Sats 100 gull", NamedTextColor.GOLD);
            meta.itemName(message);
            item.setItemMeta(meta);
            inv.setItem(5, item);
            // 250
            if (balance < 250) {
                color = Material.RED_STAINED_GLASS_PANE;
            }
            item = ItemStack.of(color, 1);
            message = Component.text("Sats 250 gull", NamedTextColor.GOLD);
            meta.itemName(message);
            item.setItemMeta(meta);
            inv.setItem(6, item);

            //Leave

            ItemStack barrier = ItemStack.of(Material.BARRIER, 1);
            ItemMeta metaLeave = barrier.getItemMeta();
            Component itemName = Component.text("Forlat spillet", NamedTextColor.GOLD);
            metaLeave.itemName(itemName);
            List<Component> loreLeave = List.of(
                    Component.text("Forlat for å avslutte spillet."),
                    Component.text("Du vil ikke bli belastet.")
            );
            metaLeave.lore(loreLeave);
            barrier.setItemMeta(metaLeave);
            inv.setItem(8, barrier);

            p.closeInventory(Reason.OPEN_NEW);
            p.openInventory(inv);
        }
    }

    public static void doubleBet(UUID uuid) {

        BlackJackPlayer blackJackPlayer = getPlayer(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p == null || blackJackPlayer == null) {
            return;
        }

        OfflinePlayer offlinePlayer = getOfflinePlayer(uuid);
        double balance = BlackJackPlugin.getEconomy().getBalance(offlinePlayer);

        int bet = blackJackPlayer.getBet();
        if (balance >= bet) {
            BlackJackPlugin.getEconomy().withdrawPlayer(offlinePlayer, bet);

            blackJackPlayer.setBet(bet * 2);
            blackJackPlayer.setDoubleBet(true);
        }
        hit(uuid);
        stand(uuid);
        updateTable(getTable(getPlayer(uuid)));
        p.closeInventory(Reason.OPEN_NEW);
        p.openInventory(blackJackPlayer.getTable());
    }

    public static void updateTable(BlackJackTable table) {

        if (isFinish(table) && !table.isRestarting()) {
            if (!table.isRestarting()) {
                //Restart etter 5 sekunder.
                table.setRestarting(true);
                countDown(table.getId(), 5, false);
            }
            if (table.getDealerDeck().size() < 2) {
                housePlay(table);
                pay(table);
            }
        }
        for (BlackJackPlayer player : table.getPlayers()) {
            //Henter inventory
            Inventory inv = player.getTable();
            if (inv == null) {
                Component title = Component.text("BlackJack");
                inv = Bukkit.createInventory(null, 54, title);
            }
            //Forlat knapp
            ItemStack barrier = ItemStack.of(Material.BARRIER, 1);
            ItemMeta metaLeave = barrier.getItemMeta();
            Component itemName = Component.text("Forlat spillet", NamedTextColor.GOLD);
            metaLeave.itemName(itemName);
            List<Component> loreLeave = new ArrayList<>();
            if (isFinish(table)) {
                loreLeave.add(Component.text("Forlat for å avslutte spillet."));
                loreLeave.add(Component.text("Du vil ikke bli belastet."));
            } else {
                loreLeave.add(Component.text("Ved å forlate spillet "));
                loreLeave.add(Component.text("mister du innsatsen din."));
            }
            metaLeave.lore(loreLeave);
            barrier.setItemMeta(metaLeave);
            inv.setItem(53, barrier);

            //Husets kort
            List<ItemStack> houseDeck = table.getDealerDeck();
            for (int i = 0; i < houseDeck.size(); i++) {
                inv.setItem(i + 3, houseDeck.get(i));
            }
            //Husets status
            ItemStack house = ItemStack.of(Material.GOLD_BLOCK, 1);
            ItemMeta metaHouse = house.getItemMeta();
            itemName = Component.text("Dealer");
            metaHouse.itemName(itemName);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Poeng: " + deckName(houseDeck)));
            metaHouse.lore(lore);
            house.setItemMeta(metaHouse);
            inv.setItem(2, house);

            for (int i = 9; i < 45; i++) {
                inv.setItem(i, null);
            }

            //Looper gjennom opptil 16 ganger
            int i = 9;
            for (BlackJackPlayer v : table.getPlayers()) {

                ItemStack playerHead = getPlayerHead(v.getPlayer());
                ItemMeta meta = playerHead.getItemMeta();
                if (v.getBet() > 0) {

                    OfflinePlayer offlinePlayer = getOfflinePlayer(v.getPlayer());
                    double balance = BlackJackPlugin.getEconomy().getBalance(offlinePlayer);
                    Player vPlayer = Bukkit.getPlayer(v.getPlayer());
                    if (vPlayer == null) return;
                    Component name = vPlayer.name();
                    meta.itemName(name);
                    List<Component> lores = new ArrayList<>();
                    lores.add(Component.text("Poeng: " + deckName(v.getPlayerDeck())));
                    lores.add(Component.text("Innsats: " + v.getBet()));
                    lores.add(Component.text("Saldo: " + (int) balance + "g"));

                    meta.lore(lores);
                }
                playerHead.setItemMeta(meta);
                inv.setItem(i, playerHead);

                if (isFinish(table)) {
                    ItemStack win = ItemStack.of(Material.GOLD_BLOCK, 1);
                    ItemStack tie = ItemStack.of(Material.IRON_BLOCK, 1);
                    ItemStack loss = ItemStack.of(Material.COAL_BLOCK, 1);
                    ItemMeta winMeta = win.getItemMeta();
                    ItemMeta tieMeta = tie.getItemMeta();
                    ItemMeta lossMeta = loss.getItemMeta();
                    winMeta.itemName(Component.text("Vant", NamedTextColor.GREEN));
                    tieMeta.itemName(Component.text("Uavgjort", NamedTextColor.GRAY));
                    lossMeta.itemName(Component.text("Tapte", NamedTextColor.RED));
                    List<Component> winlore = new ArrayList<>();
                    List<Component> tielore = new ArrayList<>();
                    List<Component> losslore = new ArrayList<>();
                    winlore.add(Component.text("Vant " + v.getBet() * 2 + " gull"));
                    tielore.add(Component.text("Fikk tilbake " + v.getBet() + " gull"));
                    losslore.add(Component.text("Tapte " + v.getBet() + " gull"));
                    winMeta.lore(winlore);
                    tieMeta.lore(tielore);
                    lossMeta.lore(losslore);
                    win.setItemMeta(winMeta);
                    tie.setItemMeta(tieMeta);
                    loss.setItemMeta(lossMeta);

                    List<ItemStack> dealerDeck = table.getDealerDeck();
                    int dealerValue = deckValue(dealerDeck);
                    if (dealerDeck.size() == 2 && dealerValue == 21) {
                        //Dealer got blackjack, no payout.
                        inv.setItem(i + 8, loss);
                    } else {
                        if (dealerValue > 21) {
                            dealerValue = 0;
                        }
                        int deckValue = deckValue(v.getPlayerDeck());
                        if (deckValue == dealerValue && !(v.getPlayerDeck().size() == 2 && deckValue == 21)) {
                            inv.setItem(i + 8, tie);
                        } else if (deckValue >= dealerValue && deckValue <= 21) {
                            inv.setItem(i + 8, win);
                        } else {
                            inv.setItem(i + 8, loss);
                        }
                    }
                } else {
                    //Status
                    if (v.isFinish()) {
                        //Første som er ferdig starter AFK timer
                        int j = 0;
                        for (BlackJackPlayer p : table.getPlayers()) {
                            if (p.isFinish()) j++;
                        }
                        if (j == 1) countDown(table.getId(), 30, true);

                        ItemStack status = ItemStack.of(Material.GREEN_CONCRETE, 1);
                        ItemMeta metaStatus = status.getItemMeta();
                        metaStatus.itemName(Component.text("Ferdig"));
                        status.setItemMeta(metaStatus);
                        inv.setItem((i + 8), status);

                    } else {
                        ItemStack status = ItemStack.of(Material.RED_CONCRETE, 1);
                        ItemMeta metaStatus = status.getItemMeta();
                        metaStatus.itemName(Component.text("Spiller..."));
                        status.setItemMeta(metaStatus);
                        inv.setItem((i + 8), status);
                    }
                }

                //Cards
                int j = i + 1;
                if (v.getPlayerDeck() != null && !v.getPlayerDeck().isEmpty() && v.getBet() != 0) {
                    for (ItemStack card : v.getPlayerDeck()) {
                        inv.setItem(j, card);
                        j++;
                    }
                }
                i += 9;
            }
            player.setTable(inv);
        }
        //Personlig utseende av inventoryet
        for (BlackJackPlayer player : table.getPlayers()) {
            Player p = Bukkit.getPlayer(player.getPlayer());
            Inventory inv = player.getTable();

            ItemStack button;
            if (player.isFinish()) {
                button = ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1);
                ItemMeta meta = button.getItemMeta();
                meta.itemName(Component.text(" "));
                button.setItemMeta(meta);
                inv.setItem(47, button);
                inv.setItem(49, button);
            } else {

                button = ItemStack.of(Material.GREEN_STAINED_GLASS_PANE, 1);
                ItemMeta meta = button.getItemMeta();
                //Doble knapp
                OfflinePlayer offlinePlayer = getOfflinePlayer(player.getPlayer());
                double balance = BlackJackPlugin.getEconomy().getBalance(offlinePlayer);
                if (balance >= player.getBet() && player.getPlayerDeck() != null && player.getPlayerDeck().size() == 2) {
                    meta.itemName(Component.text("Doble", NamedTextColor.GREEN));
                    button.setItemMeta(meta);
                    inv.setItem(47, button);
                } else {
                    ItemStack rbutton = ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1);
                    ItemMeta rmeta = rbutton.getItemMeta();
                    rmeta.itemName(Component.text(" ", NamedTextColor.RED));
                    rbutton.setItemMeta(rmeta);
                    inv.setItem(47, rbutton);
                }
                //Trekk knapp
                meta.itemName(Component.text("Trekk", NamedTextColor.GREEN));
                button.setItemMeta(meta);
                inv.setItem(49, button);
                //Stå knapp
                meta.itemName(Component.text("Stå", NamedTextColor.GREEN));
                button.setItemMeta(meta);

            }
            inv.setItem(51, button);
            player.setTable(inv);
            if (p.getOpenInventory() == null) {
                p.openInventory(inv);
            } else if (p.getOpenInventory().getTitle().equalsIgnoreCase("BlackJack")) {
                p.updateInventory();
            }
        }
    }

    private static void pay(BlackJackTable table) {
        List<ItemStack> dealerDeck = table.getDealerDeck();
        int dealerValue = deckValue(dealerDeck);
        if (dealerValue > 21) {
            dealerValue = 0;
        }
        for (BlackJackPlayer p : table.getPlayers()) {
            int deckValue = deckValue(p.getPlayerDeck());
            if (deckValue == dealerValue && !(p.getPlayerDeck().size() == 2 && deckValue == 21)) {
                //tie
                BlackJackPlugin.getEconomy().depositPlayer(getOfflinePlayer(p.getPlayer()), p.getBet());

            } else if (deckValue >= dealerValue && deckValue <= 21) {
                //win
                BlackJackPlugin.getEconomy().depositPlayer(getOfflinePlayer(p.getPlayer()), p.getBet() * 2);

            }
        }
    }

    public static void kickPlayer(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        p.closeInventory(Reason.CANT_USE);
        BlackJackTable table = getTable(getPlayer(uuid));
        List<BlackJackPlayer> players = null;
        if (table != null) {
            players = table.getPlayers();
        }
        if (players != null) {
            players.removeIf(x -> x.getPlayer() == uuid);
        }
        if (players != null) {
            if (players.size() > 0) {
                table.setPlayers(players);
                updateTable(table);
            } else {
                tables.removeIf(x -> x.getId().equals(table.getId()));
            }
        }
    }

    private static List<ItemStack> getDealerCards(BlackJackTable table) {
        List<ItemStack> dealerDeck = new ArrayList<>();
        dealerDeck.add(getNextCard(table));
        return dealerDeck;
    }

    public static ItemStack getNextCard(BlackJackTable table) {
        List<ItemStack> cardStack = table.getCardStack();
        ItemStack card = cardStack.get(0);
        cardStack.remove(0);
        table.setCardStack(cardStack);
        return card;
    }

    private static int deckValue(List<ItemStack> cardDeck) {
        int deckValue = 0;
        boolean ace11 = false;
        if (!cardDeck.isEmpty()) {
            for (ItemStack card : cardDeck) {
                Component component = card.getItemMeta().itemName();
                String itemName = ((TextComponent) component).content();
                if (itemName.equalsIgnoreCase("Ess")) {
                    ace11 = true;
                }
                deckValue += getValue(card);
            }
            if (ace11 && deckValue <= 11) {
                deckValue += 10;
            }
        }
        return deckValue;
    }

    private static String deckName(List<ItemStack> cardDeck) {
        int deckValue = 0;
        String name = "";
        boolean ace11 = false;
        if (!cardDeck.isEmpty()) {
            for (ItemStack card : cardDeck) {
                Component component = card.getItemMeta().itemName();
                String itemName = ((TextComponent) component).content();

                if (itemName.equalsIgnoreCase("Ess")) {
                    ace11 = true;
                }
                deckValue += getValue(card);
            }
            if (deckValue > 21) {
                name = "Over 21";
            } else if (ace11 && deckValue <= 11) {
                name = deckValue + " / " + (deckValue + 10);
            } else {
                name = Integer.toString(deckValue);
            }
        }
        return name;

    }

    public static BlackJackPlayer getPlayer(final UUID uuid) {
        for (BlackJackTable table : tables) {
            for (BlackJackPlayer player : table.getPlayers()) {
                if (player.getPlayer().equals(uuid)) {
                    return player;
                }
            }
        }
        return null;
    }

    public static BlackJackTable getTable(BlackJackPlayer player) {
        for (BlackJackTable table : tables) {
            if (table.getPlayers().contains(player)) {
                return table;
            }
        }
        return null;
    }

    private static void housePlay(BlackJackTable table) {
        List<ItemStack> deck = table.getDealerDeck();
        while (deckValue(deck) < 17) {
            deck.add(getNextCard(table));
        }
        updateTable(table);
    }

    public static boolean isFinish(BlackJackTable table) {
        if (!table.getPlayers().isEmpty()) {
            List<BlackJackPlayer> players = table.getPlayers();
            for (BlackJackPlayer player : players) {
                if (!player.isFinish()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static int getValue(ItemStack is) {
        Component component = is.getItemMeta().itemName();
        String name = ((TextComponent) component).content();
        if (NumberUtil.isNumeric(name)) {
            return Integer.parseInt(name);
        } else {
            if (name.equals("Knekt") || name.equals("Dronning") || name.equals("Konge")) {
                return 10;
            } else if (name.equals("Ess")) {
                return 1;
            }
        }
        return 0;
    }

    public static void countDown(UUID tableId, final int time, boolean playing) {
        BlackJackTable table = tables.stream().filter(x -> x.getId().equals(tableId)).findFirst().orElse(null);
        if (table == null) return;
        Bukkit.getScheduler().runTaskLater(BlackJackPlugin.instance, () -> {
            if (!table.isRestarting()) {
                if (time >= 1) {
                    countDownCards(table, time);
                    countDown(tableId, time - 1, true);
                } else {
                    for (BlackJackPlayer player : table.getPlayers()) {
                        if (!player.isFinish()) {
                            Player p = Bukkit.getPlayer(player.getPlayer());
                            Component component;
                            if (player.getBet() > 0) {
                                component = Component.text("Du ble kastet ut av bordet grunnet inaktivitet. Din innsats ble tapt.", NamedTextColor.RED);
                            } else {
                                component = Component.text("Du ble kastet ut av bordet grunnet inaktivitet.", NamedTextColor.RED);
                            }
                            p.sendMessage(component);
                            kickPlayer(player.getPlayer());
                        }
                    }
                    updateTable(table);
                }
            } else if (!playing) {
                if (time > 0) {
                    countDownCards(table, time);
                    countDown(tableId, time - 1, false);
                } else {
                    start(table);
                }
            }
        }, 20L);
    }

    private static void countDownCards(BlackJackTable table, int i) {
        ItemStack card = null;
        if (i == 1) card = One();
        else if (i == 2) card = Two();
        else if (i == 3) card = Three();
        else if (i == 4) card = Four();
        else if (i == 5) card = Five();
        else if (i == 6) card = Six();
        else if (i == 7) card = Seven();
        else if (i == 8) card = Eight();
        else if (i == 9) card = Nine();
        else if (i > 9 && i % 2 == 0) card = Exclamation(true);
        else if (i > 9) card = Exclamation(false);

        for (BlackJackPlayer player : table.getPlayers()) {
            //Henter inventory
            Inventory inv = player.getTable();
            if (inv == null) {
                Component title = Component.text("BlackJack");
                inv = Bukkit.createInventory(null, 54, title);
            }
            inv.setItem(45, card);
        }
    }

    private static List<ItemStack> generateCardStack() {
        List<ItemStack> stack = new ArrayList<>();
        ItemStack card;
        for (int k = 0; k < 4 * decks; k++) {
            for (int i = 0; i < 13; i++) {
                if (i == 0) card = Ace();
                else if (i == 1) card = Two();
                else if (i == 2) card = Three();
                else if (i == 3) card = Four();
                else if (i == 4) card = Five();
                else if (i == 5) card = Six();
                else if (i == 6) card = Seven();
                else if (i == 7) card = Eight();
                else if (i == 8) card = Nine();
                else if (i == 9) card = Ten();
                else if (i == 10) card = Jack();
                else if (i == 11) card = Queen();
                else card = King();
                stack.add(card);
            }
        }
        Collections.shuffle(stack);
        return stack;
    }

    private static ItemStack Ace() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("Ess", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Two() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
        patterns.add(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("2", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Three() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("3", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Four() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM));
        patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("4", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Five() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(color, PatternType.STRIPE_DOWNRIGHT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("5", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Six() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
        patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("6", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Seven() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.DIAGONAL_RIGHT));
        patterns.add(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
        patterns.add(new Pattern(color, PatternType.SQUARE_BOTTOM_LEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("7", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Eight() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("8", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Nine() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM));
        patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("9", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Ten() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
        patterns.add(new Pattern(color, PatternType.STRIPE_CENTER));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("10", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Jack() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("Knekt", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Queen() {
        DyeColor color = randomColor();
        ItemStack banner;
        if (color == DyeColor.RED) {
            banner = ItemStack.of(Material.RED_BANNER, 1);
        } else {
            banner = ItemStack.of(Material.BLACK_BANNER, 1);
        }
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
        patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
        patterns.add(new Pattern(color, PatternType.SQUARE_BOTTOM_RIGHT));
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("Dronning", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack King() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_DOWNRIGHT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
        patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
        patterns.add(new Pattern(color, PatternType.STRIPE_DOWNLEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("Konge", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack One() {
        DyeColor color = randomColor();
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_CENTER));
        patterns.add(new Pattern(color, PatternType.SQUARE_TOP_LEFT));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
        patterns.add(new Pattern(color, PatternType.STRIPE_BOTTOM));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        meta.setPatterns(patterns);
        Component component = Component.text("1", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static ItemStack Exclamation(boolean red) {
        DyeColor color = DyeColor.YELLOW;
        if (red) {
            color = DyeColor.RED;
        }
        ItemStack banner = ItemStack.of(Material.WHITE_BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(color, PatternType.STRIPE_CENTER));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.BORDER));
        patterns.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM));
        meta.setPatterns(patterns);
        Component component = Component.text("Skynd deg og spill!", NamedTextColor.RED, TextDecoration.ITALIC);
        meta.itemName(component);
        banner.setItemMeta(meta);
        return banner;
    }

    private static DyeColor randomColor() {
        DyeColor random = DyeColor.RED;
        if (NumberUtil.getRandomNumber(0, 2) == 0) {
            random = DyeColor.BLACK;
        }
        return random;
    }

    private static ItemStack getPlayerHead(UUID uuid) {
        ItemStack skull = ItemStack.of(Material.PLAYER_HEAD, 1);
        OfflinePlayer p = getOfflinePlayer(uuid);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setOwningPlayer(p);
        skull.setItemMeta(skullMeta);
        return skull;
    }
}