package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    //addional fields
    public volatile boolean PlayerStart;
    public volatile boolean PlayerIsAllowedToPressKey;
    public volatile  boolean TheDealerIsCurruntlyWaiting;
    public ConcurrentHashMap<Integer,ConcurrentHashMap<Integer,Integer>> playersTokens;
    public ConcurrentHashMap<Integer,ConcurrentHashMap<Player,Player>> SlotToToken;
    public LinkedBlockingQueue<Player> PlayersWithClaimingSet;


    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.PlayerStart = false;
        this.PlayerIsAllowedToPressKey = false;
        TheDealerIsCurruntlyWaiting = false;
        this.PlayersWithClaimingSet = new LinkedBlockingQueue<>();
        this.playersTokens = new ConcurrentHashMap<>();
        this.SlotToToken = new ConcurrentHashMap<>();
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card,slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        if(SlotToToken.containsKey(slot)){
            for (Player p:SlotToToken.get(slot).keySet())
                playersTokens.get(p.id).remove(slot);
            SlotToToken.put(slot,new ConcurrentHashMap<>());
        }
        slotToCard[slot]=null;
        env.ui.removeTokens(slot);
        env.ui.removeCard(slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(Player player, int slot) {
        // TODO implement
        //used another additonal function
    }

    public int placeTokenHelpFunc(Player player, int slot) {

        if (slotToCard[slot] == null || (playersTokens.containsKey(player.id) && playersTokens.get(player.id).size() == 3)) {
            return -1;
        }

        SlotToToken.computeIfAbsent(slot, k -> new ConcurrentHashMap<>()).put(player, player);

        playersTokens.computeIfAbsent(player.id, k -> new ConcurrentHashMap<>()).put(slot, slot);

        env.ui.placeToken(player.id, slot);

        return playersTokens.get(player.id).size();

    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */

    public boolean removeToken(int player, int slot) {

        boolean isDel = false;

        ConcurrentHashMap<Integer, Integer> playerTokens = playersTokens.get(player);

        ConcurrentHashMap<Player, Player> slotPlayers = SlotToToken.get(slot);

        if (playerTokens != null && playerTokens.remove(slot) != null && slotPlayers != null) {
            slotPlayers.remove(player);
            env.ui.removeToken(player, slot);
            isDel = true;
        }

        return isDel;
        
    }

    //addtional functions

    public boolean returnPlayerStart(){
        return this.PlayerStart;
    }
    public void setPlayerStart(boolean playerStart){
        this.PlayerStart=playerStart;
    }

    public boolean returnPlayerIsAllowedToPressKey(){
        return this.PlayerIsAllowedToPressKey;
    }
    public void setPlayerIsAllowedToPressKey(boolean playerIsAllowedToPressKey){
        this.PlayerIsAllowedToPressKey=playerIsAllowedToPressKey;
    }

    public boolean returnTheDealerIsCurruntlyWaiting(){
        return this.TheDealerIsCurruntlyWaiting;
    }
    public void setTheDealerIsCurruntlyWaitingt(boolean dealerIsCurruntlyWaiting){
        this.TheDealerIsCurruntlyWaiting=dealerIsCurruntlyWaiting;
    }

    public void addToPlayersWithClaimingSet(Player player){
        this.PlayersWithClaimingSet.add(player);
    }

    public boolean isEmptyClaiming(){
        return this.PlayersWithClaimingSet.isEmpty();
    }

    public Player removeClaiming(){
        return this.PlayersWithClaimingSet.remove();
    }
}