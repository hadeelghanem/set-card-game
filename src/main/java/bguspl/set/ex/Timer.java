package bguspl.set.ex;

import bguspl.set.Env;

public class Timer implements Runnable {
    private Env env;
    public long reshuffleTime;
    private boolean rest = false;

    public Timer(Env env) {
        this.env = env;
        this.reshuffleTime = env.config.turnTimeoutMillis;
    }

    @Override
    public void run() {
        try {
            while (reshuffleTime >=0) {
                updateCountdown();
                Thread.sleep(1000);
                reshuffleTime -= 1000;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateCountdown() {
        boolean turnRed = reshuffleTime <= env.config.turnTimeoutWarningMillis; // Set turnRed when remaining time is 3 seconds or less
        env.ui.setCountdown(reshuffleTime, turnRed);
    }
}