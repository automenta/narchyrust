package nars;

import nars.concept.Operator;
import nars.term.Functor;
import nars.term.obj.JsonTerm;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@CommandLine.Command(name = "n", header = {
        NAR.VERSION,
        "⚠️☢️☣️♾️ WARNING EXPERIMENTAL | ADVERTENCIA EXPERIMENTAL | ПРЕДУПРЕЖДЕНИЕ ЭКСПЕРИМЕНТАЛЬНЫЙ | चेतावनी प्रयोगात्मक | 警告实验 | تحذير التجريبية",
}, mixinStandardHelpOptions = true)
public class Shell implements Runnable {

//	@CommandLine.Command(name = "parse")
//	public final Callable parse = new Callable<String>() {
//		@Override
//		public String call() throws Exception {
//			return null;
//		}
//	};


//	@CommandLine.Command(name="net")
//	public void net(@CommandLine.Option(names="localhost", defaultValue = "localhost", help=true) String localhost) {
//
//	}

//	@CommandLine.Command(name="replx")
//	public void repl(/*@CommandLine.Option(names="input", defaultValue = "") String input*/) {
//	}

    public static void main(String[] args) {
        new Repl().run();
//		CommandLine cli = new CommandLine(new Shell())
//			.addSubcommand("repl",  new Repl())
//			.addSubcommand("net",  new Net())
//
//
//		;
//
//// the default is RunLast, this can be customized:
//		cli.setExecutionStrategy(new CommandLine.RunAll());
//
//		cli.setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO));
//		System.exit(cli.execute(args));
////		Object result = cli.getExecutionResult();
////		System.out.println(result);
    }

