/*
 * Key.java
 * Copyright (C) 2003
 *
 * $Id: Key.java,v 1.12 2005-12-18 22:10:12 cawe Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.client;

import jake2.Globals;
import jake2.game.Cmd;
import jake2.qcommon.Cbuf;
import jake2.qcommon.Com;
import jake2.qcommon.Cvar;
import jake2.qcommon.xcommand_t;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Key
 */
public class Key extends Globals {


    public static final int K_TAB = 9;
    public static final int K_ENTER = 13;
    public static final int K_ESCAPE = 27;
    public static final int K_SPACE = 32;


    public static final int K_BACKSPACE = 127;
    public static final int K_UPARROW = 128;
    public static final int K_DOWNARROW = 129;
    public static final int K_LEFTARROW = 130;
    public static final int K_RIGHTARROW = 131;

    public static final int K_ALT = 132;
    public static final int K_CTRL = 133;
    public static final int K_SHIFT = 134;
    public static final int K_F1 = 135;
    public static final int K_F2 = 136;
    public static final int K_F3 = 137;
    public static final int K_F4 = 138;
    public static final int K_F5 = 139;
    public static final int K_F6 = 140;
    public static final int K_F7 = 141;
    public static final int K_F8 = 142;
    public static final int K_F9 = 143;
    public static final int K_F10 = 144;
    public static final int K_F11 = 145;
    public static final int K_F12 = 146;
    public static final int K_INS = 147;
    public static final int K_DEL = 148;
    public static final int K_PGDN = 149;
    public static final int K_PGUP = 150;
    public static final int K_HOME = 151;
    public static final int K_END = 152;
    public static final int K_PAUSE = 255;
    public static final int K_MOUSE1 = 200;
    public static final int K_MOUSE2 = 201;
    public static final int K_MOUSE3 = 202;
    public static final int K_MWHEELDOWN = 239;
    public static final int K_MWHEELUP = 240;
    static final int K_KP_HOME = 160;
    static final int K_KP_UPARROW = 161;
    static final int K_KP_PGUP = 162;
    static final int K_KP_LEFTARROW = 163;
    static final int K_KP_5 = 164;
    static final int K_KP_RIGHTARROW = 165;
    static final int K_KP_END = 166;
    static final int K_KP_DOWNARROW = 167;
    static final int K_KP_PGDN = 168;
    static final int K_KP_ENTER = 169;
    static final int K_KP_INS = 170;
    static final int K_KP_DEL = 171;
    static final int K_KP_SLASH = 172;
    static final int K_KP_MINUS = 173;
    static final int K_KP_PLUS = 174;
    static final int K_JOY1 = 203;
    static final int K_JOY2 = 204;
    static final int K_JOY3 = 205;
    static final int K_JOY4 = 206;
    private static final int[] key_repeats = new int[256];
    private static final boolean[] menubound = new boolean[256];
    private static final boolean[] consolekeys = new boolean[256];
    private static final String[] keynames = new String[256];
    private static final xcommand_t Bind_f = new xcommand_t() {
        @Override
        public void execute() {
            Key_Bind_f();
        }
    };
    private static final xcommand_t Unbind_f = new xcommand_t() {
        @Override
        public void execute() {
            Key_Unbind_f();
        }
    };
    private static final xcommand_t Unbindall_f = new xcommand_t() {
        @Override
        public void execute() {
            Key_Unbindall_f();
        }
    };
    private static final xcommand_t Bindlist_f = new xcommand_t() {
        @Override
        public void execute() {
            Key_Bindlist_f();
        }
    };
    static int anykeydown;
    private static int key_waiting;
    private static int history_line;

