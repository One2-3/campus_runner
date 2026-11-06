package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.util.Arrays;

/** One immutable keyboard snapshot per update. Held movement and fresh action edges are separate. */
public final class MinigameInput {
    public static final MinigameInput NONE = new MinigameInput(false, false);
    private final boolean left, right;
    private final int[] pressed;

    public MinigameInput(boolean left, boolean right, int... pressed) {
        this.left = left;
        this.right = right;
        this.pressed = pressed.clone();
    }

    public boolean isLeftHeld() { return left; }
    public boolean isRightHeld() { return right; }
    public boolean isPressed(int key) {
        for (int value : pressed) if (value == key) return true;
        return false;
    }

    public static MinigameInput read() {
        int[] keys = {Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.UP, Input.Keys.DOWN, Input.Keys.SPACE, Input.Keys.ENTER};
        int count = 0;
        int[] edges = new int[keys.length];
        for (int key : keys) if (Gdx.input.isKeyJustPressed(key)) edges[count++] = key;
        return new MinigameInput(Gdx.input.isKeyPressed(Input.Keys.LEFT), Gdx.input.isKeyPressed(Input.Keys.RIGHT),
            Arrays.copyOf(edges,count));
    }
}
