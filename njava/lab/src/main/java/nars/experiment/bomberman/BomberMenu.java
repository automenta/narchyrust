package nars.experiment.bomberman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * File:         BomberMenu.java
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

/**
 * This class creates the main menu of the game.
 */
public class BomberMenu extends JPanel {
    /** main frame pointer */
    private final BomberMain main;
    /** image button objects */
    private final BomberImageButton[] imageButtons;
    /** current selection */
    private int selection = P2;

    /** background image object */
    private static Image backgroundImg;
    /** button images */
    private static final Image[] buttonImagesDown;
    private static final Image[] buttonImagesUp;
    /** rendering hints */
    private static Object hints;
    /** options enumeration */
    private static final int P2 = 0;
    private static final int P3 = 1;
    private static final int P4 = 2;
    private static final int CONTROL_SETUP = 3;
    private static final int EXIT = 4;

    static {
        /** if java runtime is Java 2 */
        if (Main.J2) {
            /** create the rendering hints for better graphics output */
            RenderingHints h = new RenderingHints(null);
            h.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            h.put(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            h.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            h.put(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            h.put(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            hints = h;
        }

        /** create the image objects */
        buttonImagesDown = new Image[5];
        buttonImagesUp = new Image[5];

        /** setup path names */
        Toolkit tk = Toolkit.getDefaultToolkit();

        String path = BomberMain.RP + "Images/BomberMenu/";
        try {
            /** load background image */
            String file = path + "Background.jpg";
            backgroundImg = tk.getImage(new File(file).getCanonicalPath());

            /** load each button image */
            for (int i = 0; i < 5; i++) {
                file = switch (i) {
                    case int j when j <= P4 -> path + (i + 2) + " Player Game";
                    case CONTROL_SETUP -> path + "Control Setup";
                    case EXIT -> path + "Exit";
                    default -> file;
                };
                buttonImagesDown[i] = tk.getImage(
                        new File(file + " Down.gif").getCanonicalPath());
                buttonImagesUp[i] = tk.getImage(
                        new File(file + " Up.gif").getCanonicalPath());
            }
        } catch (Exception e) {
            new ErrorDialog(e);
        }
    }

    /**
     * Constructs the menu.
     * @param main BomberMain object
     */
    public BomberMenu(BomberMain main) {
        this.main = main;
        /** set the menu dimensions */
        setPreferredSize(new Dimension(17 << BomberMain.shiftCount,
                17 << BomberMain.shiftCount));
        /** turn on double buffer */
        setDoubleBuffered(true);

        /** load the images */
        MediaTracker mt = new MediaTracker(this);

        try {
            int counter = 0;
            /** load the background image */
            mt.addImage(backgroundImg, counter++);
            /** load the button images */
            for (int i = 0; i < 5; i++) {
                mt.addImage(buttonImagesDown[i], counter++);
                mt.addImage(buttonImagesUp[i], counter++);
            }
            /** wait for images to finish loading */
            mt.waitForAll();
        } catch (Exception e) {
            new ErrorDialog(e);
        }
        /** create the button objects array */
        imageButtons = new BomberImageButton[5];
        for (int i = 0; i < 5; i++) {
            /** setup the images */
            Image[] images = {buttonImagesDown[i], buttonImagesUp[i]};
            /** create each object */
            imageButtons[i] = new BomberImageButton(this, images);
        }
        /** calculate distance between each button */
        int dy = buttonImagesDown[0].getHeight(this) / (32 / BomberMain.size * 2);
        /** setup the buttons' positions */
        for (int i = P2; i <= EXIT; i++)
            imageButtons[i].setInfo(0, (280 / (32 / BomberMain.size)) + (dy * i), i);
        /** set current selection to Player 2 */
        imageButtons[P2].setBevel(true);
    }

    /**
     * Handles key pressed events.
     * @param evt key event
     */
    public void keyPressed(KeyEvent evt) {
        /** store old selection */
        int newSelection = selection;
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_LEFT -> newSelection -= 1;
            case KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT -> newSelection += 1;
            case KeyEvent.VK_ENTER -> doCommand(selection);
        }
        /** if selection is new */
        if (selection != newSelection) {
            /** if new selection is less than 0 then set it to exit button */
            if (newSelection < 0) newSelection = EXIT;
            /** deselect old selection */
            imageButtons[selection].setBevel(false);
            /** set up selection */
            selection = newSelection;
            selection %= 5;
            /** select new selection */
            imageButtons[selection].setBevel(true);
        }
    }

    /**
     * Command handler.
     * @param command command
     */
    public void doCommand(int command) {
        /** create the dialog content */
        /** setup the dialog content */
        /** create the dialog */
        /** show the dialog */
        /** if user clicked on yes */
        switch (command) {
            case P2, P3, P4 -> main.newGame(selection + 2);
            case CONTROL_SETUP -> new BomberConfigDialog(main);
            case EXIT -> {
                JOptionPane pane =
                        new JOptionPane("Are you sure you want to exit Bomberman?");
                pane.setOptionType(JOptionPane.YES_NO_OPTION);
                pane.setMessageType(JOptionPane.WARNING_MESSAGE);
                JDialog dialog = pane.createDialog(this, "Exit Bomberman?");
                dialog.setResizable(false);
                dialog.show();
                Object selection = pane.getValue();
                if (selection != null && "0".equals(selection.toString()))
                /** terminate the program */
                    System.exit(0);
            }
        }
    }

    /**
     * Painting method.
     * @param g graphics handler
     */
    @Override
    public void paint(Graphics graphics) {
        Graphics g = graphics;
        /** if java runtime is Java 2 */
        if (Main.J2) {
            paint2D(graphics);
        }
        /** if java runtime isn't Java 2 */
        else {
            g.drawImage(backgroundImg, 0, 0, 17 <<
                    BomberMain.shiftCount, 17 << BomberMain.shiftCount, this);
        }
        for (int i = 0; i < 5; i++)
            if (imageButtons[i] != null)
                imageButtons[i].paint(g);
    }

    /**
     * Drawing method for Java 2's Graphics2D
     * @param graphics graphics handle
     */
    public void paint2D(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        /** set the rendering hints */
        g2.setRenderingHints((RenderingHints) hints);
        g2.drawImage(backgroundImg, 0, 0, 17 <<
                BomberMain.shiftCount, 17 << BomberMain.shiftCount, this);
    }
}