    static {
        keynames[K_TAB] = "TAB";
        keynames[K_ENTER] = "ENTER";
        keynames[K_ESCAPE] = "ESCAPE";
        keynames[K_SPACE] = "SPACE";
        keynames[K_BACKSPACE] = "BACKSPACE";
        keynames[K_UPARROW] = "UPARROW";
        keynames[K_DOWNARROW] = "DOWNARROW";
        keynames[K_LEFTARROW] = "LEFTARROW";
        keynames[K_RIGHTARROW] = "RIGHTARROW";
        keynames[K_ALT] = "ALT";
        keynames[K_CTRL] = "CTRL";
        keynames[K_SHIFT] = "SHIFT";

        keynames[K_F1] = "F1";
        keynames[K_F2] = "F2";
        keynames[K_F3] = "F3";
        keynames[K_F4] = "F4";
        keynames[K_F5] = "F5";
        keynames[K_F6] = "F6";
        keynames[K_F7] = "F7";
        keynames[K_F8] = "F8";
        keynames[K_F9] = "F9";
        keynames[K_F10] = "F10";
        keynames[K_F11] = "F11";
        keynames[K_F12] = "F12";

        keynames[K_INS] = "INS";
        keynames[K_DEL] = "DEL";
        keynames[K_PGDN] = "PGDN";
        keynames[K_PGUP] = "PGUP";
        keynames[K_HOME] = "HOME";
        keynames[K_END] = "END";

        keynames[K_MOUSE1] = "MOUSE1";
        keynames[K_MOUSE2] = "MOUSE2";
        keynames[K_MOUSE3] = "MOUSE3";


        keynames[K_KP_HOME] = "KP_HOME";
        keynames[K_KP_UPARROW] = "KP_UPARROW";
        keynames[K_KP_PGUP] = "KP_PGUP";
        keynames[K_KP_LEFTARROW] = "KP_LEFTARROW";
        keynames[K_KP_5] = "KP_5";
        keynames[K_KP_RIGHTARROW] = "KP_RIGHTARROW";
        keynames[K_KP_END] = "KP_END";
        keynames[K_KP_DOWNARROW] = "KP_DOWNARROW";
        keynames[K_KP_PGDN] = "KP_PGDN";
        keynames[K_KP_ENTER] = "KP_ENTER";
        keynames[K_KP_INS] = "KP_INS";
        keynames[K_KP_DEL] = "KP_DEL";
        keynames[K_KP_SLASH] = "KP_SLASH";

        keynames[K_KP_PLUS] = "KP_PLUS";
        keynames[K_KP_MINUS] = "KP_MINUS";

        keynames[K_MWHEELUP] = "MWHEELUP";
        keynames[K_MWHEELDOWN] = "MWHEELDOWN";

        keynames[K_PAUSE] = "PAUSE";
        keynames[';'] = "SEMICOLON";

        keynames[0] = "NULL";
    }

    /**
     *
     */
    public static void Init() {
        for (int i = 0; i < 32; i++) {
            key_lines[i][0] = ']';
            key_lines[i][1] = 0;
        }
        key_linepos = 1;


        for (int i = 32; i < 128; i++)
            consolekeys[i] = true;
        consolekeys[K_ENTER] = true;
        consolekeys[K_KP_ENTER] = true;
        consolekeys[K_TAB] = true;
        consolekeys[K_LEFTARROW] = true;
        consolekeys[K_KP_LEFTARROW] = true;
        consolekeys[K_RIGHTARROW] = true;
        consolekeys[K_KP_RIGHTARROW] = true;
        consolekeys[K_UPARROW] = true;
        consolekeys[K_KP_UPARROW] = true;
        consolekeys[K_DOWNARROW] = true;
        consolekeys[K_KP_DOWNARROW] = true;
        consolekeys[K_BACKSPACE] = true;
        consolekeys[K_HOME] = true;
        consolekeys[K_KP_HOME] = true;
        consolekeys[K_END] = true;
        consolekeys[K_KP_END] = true;
        consolekeys[K_PGUP] = true;
        consolekeys[K_KP_PGUP] = true;
        consolekeys[K_PGDN] = true;
        consolekeys[K_KP_PGDN] = true;
        consolekeys[K_SHIFT] = true;
        consolekeys[K_INS] = true;
        consolekeys[K_KP_INS] = true;
        consolekeys[K_KP_DEL] = true;
        consolekeys[K_KP_SLASH] = true;
        consolekeys[K_KP_PLUS] = true;
        consolekeys[K_KP_MINUS] = true;
        consolekeys[K_KP_5] = true;

        consolekeys['`'] = false;
        consolekeys['~'] = false;


        menubound[K_ESCAPE] = true;
        for (int i = 0; i < 12; i++)
            menubound[K_F1 + i] = true;


        Cmd.AddCommand("bind", Key.Bind_f);
        Cmd.AddCommand("unbind", Key.Unbind_f);
        Cmd.AddCommand("unbindall", Key.Unbindall_f);
        Cmd.AddCommand("bindlist", Key.Bindlist_f);
    }