    public static void shell(NAR n) {
        Terminal t = null;
        try {
            t = TerminalBuilder.builder()
                    //.system(true)
                    //.jna(true)
                    //.exec(true)
                    .jansi(true)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LineReader r = LineReaderBuilder.builder()
                .terminal(t)
                //.completer(systemRegistry.completer())
                //.parser(parser)
//				.variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
//				.variable(LineReader.INDENTATION, 2)
//				.variable(LineReader.LIST_MAX, 100)
                //.variable(LineReader.HISTORY_FILE, Paths.get(root, "history"))
//				.option(Option.INSERT_BRACKET, true)
//				.option(Option.EMPTY_WORD_OPTIONS, false)
//				.option(Option.USE_FORWARD_SLASH, true)             // use forward slash in directory separator
//				.option(Option.DISABLE_EVENT_EXPANSION, true)
                .build();
        String l;


        try {
			while ((l = r.readLine("> ")) != null) {
                l = l.trim();
                if (l.isEmpty())
                    continue;

                try {
                    n.input(l);
                } catch (Throwable e) {
                    r.printAbove(e.toString());
                    //r.setTailTip(e.toString());
                    //e.printStackTrace();
                }
            }
		} catch (EndOfFileException f) {

		}

        System.exit(0);


//		LineNumberReader lr = new LineNumberReader(new InputStreamReader(System.in));
//		while (true) {
//            try {
//                String l = lr.readLine();
//                if (l == null)
//                    break;
//                n.input(l);
//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                break;
//            }
//        }
    }

    @Override
    public void run() {
    }

//	private static void readJShell(NAR n) {
////		Reader originalReader = new InputStreamReader(System.in); // this reader is connected to the original data source
////
////		Modifier myModifier = new RegexModifier("(\\s+)", Pattern.CASE_INSENSITIVE, "x");
////
////		Reader modifyingReader = new ModifyingReader(originalReader, myModifier);
////
////		ReaderInputStream in = new ReaderInputStream(modifyingReader);
//
////		JShell js = JShell.builder().in(in).build();
//
//
//
////                    .in(termOut)
////
////                    .build();
//	}

//	protected static void readJLine(NAR n) {
//		try {
//
//			LineReader reader = null;
//			Terminal terminal = TerminalBuilder.builder()
//				.system(true)
//				.signalHandler(Terminal.SignalHandler.SIG_IGN)
//				.build();
//
//			reader = LineReaderBuilder.builder()
//				.terminal(terminal)
//	//			.completer(completer)
//	//			.parser(parser)
//				.variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
//				.variable(LineReader.INDENTATION, 2)
//				.build();
//
//			String line;
//			while ((line = reader.readLine())!=null) {
//				try {
//					n.input(line);
//				} catch (Narsese.NarseseException e) {
//					e.printStackTrace();
//				}
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		//tailtipWidgets = new Widgets.TailTipWidgets(reader, commandDescription, 5, Widgets.TailTipWidgets.TipType.COMPLETER);
//
////		Widgets.CmdDesc commandDescription(CmdLine line) {
////			Widgets.CmdDesc out = null;
////			switch (line.getDescriptionType()) {
////				case COMMAND:
////					out = myCommandDescription(line);
////					break;
////				case METHOD:
////					out = methodDescription(line);
////					break;
////				case SYNTAX:
////					out = syntaxDescription(line);
////					break;
////			}
////			return out;
////		}
//	}

    @CommandLine.Command
    static class Repl implements Runnable {

        @Override public void run() {
            shell(nar());
        }

        private static NAR nar() {
            NAR n = null;//NARchy.core();
            n.throttle(0.01f);

//        n.add(new Commentary(n));

            n.add(Operator.simple("awake", (t, nar) -> {
                n.throttle(0.9f);
            }));
            n.add(Operator.simple("calm", (t, nar) -> {
                n.throttle(0.25f);
            }));
            n.add(Operator.simple("sleep", (t, nar) -> {
                n.throttle(0.01f);
            }));

            n.add(Operator.simple("stat", (t, nar) -> {
                nar.stats(true, true, System.out);
            }));
            n.add(Operator.simple("what", (t, nar) -> {
                nar.focus.print(System.out);
            }));
            n.add(Functor.f1("json", x -> JsonTerm.the($.unquote(x))));
            n.add(Functor.f1("main", x -> {
                String classname = $.unquote(x);
                try {
                    Class.forName(classname).getMethod("main", String[].class).invoke(null, new String[0]);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }));

//        n.add(Operator.simple("..", (t, nar)->{
//            Term a = Operator.arg(t.term(),0);
//            if (a.ATOM()) {
//                switch (a.toString()) {
//                    case "
//                }
//            }
//            return null;
//        }));
            n.startFPS(10f);

            //readJShell(n);
            //readJLine(n);
            return n;
        }
    }

    @CommandLine.Command
    static class Net implements Runnable {
        @CommandLine.Option(names = "localAddr")
        String localAddr;

        @Override
        public void run() {

        }
    }

//	/**
//	 * TODO make stream/iterable
//	 */
//	private static void narsese(Supplier<String> s) {
//		NAR n = NARchy.ui();
//		String in = s.get();
//		if (in != null) {
//			try {
//				n.input(in);
//			} catch (Narsese.NarseseException e) {
//				e.printStackTrace();
//			}
//		}
//		n.start();
//	}

    //    public void shellSwing(NAR nar) {
//
//
//
//
//
//
//
//
//
//
//        MySwingTerminalFrame tt = new MySwingTerminalFrame(
//                "",
//                null,
//                null,
//
//                new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.EVERYTHING_BUT_SYMBOLS, new Font("Monospaced", Font.PLAIN, 28)),
//                null,
//                EnumSet.of(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode).toArray(new TerminalEmulatorAutoCloseTrigger[1]));
//        tt.setSize(800, 800);
//        tt.setVisible(true);
//
//
//        new TextUI(nar, tt, TERMINAL_DISPLAY_FPS);
//    }


//    public static class TestConsoleWidget {
//        public static void main(String[] args) throws IOException {
//
//
//
//
//
//
//
//
//
//            PipedOutputStream termIn;
//            PipedOutputStream termKeys = new PipedOutputStream();
//            PipedInputStream termOut = new PipedInputStream(termKeys);
//            DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(80, 40));
//            ConsoleTerminal termView;
//
////
////            JShell js = JShell.builder()
////                    .in(termOut)
////
////                    .build();
//
//
//            SpaceGraph.window(termView = new ConsoleTerminal(term) {
//                @Override
//                public boolean key(KeyEvent e, boolean pressedOrReleased) {
//
//
////                        js.eval(String.valueOf(e.getKeyChar()));
//
//
//
//
//                    return super.key(e, pressedOrReleased);
//                }
//            },1000,800);
//
//            termView.output().write('x');
//            termView.output().flush();
//        }
//
//
//        }
//
////        public static class ConsoleWidget extends Widget {
////
////            private final ConsoleTerminal console;
////            AtomicBoolean menuShown = new AtomicBoolean(false);
////
////            public ConsoleWidget(VirtualTerminal target) {
////
////
////                Surface menu = new Scale(new LabeledPane("Text Scale", new Gridding(
////                        new XYSlider()
////                )), 0.5f);
////
////                this.console = new ConsoleTerminal(target) {
////
////                    float charAspect = 1.6f;
////
////
////
////
////                    float scale = 80f;
////
////                    Predicate<Finger> pressable = Finger.clicked(0, () -> {
////                        if (menuShown.compareAndSet(false, true)) {
////                            setAt(menu);
////                        } else if (menuShown.compareAndSet(true, false)) {
////                            setAt(new EmptySurface());
////                        }
////                    });
////
////
////                    @Override
////                    public void onFinger(@Nullable Finger finger) {
////                        super.onFinger(finger);
////                        pressable.test(finger);
////                    }
////
////
////                    @Override
////                    public void doLayout(int dtMS) {
////
//////                        text.doLayout(dtMS);
////
////                        float cc, rr;
////                        float boundsAspect = text.h() / text.w();
////                        if (boundsAspect >= 1) {
////
////                            cc = scale / boundsAspect;
////                            rr = cc / charAspect;
////
////                        } else {
////
////                            cc = scale;
////                            rr = cc * (boundsAspect / charAspect);
////
////                        }
////
////                        resize(Math.max(2, Math.round(cc)), Math.max(2, Math.round(rr)));
////
////                    }
////
////                };
////
////
////                setAt(console);
////            }
////        }
//
//        /**
//         * the original SwingTerminalFrame avoids any means of reasonable configuration :(
//         * <p>
//         * whats so scary about 'public final' FFS
//         */
//        public static class MySwingTerminalFrame extends JFrame implements IOSafeTerminal {
//            public final SwingTerminal swingTerminal;
//            public final EnumSet<TerminalEmulatorAutoCloseTrigger> autoCloseTriggers;
//            private boolean disposed;
//
//            /**
//             * Creates a new SwingTerminalFrame with an optional list of auto-close triggers
//             *
//             * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
//             */
//            @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
//            public MySwingTerminalFrame(TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
//                this("SwingTerminalFrame", autoCloseTriggers);
//            }
//
//            /**
//             * Creates a new SwingTerminalFrame with a specific title and an optional list of auto-close triggers
//             *
//             * @param title             Title to use for the window
//             * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
//             */
//            @SuppressWarnings("WeakerAccess")
//            public MySwingTerminalFrame(String title, TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) throws HeadlessException {
//                this(title, new SwingTerminal(), autoCloseTriggers);
//            }
//
//            /**
//             * Creates a new SwingTerminalFrame using a specified title and a series of swing terminal configuration objects
//             *
//             * @param title               What title to use for the window
//             * @param deviceConfiguration Device configuration for the embedded SwingTerminal
//             * @param fontConfiguration   Font configuration for the embedded SwingTerminal
//             * @param colorConfiguration  Color configuration for the embedded SwingTerminal
//             * @param autoCloseTriggers   What to trigger automatic disposal of the JFrame
//             */
//            public MySwingTerminalFrame(String title,
//                                        TerminalEmulatorDeviceConfiguration deviceConfiguration,
//                                        SwingTerminalFontConfiguration fontConfiguration,
//                                        TerminalEmulatorColorConfiguration colorConfiguration,
//                                        TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
//                this(title, null, deviceConfiguration, fontConfiguration, colorConfiguration, autoCloseTriggers);
//            }
//
//            /**
//             * Creates a new SwingTerminalFrame using a specified title and a series of swing terminal configuration objects
//             *
//             * @param title               What title to use for the window
//             * @param terminalSize        Initial size of the terminal, in rows and columns. If null, it will default to 80x25.
//             * @param deviceConfiguration Device configuration for the embedded SwingTerminal
//             * @param fontConfiguration   Font configuration for the embedded SwingTerminal
//             * @param colorConfiguration  Color configuration for the embedded SwingTerminal
//             * @param autoCloseTriggers   What to trigger automatic disposal of the JFrame
//             */
//            public MySwingTerminalFrame(String title,
//                                        TerminalSize terminalSize,
//                                        TerminalEmulatorDeviceConfiguration deviceConfiguration,
//                                        SwingTerminalFontConfiguration fontConfiguration,
//                                        TerminalEmulatorColorConfiguration colorConfiguration,
//                                        TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
//                this(title,
//                        new SwingTerminal(terminalSize, deviceConfiguration, fontConfiguration, colorConfiguration),
//                        autoCloseTriggers);
//            }
//
//            private MySwingTerminalFrame(String title, SwingTerminal swingTerminal, TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
//                super(title != null ? title : "SwingTerminalFrame");
//                this.swingTerminal = swingTerminal;
//                this.autoCloseTriggers = EnumSet.copyOf(Arrays.asList(autoCloseTriggers));
//                this.disposed = false;
//
//                swingTerminal.setIgnoreRepaint(true);
//                setContentPane(swingTerminal);
//
//
//                setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//                setBackground(Color.BLACK);
//                pack();
//
//
//                swingTerminal.requestFocusInWindow();
//            }
//
//            /**
//             * Returns the auto-close triggers used by the SwingTerminalFrame
//             *
//             * @return Current auto-close trigger
//             */
//            public Set<TerminalEmulatorAutoCloseTrigger> getAutoCloseTrigger() {
//                return EnumSet.copyOf(autoCloseTriggers);
//            }
//
//            /**
//             * Sets the auto-close trigger to use on this terminal. This will reset any previous triggers. If called with
//             * {@code null}, all triggers are cleared.
//             *
//             * @param autoCloseTrigger Auto-close trigger to use on this terminal, or {@code null} to clear all existing triggers
//             * @return Itself
//             */
//            public MySwingTerminalFrame setAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger autoCloseTrigger) {
//                this.autoCloseTriggers.clear();
//                if (autoCloseTrigger != null) {
//                    this.autoCloseTriggers.add(autoCloseTrigger);
//                }
//                return this;
//            }
//
//            /**
//             * Adds an auto-close trigger to use on this terminal.
//             *
//             * @param autoCloseTrigger Auto-close trigger to add to this terminal
//             * @return Itself
//             */
//            public MySwingTerminalFrame addAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger autoCloseTrigger) {
//                if (autoCloseTrigger != null) {
//                    this.autoCloseTriggers.add(autoCloseTrigger);
//                }
//                return this;
//            }
//
//            @Override
//            public void dispose() {
//                super.dispose();
//                disposed = true;
//            }
//
//            @Override
//            public void close() {
//                dispose();
//            }
//
//            /**
//             * Takes a KeyStroke and puts it on the input queue of the terminal emulator. This way you can insert synthetic
//             * input events to be processed as if they came from the user typing on the keyboard.
//             *
//             * @param keyStroke Key stroke input event to put on the queue
//             */
//            public void addInput(com.googlecode.lanterna.input.KeyStroke keyStroke) {
//                swingTerminal.addInput(keyStroke);
//            }
//
//
//
//
//            @Override
//            public com.googlecode.lanterna.input.KeyStroke pollInput() {
//                if (disposed) {
//                    return new com.googlecode.lanterna.input.KeyStroke(KeyType.EOF);
//                }
//                com.googlecode.lanterna.input.KeyStroke keyStroke = swingTerminal.pollInput();
//                if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnEscape) &&
//                        keyStroke != null &&
//                        keyStroke.getKeyType() == KeyType.Escape) {
//                    dispose();
//                }
//                return keyStroke;
//            }
//
//            @Override
//            public com.googlecode.lanterna.input.KeyStroke readInput() {
//                return swingTerminal.readInput();
//            }
//
//            @Override
//            public void enterPrivateMode() {
//                swingTerminal.enterPrivateMode();
//            }
//
//            @Override
//            public void exitPrivateMode() {
//                swingTerminal.exitPrivateMode();
//                if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode)) {
//                    dispose();
//                }
//            }
//
//            @Override
//            public void clearScreen() {
//                swingTerminal.clearScreen();
//            }
//
//            @Override
//            public void setCursorPosition(int x, int y) {
//                swingTerminal.setCursorPosition(x, y);
//            }
//
//            @Override
//            public TerminalPosition getCursorPosition() {
//                return swingTerminal.getCursorPosition();
//            }
//
//            @Override
//            public void setCursorPosition(TerminalPosition position) {
//                swingTerminal.setCursorPosition(position);
//            }
//
//            @Override
//            public void setCursorVisible(boolean visible) {
//                swingTerminal.setCursorVisible(visible);
//            }
//
//            @Override
//            public void putCharacter(char c) {
//                swingTerminal.putCharacter(c);
//            }
//
//            @Override
//            public TextGraphics newTextGraphics() {
//                return swingTerminal.newTextGraphics();
//            }
//
//            @Override
//            public void enableSGR(SGR sgr) {
//                swingTerminal.enableSGR(sgr);
//            }
//
//            @Override
//            public void disableSGR(SGR sgr) {
//                swingTerminal.disableSGR(sgr);
//            }
//
//            @Override
//            public void resetColorAndSGR() {
//                swingTerminal.resetColorAndSGR();
//            }
//
//            @Override
//            public void setForegroundColor(TextColor color) {
//                swingTerminal.setForegroundColor(color);
//            }
//
//            @Override
//            public void setBackgroundColor(TextColor color) {
//                swingTerminal.setBackgroundColor(color);
//            }
//
//            @Override
//            public TerminalSize getTerminalSize() {
//                return swingTerminal.getTerminalSize();
//            }
//
//            @Override
//            public byte[] enquireTerminal(int timeout, TimeUnit timeoutUnit) {
//                return swingTerminal.enquireTerminal(timeout, timeoutUnit);
//            }
//
//            @Override
//            public void bell() {
//                swingTerminal.bell();
//            }
//
//            @Override
//            public void flush() {
//                swingTerminal.flush();
//            }
//
//            @Override
//            public void addResizeListener(TerminalResizeListener listener) {
//                swingTerminal.addResizeListener(listener);
//            }
//
//            @Override
//            public void removeResizeListener(TerminalResizeListener listener) {
//                swingTerminal.removeResizeListener(listener);
//            }
//        }
//
}