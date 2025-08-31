package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;
    private final List<Thread> PlayersThreadList;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    
    public boolean rest=false;
    private Timer time;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.PlayersThreadList=new LinkedList<>();
        terminate=false;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        env.ui.setCountdown(env.config.turnTimeoutMillis,false);

        if(env.config.turnTimeoutMillis<0){env.ui.dispose();}

        while (!shouldFinish() & env.config.turnTimeoutMillis>=0) {

            placeCardsOnTable();
            updateTimerDisplay(true);

            if (!table.returnPlayerStart()){

                //start the players threads
                creatPlayerThreads();
                table.setPlayerStart(true);
            }

            timerLoop();
            table.PlayerIsAllowedToPressKey =false;
            removeAllCardsFromTable();

            if (deck.size()==0)
                terminate();

        }

        terminate();

        for(int j=0; j<PlayersThreadList.size(); j++){
            PlayersThreadList.get(j).interrupt();
        }

        for(int l=0; l<PlayersThreadList.size(); l++)
            try{
                PlayersThreadList.get(l).join();
            }

            catch (InterruptedException e){}

        announceWinners();
        System.out.println("thread " + Thread.currentThread().getName() + " terminated.");
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");

    }

    public void creatPlayerThreads(){
        int i=0;
        while (i < players.length) {
            Thread playerThread = new Thread(players[i], "" + players[i].id);
            PlayersThreadList.add(playerThread);
            playerThread.start();
            i=i+1;
        }
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {

        if(env.config.hints){
            table.hints();
        }

        time=new Timer(env);
        Thread t=new Thread(time);
        t.start();
        table.PlayerIsAllowedToPressKey=true;

        while (!terminate && time.reshuffleTime>0) {
            SleepUntillHelpFunction(time.reshuffleTime);
            if( !table.PlayersWithClaimingSet.isEmpty()) {
                removeCardsFromTable();
                placeCardsOnTable();
            }
        }

    }



    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate=true;
        int i=players.length-1;
        while(i>=0){
            players[i].terminate();
            i=i-1;
        }

    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */

    private void removeCardsFromTable() {
        // TODO implement
        Player ClaimingPlayer = table.removeClaiming();

        if (ClaimingPlayer != null) {
            int[] cardSet = extractCardSetFromTable(ClaimingPlayer); //if we have a player with a set, get the set

            if (env.util.testSet(cardSet)) { //check if the set is legal
                handleValidSet(ClaimingPlayer, cardSet); //if yes, handle this case
            } 
            else {
                handleInvalidSet(ClaimingPlayer); //is not, handle this case
            }
        }

    }

    //additional method
    private int[] extractCardSetFromTable(Player player) {
        int[] cardSet = new int[3];
        int i = 0;

        for (Integer slot : table.playersTokens.get(player.id).keySet())
            cardSet[i++] = table.slotToCard[slot];

        return cardSet;
    }

    //additional method
    private void handleValidSet(Player player, int[] cardSet) {
        player.isPoint = true;

        for (Integer slot : table.playersTokens.get(player.id).keySet())
            table.removeCard(slot);

        table.playersTokens.get(player.id).clear();
        env.ui.setCountdown(env.config.turnTimeoutMillis, false);
        time.reshuffleTime = env.config.turnTimeoutMillis;
        notifyPlayerHelpLocker(player);
    }

    //additional method
    private void handleInvalidSet(Player player) {
        player.isPenalty = true;
        notifyPlayerHelpLocker(player);
    }

    //additional method
    private void notifyPlayerHelpLocker(Player player) {
        synchronized (player.HelpLocker) {
            player.HelpLocker.notifyAll();
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */

    public void placeCardsOnTable() {
        // TODO implement
        suffleList(deck);
        List<Integer> EmptySlots = findEmptySlots();
        suffleList(EmptySlots); //place the cards randomly

        for (int i = 0; i < Math.min(deck.size(), EmptySlots.size()); i++) {
            int card = deck.remove(0);
            int emptySlot = EmptySlots.get(i);
            table.placeCard(card, emptySlot);
        }

    }

    //additional method
    private List<Integer> findEmptySlots() {
        List<Integer> EmptySlots = new LinkedList<>();

        for (int i = 0; i < env.config.tableSize; i++) {

            if (table.slotToCard[i] == null) {
                EmptySlots.add(i);
            }

        }

        return EmptySlots;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */

    private void sleepUntillWokenOrTimeout() {
        // TODO implement
        //used another help function
    }

    //addtional function
    private void SleepUntillHelpFunction(long time) {
        try {
            synchronized (table.PlayersWithClaimingSet) {
                table.setTheDealerIsCurruntlyWaitingt(true);
                table.PlayersWithClaimingSet.wait(time);
                table.setTheDealerIsCurruntlyWaitingt(false);
            }
        }catch(InterruptedException e){}


    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(reset){env.ui.setCountdown(env.config.turnTimeoutMillis,false);}
    }

    /**
     * Returns all the cards from the table to the deck.
     */

    public void removeAllCardsFromTable() {
        List<Integer> filledSlots = findFilledSlots();
        shuffleAndReturnToDeck(filledSlots);
        removeCardsFromTable(filledSlots);
    }

    //additional method
    private List<Integer> findFilledSlots() {
        List<Integer> filledSlots = new LinkedList<>();

        for (int i = 0; i < env.config.tableSize; i++) {

            if (table.slotToCard[i] != null) {
                filledSlots.add(i);
            }

        }

        return filledSlots;
    }

    //additional method
    private void shuffleAndReturnToDeck(List<Integer> filledSlots) {
        suffleList(filledSlots);
        for (int slot :filledSlots) {
            deck.add(table.slotToCard[slot]);
        }
    }

    //additional method
    private void removeCardsFromTable(List<Integer> filledSlots) {
        for (int slot:filledSlots) {
            table.removeCard(slot);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */

    private void announceWinners() {
        int maxScore = findMaxScore();
        int numOfWinners = countWinners(maxScore);
        int[] winners = findWinnerIds(maxScore, numOfWinners);
        env.ui.announceWinner(winners);
    }

    //additional method
    private int findMaxScore() {
        int maxScore = 0;

        for (Player player : players) {
            maxScore = Math.max(maxScore, player.score());
        }

        return maxScore;
    }

    //additional method
    private int countWinners(int maxScore) {
        int numOfWinners = 0;

        for (Player player : players) {

            if (player.score() == maxScore) {
                numOfWinners++;
            }

        }

        return numOfWinners;
    }

    //additional method
    private int[] findWinnerIds(int maxScore, int numOfWinners) {
        int[] winners = new int[numOfWinners];
        int i = 0;

        for (Player player: players) {

            if (player.score() == maxScore) {
                winners[i++] = player.id;
            }

        }

        return winners;
    }

    //additional method
    public void suffleList(List<Integer> deck) {
        Random random = new Random();

        for (int i = deck.size() - 1; i > 0; i=i-1) {

            int index = random.nextInt(i + 1);
            Integer temp = deck.get(index);
            deck.set(index, deck.get(i));
            deck.set(i, temp);
            
        }
    }


}