    public static void ClearTyping() {
        key_lines[edit_line][1] = 0;
        key_linepos = 1;
    }

    /**
     * Called by the system between frames for both key up and key down events.
     */
    public static void Event(int key, boolean down, int time) {


        if (key_waiting == -1) {
            if (down)
                key_waiting = key;
            return;
        }


        if (down) {
            key_repeats[key]++;
            if (key_repeats[key] > 1
                    && cls.key_dest == key_game
                    && cls.state != ca_disconnected)
                return;

            if (key >= 200 && keybindings[key] == null)
                Com.Printf(KeynumToString(key) + " is unbound, hit F4 to setAt.\n");
        } else {
            key_repeats[key] = 0;
        }

        boolean shift_down;
        if (key == K_SHIFT)
            shift_down = down;


        if (key == '`' || key == '~') {
            if (!down)
                return;

            Console.ToggleConsole_f.execute();
            return;
        }


        if (cl.attractloop && cls.key_dest != key_menu && !(key >= K_F1 && key <= K_F12))
            key = K_ESCAPE;


        if (key == K_ESCAPE) {
            if (!down)
                return;

            if (cl.frame.playerstate.stats[STAT_LAYOUTS] != 0 && cls.key_dest == key_game) {

                Cbuf.AddText("cmd putaway\n");
                return;
            }
            switch (cls.key_dest) {
                case key_message -> Message(K_ESCAPE);
                case key_menu -> Menu.Keydown(K_ESCAPE);
                case key_game, key_console -> Menu.Menu_Main_f();
                default -> Com.Error(ERR_FATAL, "Bad cls.key_dest");
            }
            return;
        }


        keydown[key] = down;
        if (down) {
            if (key_repeats[key] == 1)
                anykeydown++;
        } else {
            anykeydown--;
            if (anykeydown < 0)
                anykeydown = 0;
        }


        String cmd;
        String kb;
        if (!down) {
            kb = keybindings[key];
            if (kb != null && !kb.isEmpty() && kb.charAt(0) == '+') {
                cmd = '-' + kb.substring(1) + ' ' + key + ' ' + time + '\n';
                Cbuf.AddText(cmd);
            }


            return;
        }


        if ((cls.key_dest == key_menu && menubound[key])
                || (cls.key_dest == key_console && !consolekeys[key])
                || (cls.key_dest == key_game && (cls.state == ca_active || !consolekeys[key]))) {
            kb = keybindings[key];
            if (kb != null) {
                if (!kb.isEmpty() && kb.charAt(0) == '+') {

                    cmd = kb + ' ' + key + ' ' + time + '\n';
                    Cbuf.AddText(cmd);
                } else {
                    Cbuf.AddText(kb + '\n');
                }
            }
            return;
        }

        if (!down)
            return;


        switch (cls.key_dest) {
            case key_message -> Message(key);
            case key_menu -> Menu.Keydown(key);
            case key_game, key_console -> Console(key);
            default -> Com.Error(ERR_FATAL, "Bad cls.key_dest");
        }
    }

    /**
     * Returns a string (either a single ascii char, or a K_* name) for the
     * given keynum.
     */
    public static String KeynumToString(int keynum) {
        if (keynum < 0 || keynum > 255)
            return "<KEY NOT FOUND>";
        if (keynum > 32 && keynum < 127)
            return Character.toString((char) keynum);

        if (keynames[keynum] != null)
            return keynames[keynum];

        return "<UNKNOWN KEYNUM>";
    }

    /**
     * Returns a key number to be used to index keybindings[] by looking at
     * the given string. Single ascii characters return themselves, while
     * the K_* names are matched up.
     */
    private static int StringToKeynum(String str) {

        if (str == null)
            return -1;

        if (str.length() == 1)
            return str.charAt(0);

        int bound = keynames.length;
        return IntStream.range(0, bound).filter(i -> str.equalsIgnoreCase(keynames[i])).findFirst().orElse(-1);
    }

