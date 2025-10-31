package nars;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.menu.Menu;
import com.googlecode.lanterna.gui2.menu.MenuBar;
import com.googlecode.lanterna.gui2.menu.MenuItem;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.gui2.table.TableModel;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import jcog.Config;
import jcog.event.Off;
import jcog.pri.PriReference;
import jcog.pri.bag.util.Bagregate;
import jcog.pri.op.PriMerge;

import java.awt.*;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * text UI
 */
public class TUI {


    private final NAR nar;
    private final Focus focus;
    private Panel content;
    @Deprecated private Component contentComponent;

    public static void main(String[] args) throws IOException, InterruptedException {
        NAR n = NARS.tmp();
        n.startFPS(1);
        new TUI(n);
    }

    static final int fontSize = 22;

    public TUI(NAR n) throws InterruptedException, IOException {

        this.nar = n;
        this.focus = n.main();

        DefaultTerminalFactory f = new DefaultTerminalFactory();
        f.setTelnetPort(Config.INT("telnetport", 0));
        var ff = SwingTerminalFontConfiguration
                .newInstance(new Font("Monospaced", 0, fontSize));
        f.setTerminalEmulatorFontConfiguration(ff);

        Screen s = f.createScreen();
        s.startScreen();

        var ui = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), s);

        //ui.setTheme(LanternaThemes.getRegisteredTheme(theme));
        ui.setBlockingIO(false);
        ui.setEOFWhenNoWindows(true);

        try {
            ui.addWindow(
                narWindow(n)
                //exampleWindow()
            );

            var guiThread = (AsynchronousTextGUIThread) ui.getGUIThread();
            guiThread.start();

            ready(ui);

            guiThread.waitForStop();

        } finally {
            s.stopScreen();
        }
    }

    protected void ready(WindowBasedTextGUI textGUI) {
    }

    private Window narWindow(NAR n) {
        BasicWindow w = new BasicWindow(n.self().toString());

        w.setHints(java.util.List.of(Window.Hint.FULL_SCREEN
                //, Window.Hint.NO_DECORATIONS
        ));

        Panel c = new Panel(new BorderLayout());
//        {
//            ComboBox running = new ComboBox();
//            running.setReadOnly(true);
//            running.addItem("Fast");
//            running.addItem("Slow");
//            running.addItem("Pause");
//            running.addListener((selectedIndex, previousSelection) -> {
//
//            });
//
//            Panel exec = Panels.horizontal(
//                running,
//                new Button("Clear", focus::clear),
//                new Button("Load", ()-> todoMessage(w)),
//                new Button("Save", ()-> todoMessage(w)),
//                new Button("Meta", ()-> todoMessage(w))
//            );
//            exec.setLayoutData(BorderLayout.Location.TOP);
//            exec.addTo(c);
//        }

        {
            var contentWrapper = new Panel(new BorderLayout());
            {
                Panel content = new Panel();
                content.setLayoutData(BorderLayout.Location.CENTER);
                content.addTo(contentWrapper);
                this.content = content;
            }
            {
                var prompt = new TextBox("", TextBox.Style.SINGLE_LINE) {

                    @Override public Result handleKeyStroke(KeyStroke keyStroke) {
                        if (keyStroke.getKeyType()== KeyType.Enter) {
                            handleInput();
                            return Result.HANDLED;
                        } else
                            return super.handleKeyStroke(keyStroke);
                    }

                    private void handleInput() {
                        try {
                            input(getText());
                            setText("");
                        } catch (Narsese.NarseseException e) {
                            message((WindowBasedTextGUI) getTextGUI(), "Syntax error", e.toString());
                        }
                    }

                };
                prompt.setLayoutData(BorderLayout.Location.BOTTOM);
                prompt.withBorder(Borders.singleLine("Input")).addTo(contentWrapper);
            }
            contentWrapper.setLayoutData(BorderLayout.Location.CENTER);
            contentWrapper.addTo(c);
        }

        w.setComponent(c);

        MenuBar m = new MenuBar();

        {
            Menu mf = new Menu("What");

            mf.add(new com.googlecode.lanterna.gui2.menu.MenuItem("main"));
            mf.add(new com.googlecode.lanterna.gui2.menu.MenuItem("New..."));
            //TODO all focuses, continually updated

            m.add(mf);
        }
        {
            Menu mf = new Menu("Focus");

            mf.add(new MenuItem("Clear"));
            mf.add(new MenuItem("Load..."));
            mf.add(new MenuItem("Save..."));
            mf.add(new MenuItem("Stream..."));
            mf.add(new MenuItem("Stats", () -> {}));

            //TODO all focuses, continually updated

            m.add(mf);
        }

        {
            Menu mf = new Menu("Run");
            mf.add(new com.googlecode.lanterna.gui2.menu.MenuItem("Pause"));
            mf.add(new com.googlecode.lanterna.gui2.menu.MenuItem("Slow"));
            mf.add(new com.googlecode.lanterna.gui2.menu.MenuItem("Fast"));
            m.add(mf);
        }

        /* view buttons */
        {
            m.add(new MenuButton("Log", this::viewLog));
            m.add(new MenuButton("Tasks", this::viewTasks));
            m.add(new MenuButton("Concepts", () -> { }));
            m.add(new MenuButton("Links", () -> { }));
        }

        w.setMenuBar(m);

        viewLog();
        //viewTasks();

        return w;
    }


    private static MessageDialogButton todoMessage(BasicWindow w) {
        return message(w.getTextGUI(), "TODO", "TODO");
    }

    private static MessageDialogButton message(WindowBasedTextGUI w, String title, String text) {
        return MessageDialog.showMessageDialog(w, title, text);
    }

    public void input(String x) throws Narsese.NarseseException {
        //System.out.println("input: " + x);
        focus.accept(nar.inputTask(x));
    }

    public synchronized void setContent(String title, Component l) {
        if (contentComponent!=null) {
            //HACK because Lanterna doesnt call onRemoved recursively for the border wrapper
            this.contentComponent.onRemoved(contentComponent.getParent());
            this.contentComponent = null;
        }

        content.removeAllComponents();
        content.addComponent((this.contentComponent = l).withBorder(Borders.singleLine()));
    }

    public void viewLog() {
        setContent("Log", new LogView());
    }

    public void viewTasks() {
        setContent("Tasks", new TaskView());
    }


    @Deprecated private BasicWindow exampleWindow() {
        Panel leftPanel = new Panel();
        Panel cbPanel = new Panel();
        for (int i = 0; i < 4; i++)
            cbPanel.addComponent(new CheckBox("Checkbox #" + (i + 1)));


        Panel textBoxPanel = new Panel();
        textBoxPanel.addComponent(Panels.horizontal(new Label("Normal:   "), new TextBox(new TerminalSize(12, 1), "Text")));
        textBoxPanel.addComponent(Panels.horizontal(new Label("Password: "), new TextBox(new TerminalSize(12, 1), "Text").setMask('*')));

        Panel buttonPanel = new Panel();
        buttonPanel.addComponent(new Button("Enable spacing", () -> {
            LinearLayout layoutManager = (LinearLayout) leftPanel.getLayoutManager();
            layoutManager.setSpacing(layoutManager.getSpacing() == 0 ? 1 : 0);
        }));

        leftPanel.addComponent(cbPanel.withBorder(Borders.singleLine("CheckBoxes")));
        leftPanel.addComponent(textBoxPanel.withBorder(Borders.singleLine("TextBoxes")));
        leftPanel.addComponent(buttonPanel.withBorder(Borders.singleLine("Buttons")));

        Panel rightPanel = new Panel();
        textBoxPanel = new Panel();
        TextBox readOnlyTextArea = new TextBox(new TerminalSize(16, 8));
        readOnlyTextArea.setReadOnly(true);
        readOnlyTextArea.setText("abc");
        textBoxPanel.addComponent(readOnlyTextArea);
        rightPanel.addComponent(textBoxPanel.withBorder(Borders.singleLine("Read-only")));

        ProgressBar progressBar = new ProgressBar(0, 100, 16);
        progressBar.setRenderer(new ProgressBar.LargeProgressBarRenderer());
        progressBar.setLabelFormat("%2.0f%%");
        rightPanel.addComponent(progressBar.withBorder(Borders.singleLine("ProgressBar")));
        rightPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

        Timer timer = new Timer("ProgressBar-timer", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (progressBar.getValue() == progressBar.getMax()) {
                    progressBar.setValue(0);
                } else {
                    progressBar.setValue(progressBar.getValue() + 1);
                }
            }
        }, 250, 250);

        Panel contentArea = new Panel();
        contentArea.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        contentArea.addComponent(Panels.horizontal(leftPanel, rightPanel));
        contentArea.addComponent(
                new Separator(Direction.HORIZONTAL).setLayoutData(
                        LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));

        BasicWindow window = new BasicWindow("Grid layout test");

        Button okButton = new Button("OK", () -> {
            window.close();
            timer.cancel();
        }).setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        contentArea.addComponent(okButton);
        window.setComponent(contentArea);
        window.setFocusedInteractable(okButton);

        return window;
    }

    static class MenuButton extends Menu {
        public Runnable action;

        MenuButton(String label, Runnable action) {
            super(label);
            this.action = action;
        }

        @Override
        protected Result handleKeyStroke(KeyStroke keyStroke) {
            if (keyStroke.getKeyType()== KeyType.Enter) {
                this.action.run();
            }
            return super.handleKeyStroke(keyStroke);
        }
    }

    /** see nars.gui.AbstractItemList */
    private class TaskView extends Table {



        TaskView() {
            super("Task");
        }

        static final int capacity = 100;
        static final float updateRate = 0.25f;

        private Off on, on2;

        final Bagregate<NALTask> bag = new Bagregate<>(capacity, PriMerge.max)/* {
//            @Override
//            protected float pri(NALTask x) {
//                return super.pri(x) * updateRate;
//            }
        }*/;

        private synchronized void onTask(Task task) {
            if (task instanceof NALTask N)
                bag.put(N);
        }
        private void update() {
            TextGUI g = getTextGUI();if (g==null) return;

            g.getGUIThread().invokeLater(()->{
                TableModel m = getTableModel();
                m.clear();
                bag.commit();
                for (var r : bag)
                    m.addRow(row(r));
            });
        }

        private Object[] row(PriReference<NALTask> r) {
            return new Object[] {
                r.get().toString()
            };
        }

        @Override
        public void onAdded(Container container) {
            super.onAdded(container);
            on = focus.onTask(this::onTask);
            on2 = focus.nar.onDur(this::update, 1/updateRate);
        }

        @Override
        public void onRemoved(Container container) {
            on2.close();
            on.close();
            on = null;
            super.onRemoved(container);
        }
    }

    private class LogView extends TextBox {

        int maxLines = 100;
        int maxCols = 1000;
        boolean showStamp = true;

        {
            setHorizontalFocusSwitching(true);
            setVerticalFocusSwitching(true);
            setReadOnly(true);
            setPreferredSize(new TerminalSize(maxCols, maxLines));
        }

        LogView() {
            super("", Style.MULTI_LINE);
        }

        private synchronized void onTask(Task task) {

            TextGUI g = getTextGUI(); if (g == null) return;

            //TODO batch these calls
            g.getGUIThread().invokeLater(()->{
                if (getLineCount() >= maxLines)
                    this.removeLine(0);
                addLine(taskString(task));
                //int n = getLineCount();
                //this.setCaretPosition(n-1,0);
                //this.getRenderer().setViewTopLeft(new TerminalPosition(0, Math.max(0,n-1-getSize().getRows()-1)));
                //this.invalidate();

                handleInput(new KeyStroke(KeyType.PageDown));
            });

//                if (getLineCount() >= maxLines)
//                    this.removeLine(getLineCount()-1);
//                addLine()

        }

        private String taskString(Task task) {
            if (showStamp)
                return task.toString(true).toString();
            else
                return task.toString();
        }

        private Off on;

        @Override
        public void onAdded(Container container) {
            super.onAdded(container);
            on = focus.onTask(this::onTask);
        }

        @Override
        public void onRemoved(Container container) {
            on.close();
            on = null;
            super.onRemoved(container);
        }
    }
}