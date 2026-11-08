package io.jbnu.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Catch coffee, avoid trash, and keep playing until the full thirty seconds have elapsed. */
public final class CafeMinigame extends VacationMinigame {
    public static final int TARGET = 100;
    public static final float TRAY_WIDTH = 110, TRAY_Y = 70, TRAY_HEIGHT = 18;
    public static final float DROP_WIDTH = 24, DROP_HEIGHT = 28;
    private static final float TRAY_SPEED = 500;

    public static final class Drop {
        private final float x, speed;
        private final boolean coffee;
        private float y = 440; // Keep newly spawned cups below the HUD and rule caption.
        private Drop(float x, float speed, boolean coffee) { this.x = x; this.speed = speed; this.coffee = coffee; }
        public float getX() { return x; }
        public float getY() { return y; }
        public boolean isCoffee() { return coffee; }
    }

    private final Random random;
    private final List<Drop> drops = new ArrayList<>();
    private final List<Drop> visibleDrops = Collections.unmodifiableList(drops);
    private float trayX = 345;
    private double untilSpawn = .35;
    private int score, spawned, removed, coffeeCaught, trashCaught, missedCoffee;

    public CafeMinigame() { this(new Random()); }
    public CafeMinigame(Random random) { super(VacationEventType.CAFE,30); this.random = random; }

    @Override protected void play(float delta, MinigameInput input) {
        float direction = (input.isRightHeld() ? 1 : 0) - (input.isLeftHeld() ? 1 : 0);
        trayX = Math.max(40,Math.min(760-TRAY_WIDTH,trayX+direction*TRAY_SPEED*delta));
        untilSpawn -= delta;
        while (untilSpawn <= 0) {
            boolean coffee = spawned % 4 != 3;
            drops.add(new Drop(80+random.nextFloat()*616,coffee ? 155 : 175,coffee));
            spawned++;
            untilSpawn += .72;
        }
        for (int i = drops.size()-1; i >= 0; i--) {
            Drop drop = drops.get(i);
            float previousY = drop.y;
            drop.y -= drop.speed*delta;
            boolean crossesTray = previousY >= TRAY_Y+TRAY_HEIGHT && drop.y <= TRAY_Y+TRAY_HEIGHT;
            boolean overlapsTray = drop.y < TRAY_Y+TRAY_HEIGHT && drop.y+DROP_HEIGHT > TRAY_Y;
            if ((crossesTray || overlapsTray) && drop.x+DROP_WIDTH > trayX && drop.x < trayX+TRAY_WIDTH) {
                if (drop.coffee) { score += 10; coffeeCaught++; }
                else { score = Math.max(0,score-10); trashCaught++; }
                drops.remove(i); removed++;
            } else if (drop.y+DROP_HEIGHT < 0) {
                if (drop.coffee) missedCoffee++;
                drops.remove(i); removed++;
            }
        }
    }

    @Override protected boolean successAtTimeout() { return score >= TARGET; }
    public float getTrayX() { return trayX; }
    public List<Drop> getDrops() { return visibleDrops; }
    public int getScore() { return score; }
    public int getSpawnedCount() { return spawned; }
    public int getRemovedCount() { return removed; }
    public int getCoffeeCaught() { return coffeeCaught; }
    public int getTrashCaught() { return trashCaught; }
    public int getMissedCoffee() { return missedCoffee; }
}
