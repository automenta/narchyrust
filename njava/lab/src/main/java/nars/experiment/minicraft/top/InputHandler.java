package nars.experiment.minicraft.top;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class InputHandler implements KeyListener {
    public final class Key {
        public int presses;
        public int absorbs;
        public boolean down;
        public boolean clicked;

        public Key() {
            keys.add(this);
        }

        public void pressIfUnpressed() {
            if (!clicked)
                press(true);
        }

        public void press(boolean pressed) {

            down = pressed;

            if (pressed) {
                presses++;
            }


        }

        public void tick() {
            if (absorbs < presses) {
                absorbs++;
                clicked = true;
            } else {
                clicked = false;
            }
        }
    }

    public final List<Key> keys = new ArrayList<>();

    public final Key up = new Key();
    public final Key down = new Key();
    public final Key left = new Key();
    public final Key right = new Key();
    public final Key attack = new Key();
    public final Key menu = new Key();

    public void releaseAll() {
        for (int i = 0; i < keys.size(); i++) {
            keys.get(i).down = false;
        }
    }

    public void tick() {
        for (int i = 0; i < keys.size(); i++) {
            keys.get(i).tick();
        }
    }

    public InputHandler(TopDownMinicraft game) {
        game.addKeyListener(this);
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        toggle(ke, true);
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        toggle(ke, false);
    }

    private void toggle(KeyEvent ke, boolean pressed) {
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD8) up.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD2) down.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD4) left.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD6) right.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_W) up.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_S) down.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_A) left.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_D) right.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_UP) up.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_DOWN) down.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_LEFT) left.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_RIGHT) right.press(pressed);

        if (ke.getKeyCode() == KeyEvent.VK_TAB) menu.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_ALT) menu.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_ALT_GRAPH) menu.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_SPACE) attack.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) attack.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD0) attack.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_INSERT) attack.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) menu.press(pressed);

        if (ke.getKeyCode() == KeyEvent.VK_X) menu.press(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_C) attack.press(pressed);
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }
}
