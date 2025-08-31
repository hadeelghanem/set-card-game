package bguspl.set.ex;

import bguspl.set.Env;


import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    public final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;
    public volatile boolean thePlayerIsCurrWaiting;
//    public AtomicBoolean dealersResponse;


    /**
     * The current score of the player.
     */
    private int score;


    //addtional feilds

    public volatile  boolean thePlayerIsAsleep;

    private LinkedBlockingQueue<Object> keys;

    private volatile  boolean checkTheAns;

    public volatile boolean isPenalty;

    public volatile boolean isPoint;

    public Object HelpLocker;


    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.terminate=false;
        this.thePlayerIsCurrWaiting =true;
        thePlayerIsAsleep = false;
        keys = new LinkedBlockingQueue<>();
        HelpLocker = new Object();
        checkTheAns=false;
        isPoint=false;
        isPenalty=false;


    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {

            if (checkTheAns) {
                checkTheAns = false;

                if (isPoint) {
                    if (env.config.pointFreezeMillis > 0)
                        thePlayerIsAsleep = true;
                    point();
                    isPoint = false;
                    keys.clear();
                } 

                else if (isPenalty) {

                    if (env.config.penaltyFreezeMillis > 0)
                        thePlayerIsAsleep = true;
                    penalty();
                    isPenalty = false;

                }

            }

            thePlayerIsAsleep = false;
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        System.out.println("thread " + Thread.currentThread().getName() + " terminated.");
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");

            while (!terminate) {

                LinkedList<Integer> fullSlots=new LinkedList<>();

                for (int i=0;i<env.config.tableSize;i++)
                    if(table.slotToCard[i]!=null)
                        fullSlots.add(i);

                int[] Set =new int[3];
                shuffleList(fullSlots);
                int firstSlot=0;
                int secondSlot=0;
                int thirdSlot=0;

                if(fullSlots.size() > 0) {

                    firstSlot = fullSlots.get(0);
                    if(table.slotToCard[firstSlot] != null)
                        Set[0] = table.slotToCard[firstSlot];

                }

                if(fullSlots.size() > 1) {
                    secondSlot = fullSlots.get(1);
                    if(table.slotToCard[secondSlot] != null)
                        Set[1] = table.slotToCard[secondSlot];
                }

                if(fullSlots.size() > 2) {
                    thirdSlot = fullSlots.get(2);
                    if(table.slotToCard[thirdSlot] != null)
                        Set[2] = table.slotToCard[thirdSlot];
                }

                if(fullSlots.size()!=0) {
                    pressHelp(firstSlot, secondSlot, thirdSlot);
                    if(!env.util.testSet(Set)){
                        pressHelp(firstSlot, secondSlot, thirdSlot);
                    }
                }

            }
            System.out.println("thread " + Thread.currentThread().getName() + " terminated.");
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    //addtional method
    public void shuffleList(List<Integer> deck) {
        Random random = new Random();
        for (int i = deck.size() - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            Integer temp = deck.get(index);
            deck.set(index, deck.get(i));
            deck.set(i, temp);
        }
    }

    //addtional method
    public void pressHelp(int firstSlot, int secondSlot, int thirdSlot){
        keyPressed(firstSlot);
        keyPressed(secondSlot);
        keyPressed(thirdSlot);
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate=true;
        try {
            playerThread.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */

    public void keyPressed(int slot){
        // TODO implement
        //we used help function
        keyPressedHelpFunction(slot);
    }

    public boolean keyPressedHelpFunction(int slot) {
        if ( thePlayerIsAsleep || !table.returnPlayerStart() || !table.returnPlayerIsAllowedToPressKey() ||  !table.returnTheDealerIsCurruntlyWaiting())
            return false;

        synchronized (table) {
            keys.add(new Object());

            if (!table.returnTheDealerIsCurruntlyWaiting()) {
                return false;
            }

            if (table.playersTokens.containsKey(id) && table.playersTokens.get(id).containsKey(slot)) {
                removeANDpoll(id, slot, keys);
            } 
            
            else {
                int placedToken = table.placeTokenHelpFunc(this, slot);

                if (placedToken == 3) {
                    table.PlayersWithClaimingSet.add(this);
                    synchronized (HelpLocker) {
                        synchronized (table.PlayersWithClaimingSet) {
                            table.PlayersWithClaimingSet.notifyAll();
                        }
                        Updates();
                    }
                }

                keys.poll();

            }
        }
        return true;
    }

    //addtional method
    public void removeANDpoll(int id, int slot,LinkedBlockingQueue<Object> keys ){
        table.removeToken(id, slot);
        keys.poll();
    }

    //addtional methods
    public void Updates(){
        try {
            thePlayerIsCurrWaiting = false;
            HelpLocker.wait();
            checkTheAns = true;
            thePlayerIsCurrWaiting = true;
        } catch (InterruptedException e) {
        }
    }


    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */

    public void point() {
        // TODO implement
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, score+=1);
        if(env.config.pointFreezeMillis>0){
            long pointdelay=env.config.pointFreezeMillis;
            loopPoint(pointdelay);
        }

    }

    //addtional function
    public void loopPoint(long pointdelay) {
        try {
            for (; pointdelay > 0; pointdelay = pointdelay -1000) {
                env.ui.setFreeze(id, pointdelay);
                Thread.sleep(1000, 0);
            }
            env.ui.setFreeze(id, 0);
        } catch (InterruptedException e) {}
    }
   

    /**
     * Penalize a player and perform other related actions.
     */



    public void penalty() {
        // TODO implement
        if(env.config.penaltyFreezeMillis>0){
            long penaltydelay=env.config.penaltyFreezeMillis;
            loopPenalty(penaltydelay);
        }

    }

    //addtional function
    public void loopPenalty(long penaltydelay) {
        try {
            for (; penaltydelay > 0;  penaltydelay = penaltydelay -1000) {
                env.ui.setFreeze(id, penaltydelay);
                Thread.sleep(1000, 0);
            }
            env.ui.setFreeze(id, 0);
        } catch (InterruptedException e) {}
    }
    

    public int score() {
        return score;
    }

    /**just for tests

     */

    //additional method
    public void fillTheList(LinkedList list){

        for (int i = 0; i < env.config.tableSize; i++) {

            if (table.slotToCard[i] != null) {
                list.add(i);
            }

        }

    }

    //addtional function
    public void AddandPress(int[] set, int slot){

        if (table.slotToCard[slot] != null) {
            set[0] = table.slotToCard[slot];
        }

        keyPressed(slot);
        
    }
}