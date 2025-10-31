package nars.experiment.bomberman;/*
 * Main.java
 * Created on February 17, 2001, 12:51 AM
 */

import javax.swing.*;

/**
 * This class is the starting proint of the program.
 *
 * @author Sammy Leong
 * @version 1.0
 */
public class Main {
    public static BomberMain bomberMain;

    /**
     * relative path
     */
    public static final String RP = "./";
    /**
     * flag: whether current machine's java runtime is version 2 or not
     */
    public static boolean J2;


    /**
     * Starts Bomberman
     */
    public static void startBomberman() {
        bomberMain = new BomberMain();
    }

    /**
     * Starts the program by creating an instance of MainFrame.
     */
    public static void main(String[] args) {
        boolean bombermanMode = false;
        boolean badArg = false;
        /** default look and feel: metal */
        int lookAndFeel = 1;
        /** check supplied parameters (if any) */
        for (int i = 0; i < args.length; i++) {
            /** if "bomberman" parameter is supplied */
            if ("Bomberman".equals(args[i]) || "bomberman".equals(args[i]))
                bombermanMode = true;
            /** if look and feel parameter is supplied */
            if (args[i].startsWith("-l")) {
                switch (args[i].substring(2)) {
                    case "System" -> lookAndFeel = 0;
                    case "Metal" -> lookAndFeel = 1;
                    case "Windows" -> lookAndFeel = 2;
                    case "Mac" -> lookAndFeel = 3;
                    case "Motif" -> lookAndFeel = 4;
                }
            }
        }
        /** if look and feel isn't default: metal */
        if (lookAndFeel != 1) {
            try {
                /**
                 * available look and feels:
                 * =========================
                 * "javax.swing.plaf.metal.MetalLookAndFeel"
                 * "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
                 * "com.sun.java.swing.plaf.motif.MotifLookAndFeel"
                 * "javax.swing.plaf.mac.MacLookAndFeel"
                 */
                String laf = switch (lookAndFeel) {
                    case 0 -> UIManager.getSystemLookAndFeelClassName();
                    case 1 -> "javax.swing.plaf.metal.MetalLookAndFeel";
                    case 2 -> "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                    case 3 -> "javax.swing.plaf.mac.MacLookAndFeel";
                    case 4 -> "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
                    default -> "javax.swing.plaf.metal.MetalLookAndFeel";
                };
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) {
                new ErrorDialog(e);
            }
        }

        startBomberman();
    }
}