    private static void Message(int key) {

        switch (key) {
            case K_ENTER, K_KP_ENTER -> {
                Cbuf.AddText(chat_team ? "say_team \"" : "say \"");

                Cbuf.AddText(chat_buffer);
                Cbuf.AddText("\"\n");

                cls.key_dest = key_game;
                chat_buffer = "";
            }
            case K_ESCAPE -> {
                cls.key_dest = key_game;
                chat_buffer = "";
            }
            case int i when i > 127 -> {
                return;
            }
            case K_BACKSPACE -> {
                chat_buffer = chat_buffer.length() > 2 ? chat_buffer.substring(0, chat_buffer.length() - 2) : "";
            }
            default -> {
                if (chat_buffer.length() <= MAXCMDLINE)
                    chat_buffer += (char) key;
            }
        }
    }

    /**
     * Interactive line editing and console scrollback.
     */
    private static void Console(int key) {

        switch (key) {
            case K_KP_SLASH -> key = '/';
            case K_KP_MINUS -> key = '-';
            case K_KP_PLUS -> key = '+';
            case K_KP_HOME -> key = '7';
            case K_KP_UPARROW -> key = '8';
            case K_KP_PGUP -> key = '9';
            case K_KP_LEFTARROW -> key = '4';
            case K_KP_5 -> key = '5';
            case K_KP_RIGHTARROW -> key = '6';
            case K_KP_END -> key = '1';
            case K_KP_DOWNARROW -> key = '2';
            case K_KP_PGDN -> key = '3';
            case K_KP_INS -> key = '0';
            case K_KP_DEL -> key = '.';
        }

        boolean finished = false;
        if (key == 'l') {
            if (keydown[K_CTRL]) {
                Cbuf.AddText("clear\n");
                finished = true;
            }
        }
        if (!finished) {
            if (key == K_ENTER || key == K_KP_ENTER) {

                if (key_lines[edit_line][1] == '\\' || key_lines[edit_line][1] == '/')
                    Cbuf.AddText(
                            new String(key_lines[edit_line], 2, Lib.strlen(key_lines[edit_line]) - 2));
                else
                    Cbuf.AddText(
                            new String(key_lines[edit_line], 1, Lib.strlen(key_lines[edit_line]) - 1));


                Cbuf.AddText("\n");

                Com.Printf(new String(key_lines[edit_line], 0, Lib.strlen(key_lines[edit_line])) + '\n');
                edit_line = (edit_line + 1) & 31;
                history_line = edit_line;

                key_lines[edit_line][0] = ']';
                key_linepos = 1;
                if (cls.state == ca_disconnected)
                    SCR.UpdateScreen();
            } else if (key == K_TAB) {

                CompleteCommand();
            } else if ((key == K_BACKSPACE) || (key == K_LEFTARROW) || (key == K_KP_LEFTARROW) || ((key == 'h') && (keydown[K_CTRL]))) {
                if (key_linepos > 1)
                    key_linepos--;
            } else if ((key == K_UPARROW) || (key == K_KP_UPARROW) || ((key == 'p') && keydown[K_CTRL])) {
                do {
                    history_line = (history_line - 1) & 31;
                }
                while (history_line != edit_line && key_lines[history_line][1] == 0);
                if (history_line == edit_line)
                    history_line = (edit_line + 1) & 31;

                System.arraycopy(key_lines[history_line], 0, key_lines[edit_line], 0, key_lines[edit_line].length);
                key_linepos = Lib.strlen(key_lines[edit_line]);
            } else if ((key == K_DOWNARROW) || (key == K_KP_DOWNARROW) || ((key == 'n') && keydown[K_CTRL])) {
                if (history_line != edit_line) {
                    do {
                        history_line = (history_line + 1) & 31;
                    }
                    while (history_line != edit_line && key_lines[history_line][1] == 0);
                    if (history_line == edit_line) {
                        key_lines[edit_line][0] = ']';
                        key_linepos = 1;
                    } else {

                        System.arraycopy(key_lines[history_line], 0, key_lines[edit_line], 0, key_lines[edit_line].length);
                        key_linepos = Lib.strlen(key_lines[edit_line]);
                    }
                }
            } else if (key == K_PGUP || key == K_KP_PGUP) {
                con.display -= 2;
            } else if (key == K_PGDN || key == K_KP_PGDN) {
                con.display += 2;
                if (con.display > con.current)
                    con.display = con.current;
            } else if (key == K_HOME || key == K_KP_HOME) {
                con.display = con.current - con.totallines + 10;
            } else if (key == K_END || key == K_KP_END) {
                con.display = con.current;
            } else if (key >= 32 && key <= 127) {
                if (key_linepos < MAXCMDLINE - 1) {
                    key_lines[edit_line][key_linepos] = (byte) key;
                    key_linepos++;
                    key_lines[edit_line][key_linepos] = 0;
                }
            }

        }

    }

    private static void printCompletions(String type, List compl) {
        Com.Printf(type);
        for (Object o : compl) {
            Com.Printf(o + " ");
        }
        Com.Printf("\n");
    }

    private static void CompleteCommand() {

        int start = 1;
        if (key_lines[edit_line][start] == '\\' || key_lines[edit_line][start] == '/')
            start++;

        int end = start;
        while (key_lines[edit_line][end] != 0) end++;

        String s = new String(key_lines[edit_line], start, end - start);

        var cmds = Cmd.CompleteCommand(s);
        var vars = Cvar.CompleteVariable(s);

        int c = cmds.size();
        int v = vars.size();

        if ((c + v) > 1) {
            if (c > 0) printCompletions("\nCommands:\n", cmds);
            if (v > 0) printCompletions("\nVariables:\n", vars);
            return;
        } else if (c == 1) {
            s = (String) cmds.get(0);
        } else if (v == 1) {
            s = (String) vars.get(0);
        } else return;

        key_lines[edit_line][1] = '/';
        byte[] bytes = Lib.stringToBytes(s);
        System.arraycopy(bytes, 0, key_lines[edit_line], 2, bytes.length);
        key_linepos = bytes.length + 2;
        key_lines[edit_line][key_linepos++] = ' ';
        key_lines[edit_line][key_linepos] = 0;

    }

    private static void Key_Bind_f() {
        int c = Cmd.Argc();

        if (c < 2) {
            Com.Printf("bind <key> [command] : attach a command to a key\n");
            return;
        }
        int b = StringToKeynum(Cmd.Argv(1));
        if (b == -1) {
            Com.Printf('"' + Cmd.Argv(1) + "\" isn't a valid key\n");
            return;
        }

        if (c == 2) {
            if (keybindings[b] != null)
                Com.Printf('"' + Cmd.Argv(1) + "\" = \"" + keybindings[b] + "\"\n");
            else
                Com.Printf('"' + Cmd.Argv(1) + "\" is not bound\n");
            return;
        }


        String cmd = "";
        for (int i = 2; i < c; i++) {
            cmd += Cmd.Argv(i);
            if (i != (c - 1))
                cmd += " ";
        }

        SetBinding(b, cmd);
    }

    static void SetBinding(int keynum, String binding) {
        if (keynum == -1)
            return;


        keybindings[keynum] = null;

        keybindings[keynum] = binding;
    }

    private static void Key_Unbind_f() {

        if (Cmd.Argc() != 2) {
            Com.Printf("unbind <key> : remove commands from a key\n");
            return;
        }

        int b = StringToKeynum(Cmd.Argv(1));
        if (b == -1) {
            Com.Printf('"' + Cmd.Argv(1) + "\" isn't a valid key\n");
            return;
        }

        SetBinding(b, null);
    }

    private static void Key_Unbindall_f() {
        for (int i = 0; i < 256; i++)
            SetBinding(i, null);
    }

    private static void Key_Bindlist_f() {
        for (int i = 0; i < 256; i++)
            if (keybindings[i] != null && !keybindings[i].isEmpty())
                Com.Printf(KeynumToString(i) + " \"" + keybindings[i] + "\"\n");
    }

    static void ClearStates() {

        anykeydown = 0;

        for (int i = 0; i < 256; i++) {
            if (keydown[i] || key_repeats[i] != 0)
                Event(i, false, 0);
            keydown[i] = false;
            key_repeats[i] = 0;
        }
    }

    public static void WriteBindings(RandomAccessFile f) {
        for (int i = 0; i < 256; i++)
            if (keybindings[i] != null && !keybindings[i].isEmpty())
                try {
                    f.writeBytes("bind " + KeynumToString(i) + " \"" + keybindings[i] + "\"\n");
                } catch (IOException e) {
                }
    }

}
