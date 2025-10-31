package nars.experiment.market;

import jcog.Fuzzy;
import jcog.Util;
import jcog.agent.Agent;
import jcog.data.list.Lst;
import jcog.math.FloatDifference;
import jcog.math.FloatMeanEwma;
import jcog.math.normalize.FloatNormalized;
import jcog.signal.ArraySensor;
import jcog.tensor.Agents;
import nars.$;
import nars.Player;
import nars.experiment.RLPlayer;
import nars.game.Game;
import nars.game.action.util.Reflex0;
import nars.gui.ReflexUI;
import nars.sensor.BitmapSensor;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.hipparchus.linear.ArrayRealVector;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Stacking;
import spacegraph.video.Draw;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.io.Source;
import tech.tablesaw.io.csv.CsvReader;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jcog.Str.n4;
import static spacegraph.SpaceGraph.window;

public class Market {

    static final boolean trace = true;

    /** Prints performance at end of an episode */
    static final boolean traceEpisode = false;

    /**
     * Ticker symbol index
     */
    final Map<String, Stock> sym = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        var m = new Market();
        var g = m.game(10000,
                "/home/me/crypto/all.csv","BTC", "LTC", "DOGE" //, "ETH", "XRP"
        );
        m.sym.get("BTC").minShare = 1.0E-8; //satoshis


        // Commented out old CSV builder for clarity
        /*
        MarketGame g = new Market().game(10000, (G)->{
            String mpdata = "/home/me/mpdata/import/klines/1m/";
            //m.csv("BTC", mpdata + "BTC/USDT/final-BTC-USDT-1m.out.csv").accept(G);
            m.csv("DOGE", mpdata + "DOGE/USDT/final-DOGE-USDT-1m.out.csv").accept(G);
            m.csv("XRP", mpdata + "XRP/USDT/final-XRP-USDT-1m.out.csv").accept(G);
            m.csv("LTC", mpdata + "LTC/USDT/final-LTC-USDT-1m.out.csv").accept(G);
        });
        */

        g.cycleSeconds =
                //30 * 60; //30min
                2 * 60 * 60; //2hr
                //12 * 60 * 60; //12hr

        var nars = false;
        var rlBoost = true;

        final IntIntToObjectFunction<Agent> policy =
                Agents::PPO
                //Agents::StreamAC
                //Agents::BayesZero
                //Agents::DQN
                //Agents::CMAESZero
                //Agents::CMAES
                //Agents::DQrecurrent
                ;

        if (nars) {
            runNARS(g, rlBoost, policy);
        } else {
            var fps = 0;
            RLPlayer.run(g, policy, fps,
                /* HACK TODO dont exceed 32 according to reset period below */
                //new float[] { 1, 2, 3, 4, 6, 7, 8, 12, 16 }
                new float[] { 1, 2 }
            );
        }
    }

    private static void runNARS(MarketGame g, boolean rlBoost, IntIntToObjectFunction<Agent> policy) {
        /* HACK TODO dont exceed 32 according to reset period below */
        final float[] history = {
                //1
                //1,2
                //1,4
                1, 2, 4, 8, 16
        };

        var r = rlBoost ? new Reflex0(policy, g, history) : null;

        var p = new Player(g).fps(50);

        p.start();

        if (rlBoost)
            window(ReflexUI.reflexUI(r), 900, 900);
    }

    public Consumer<MarketGame> csv(String sym, String csvPath) {
        return new NewCSVBuilder(sym, csvPath);
    }

    @Deprecated public MarketGame game(double initialCash, String csvPath, String... portfolio_) {
        return game(initialCash, new OldCSVBuilder(csvPath, portfolio_));
    }

    public MarketGame game(double initialCash, Consumer<MarketGame> builder) {
        return new MarketGame(initialCash, builder);
    }

    private Stock sym(String s) {
        return sym.get(s);
    }

    Stock symbolAdd(String s) {
        return sym.computeIfAbsent(s, Stock::new);
    }

    public double value(Account a) {
        double totalValue = a.cash; // Include cash directly
        for (var s : sym.values())
            totalValue += value(s, a);
        return totalValue;
    }

    private static double value(Stock s, Account a) {
        var p = s.price;
        return Double.isFinite(p) ?
                p * s.owns(a)
                :
                0;
    }

    public Stock symbol(String symbol) {
        return sym.get(symbol);
    }

    /**
     * Snapshot of portfolio state
     */
    static class Performance {
        final long time;
        final double value;
        final double valueChange;
        final private long iteration;

        Performance(long time, double value, double valueChange, long iteration) {
            this.time = time;
            this.value = value;
            this.valueChange = valueChange;
            this.iteration = iteration;
        }
        //TODO store complete portfolio clone
    }

    public class OldCSVBuilder implements Consumer<MarketGame> {

        public final String csvPath;
        private final String[] portfolio;

        public OldCSVBuilder(String csvPath, String... portfolio) {
            this.csvPath = csvPath;
            this.portfolio = portfolio;
        }

        @Override
        public void accept(MarketGame g) {
            var t = new CsvReader().read(new Source(new File(csvPath)));
            //OLD CSV FORMAT
            //[Integer column: SNo, String column: Name,
            // String column: Symbol, DateTime column: Date, Double column: High, Double column: Low, Double column: Open, Double column: Close, Double column: Volume, Double column: Marketcap]

            g.cycleSeconds = 6 /* hr */ * 60 * 60;

            t.removeColumns(0, 1);
            t.forEach(r -> {

                var when = (r.getDateTime(1).getLong(ChronoField.EPOCH_DAY) + 1) * (24 * 60 * 60);
                //WHEN.toEpochSecond(ZoneOffset.UTC)

                var volume = r.getDouble(6);
                var priceOpen = r.getDouble(4);
                var priceClose = r.getDouble(5);
                //double priceOpenClose = Fuzzy.mean(priceOpen, priceClose;

                var s = symbolAdd(r.getString(0));

                g.addEvent(when + (9 /* 9am */ * 60 * 60), m -> {
                    s.price = priceOpen;
                    s.volume = volume;
                });

                g.addEvent(when + (17 /* 5pm*/ * 60 * 60) /* 12hrs later */, m -> {
                    s.price = priceClose;
                });
            });

            for (var p : portfolio)
                g.portfolio.add(sym(p));
        }
    }

    public class NewCSVBuilder implements Consumer<MarketGame> {

        public final String csvPath;
        private final Stock sym;
        private final boolean trading;

        public NewCSVBuilder(String sym, String csvPath) {
            this(sym, csvPath, true);
        }

        public NewCSVBuilder(String sym, String csvPath, boolean trading) {
            this.csvPath = csvPath;
            this.sym = symbolAdd(sym);
            this.trading = trading;
        }

        @Override
        public void accept(MarketGame g) {
            var t = new CsvReader().read(new Source(new File(csvPath)));
            /* MP-format */


            t.forEach(new Consumer<>() {
                /*
                   MP-format

                   String column: Open_time
                   String column: Close_time
                   Double column: Open
                   Double column: High
                   Double column: Low
                   Double column: Close
                   Integer column: Volume
                   Double column: Quote_asset_volume
                   Integer column: Number_of_trades
                   Integer column: Taker_buy_base_asset_volume
                   Double column: Taker_buy_quote_asset_volume
                   Double column: bollupper20
                   Double column: bollmid20
                   Double column: bolllow20
                   Text column: bollwd
                   Text column: bollpct
                */

                @Override
                public void accept(Row r) {
                    try {
                        var openTime = getDate(r.getString(0));
                        //var closeTime = getDate(r.getString(1));
                        var open = r.getDouble(2);
                        var close = r.getDouble(5);
                        var volume = r.getColumnType(6) == ColumnType.DOUBLE ? r.getDouble(6) : r.getInt(6);
                        //TODO bollinger

                        g.addEvent(openTime.getTime() / 1000 /* ms -> s */, m -> {
                            sym.price = open;
                            sym.volume = volume;
                        });

                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }

            });

            if (trading)
                g.portfolio.add(sym);
        }

        private final static SimpleDateFormat dateParser = new SimpleDateFormat("y-M-d'T'H:m:sX"); //2022-07-02T00:00:00Z
        private final static SimpleDateFormat dateParserMS = new SimpleDateFormat("y-M-d'T'H:m:s'.'SX"); //2022-07-02T00:00:00Z

        private static Date getDate(String d) throws ParseException {
            return (d.contains(".") ? dateParserMS : dateParser).parse(d);
        }

    }

    class MarketGame extends Game {

        /**
         * "taker" fee (ex: 0.1%)
         */
        static final double feePerVolume = 0.001;

        @Deprecated
        static final int historyYearMin = 2012;
        @Deprecated
        static final int historyYearMax = 2023;

        static private final boolean trainCalendar = true;
        static private final boolean trainCalendarYear = false;
        private static final boolean buySellSeparate = false;

        final NavigableMap<Long, Consumer<Market>> events = new TreeMap<>();

        final Lst<Stock> portfolio = new Lst<>();

        final double[] buy, sell, buyActual, sellActual, ownActual;

        @Deprecated
        final Map<Integer, Double> BUYING = new LinkedHashMap<>();
        @Deprecated
        final Map<Integer, Double> SELLING = new LinkedHashMap<>();

        private final double initialCash;

        private final long START, END;

        private final MarketPerformance view;
        private final MarketPerformanceCloud view2;

        /**
         * Reward normalization period
         */
        private final static int rewardNormPeriod = 100;

        /**
         * Maximum buy/sell rate as a fraction of total portfolio value
         */
        public static final float buyRateMax =
                //0.001f
                //0.1f
                0.2f
                //1/3f
                , sellRateMax = buyRateMax;

        /**
         * Allows memory-dependent sensor functions a cycle to clear on rewind
         */
        int reset;
        private long iteration;
        private double valueChange;
        private double valueBefore;
        private Account a;

        /**
         * HACK cycles between episodes allowing history values to clear
         */
        @Deprecated
        static final int RESET_PERIOD = 32;

        /**
         * In seconds
         */
        private long t, marketEnd = Long.MAX_VALUE;

        /**
         * Simulated seconds per episode
         */
        long cycleSeconds;

        /**
         * Flag to toggle sinusoidal time encoding
         */
        boolean useSinusoidalTimeEncoding = false;

        /**
         * @param builder sets 'cycleSeconds', populates 'events', and adds trading stocks to 'portfolio'
         */
        MarketGame(double initialCash, Consumer<MarketGame> builder) {
            super("market");

            this.initialCash = initialCash;

            builder.accept(this);

            START = events.firstKey();
            END = events.lastKey();

            var portfolioSize = portfolio.size();
            assert (portfolioSize > 0);

            valueBefore = Double.NaN;
            reset = 0;
            t = Long.MIN_VALUE;

            a = new Account();
            a.cash = initialCash;

            buy = new double[portfolioSize];
            sell = new double[portfolioSize];

            buyActual = new double[portfolioSize];
            sellActual = new double[portfolioSize];
            ownActual = new double[portfolioSize];

            /* SENSE: stock prices */
            var syms = sym.values().toArray(new Stock[0]);

            addSensor(new BitmapSensor(new ArraySensor(syms.length, true) {
                @Override
                protected float value(int i) {
                    return /* soften */(float) syms[i].price;
                }
            }, (x, y) -> $.inh(x >= 0 ? $.atomic(syms[x].id) : $.varDep(1), "price"))).freqRes(0);

            /* SENSE: owned portfolio wealth distribution */
            addSensor(new BitmapSensor(new ArraySensor(1 + portfolioSize, true) {
                @Override
                protected float value(int i) {
                    double amt;
                    if (i == portfolioSize)
                        amt = a.cash;
                    else {
                        var s = portfolio.get(i);
                        var p = s.price;
                        if (!Double.isFinite(p)) return 0;
                        amt = p * s.owns(a);
                    }
                    //return (float) (amt / valueBefore);
                    return (float) amt;
                }
            }, (x, y) -> $.inh(x < 0 ? $.varDep(1) : ($.atomic(x < portfolioSize ? portfolio.get(x).id : "cash")), "own"))).freqRes(0);

            /* Sense: market volume */
            addSensor(new BitmapSensor(new ArraySensor(syms.length, true) {
                @Override
                protected float value(int i) {
                    return (float) syms[i].volume;
                }
            }, (x, y) -> $.inh(x >= 0 ? $.atomic(syms[x].id) : $.varDep(1), "volume"))).freqRes(0);


            if (trainCalendar) {
                if (useSinusoidalTimeEncoding) {
                    /* SINUSOIDAL TIME ENCODING */

                    // Sense: Day of Week (1-7) - Sine
                    sense($.inh(id, $.p("dayOfWeek", "week_sin")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var dayOfWeek = epochDay().get(ChronoField.DAY_OF_WEEK); // 1..7
                        double angle = 2 * Math.PI * (dayOfWeek - 1) / 7.0;
                        return (float) Math.sin(angle);
                    }).freqRes(0);

                    // Sense: Day of Week (1-7) - Cosine
                    sense($.inh(id, $.p("dayOfWeek", "week_cos")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var dayOfWeek = epochDay().get(ChronoField.DAY_OF_WEEK); // 1..7
                        double angle = 2 * Math.PI * (dayOfWeek - 1) / 7.0;
                        return (float) Math.cos(angle);
                    }).freqRes(0);

                    // Sense: Day of Month (1-31) - Sine
                    sense($.inh(id, $.p("dayOfMonth", "month_sin")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var dayOfMonth = epochDay().getDayOfMonth(); // 1..31
                        double angle = 2 * Math.PI * (dayOfMonth - 1) / 31.0;
                        return (float) Math.sin(angle);
                    }).freqRes(0);

                    // Sense: Day of Month (1-31) - Cosine
                    sense($.inh(id, $.p("dayOfMonth", "month_cos")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var dayOfMonth = epochDay().getDayOfMonth(); // 1..31
                        double angle = 2 * Math.PI * (dayOfMonth - 1) / 31.0;
                        return (float) Math.cos(angle);
                    }).freqRes(0);

                    // Sense: Month of Year (1-12) - Sine
                    sense($.inh(id, $.p("monthOfYear", "year_sin")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var monthOfYear = epochDay().get(ChronoField.MONTH_OF_YEAR); // 1..12
                        double angle = 2 * Math.PI * (monthOfYear - 1) / 12.0;
                        return (float) Math.sin(angle);
                    }).freqRes(0);

                    // Sense: Month of Year (1-12) - Cosine
                    sense($.inh(id, $.p("monthOfYear", "year_cos")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var monthOfYear = epochDay().get(ChronoField.MONTH_OF_YEAR); // 1..12
                        double angle = 2 * Math.PI * (monthOfYear - 1) / 12.0;
                        return (float) Math.cos(angle);
                    }).freqRes(0);

                    // Optional: Year encoding (if trainCalendarYear is true)
                    if (trainCalendarYear) {
                        // Sense: Year - Sine
                        sense($.inh(id, $.p("year", "history_sin")), () -> {
                            if (t < 0) return Float.NaN; // Uninitialized
                            var year = epochDay().getYear(); // e.g., 2012..2023
                            double normalizedYear = Util.normalize(year, historyYearMin, historyYearMax);
                            double angle = 2 * Math.PI * (normalizedYear - Math.floor(normalizedYear));
                            return (float) Math.sin(angle);
                        }).freqRes(0);

                        // Sense: Year - Cosine
                        sense($.inh(id, $.p("year", "history_cos")), () -> {
                            if (t < 0) return Float.NaN; // Uninitialized
                            var year = epochDay().getYear(); // e.g., 2012..2023
                            double normalizedYear = Util.normalize(year, historyYearMin, historyYearMax);
                            double angle = 2 * Math.PI * (normalizedYear - Math.floor(normalizedYear));
                            return (float) Math.cos(angle);
                        }).freqRes(0);
                    }

                } else {
                    /* NON-SINUSOIDAL TIME ENCODING */

                    // Existing non-sinusoidal sensors
                    sense($.inh(id, $.p("day", "week")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var dayOfWeek = epochDay().get(ChronoField.DAY_OF_WEEK); // 1..7
                        return (dayOfWeek - 1) / (7f - 1);
                    }).freqRes(0);

                    sense($.inh(id, $.p("day", "month")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var dayOfMonth = epochDay().getDayOfMonth(); // 1..31
                        return (dayOfMonth - 1) / (31f - 1); // HACK
                    }).freqRes(0);

                    sense($.inh(id, $.p("month", "year")), () -> {
                        if (t < 0) return Float.NaN; // Uninitialized
                        var monthOfYear = epochDay().get(ChronoField.MONTH_OF_YEAR); // 1..12
                        return (monthOfYear - 1) / (12f - 1);
                    }).freqRes(0);
                }
            }

            /* Buy/Sell Actions */
            for (var i = 0; i < portfolioSize; i++) {
                var I = i;
                final var s = portfolio.get(I);
                if (buySellSeparate) {
                    var B = action($.inh($.p(id, s.id), "buy"), x -> {
                        if (!Double.isFinite(s.price))
                            x = 0; // Symbol doesn't exist
                        buy[I] = x;
                        return (float) buyActual[I];
                    }, z -> (float) buyActual[I]);

                    var S = action($.inh($.p(id, s.id), "sell"), x -> {
                        if (!Double.isFinite(s.price))
                            x = 0; // Symbol doesn't exist
                        sell[I] = x;
                        return (float) sellActual[I];
                    }, z -> (float) sellActual[I]);
                } else {
                    final var UNCHANGED = 0.5f;
                    action($.inh($.p(id, s.id), "own"), x -> {
                        if (!Double.isFinite(s.price))
                            return UNCHANGED; // Symbol doesn't exist

                        double fraction = Fuzzy.polarize(x); // Map action to [-1, +1]
                        if (fraction >= 0) {
                            if (s.price <= 0) {
                                return nop(I, UNCHANGED);
                            } else {
                                buy[I] = fraction;
                                sell[I] = 0;
                            }
                        } else {
                            if (s == null || s.owns(a) <= 0) {
                                return nop(I, UNCHANGED);
                            } else {
                                sell[I] = -fraction;
                                buy[I] = 0;
                            }
                        }
                        return x;
                    }, z ->
                            (float) ownActual[I]);
                }
            }

            /* Reward Computation and Normalization */
            var normalizedDiff = new FloatNormalized(
                    new FloatDifference(() -> {
                        double currentValue = value();
                        if (Double.isNaN(currentValue)) {
                            return Float.NaN;
                        }
                        return (float) currentValue;
                    }, FloatDifference.Mode.PctChange, this::time) {
                        @Override
                        public float asFloat() {
                            double v = super.asFloat();
                            valueChange = v;

                            // Clamp the value to prevent extreme spikes
                            return (float) Util.clampSafe(v, -1, +1);
                        }
                    },
                    0,
                    rewardNormPeriod
            ).polar();
            reward($.inh(id, "value"), normalizedDiff); //.resolution(0);

            // rewardNormalized($.inh(id, "value"), ()-> (float) value()); //absolute value

            onFrame(this::pre);
            afterFrame(this::post);

            view = new MarketPerformance();
            view2 = new MarketPerformanceCloud();
            window(new Stacking(view2, view), 600, 600);
        }

        private float nop(int I, float UNCHANGED) {
            buy[I] = sell[I] = 0;
            ownActual[I] = 0.5f;
            return UNCHANGED;
        }

        private LocalDate epochDay() {
            return LocalDate.ofEpochDay(t / (24 * 60 * 60));
        }

        private void addEvent(long when, Consumer<Market> e) {
            events.merge(when, e, Consumer::andThen);
        }

        /**
         * Gather the buy/sell vectors and execute trades with proper normalization.
         */
        private void pre() {
            var now = t;
            var prev = now;

            if (reset == 0) {
                var episodeFinished = now == Long.MIN_VALUE || now >= marketEnd;
                if (episodeFinished) {
                    episodeFinish();

                    reset();
                    prev = now = t;
                } else {
                    now = t = now + cycleSeconds;
                }
            } else {
                --reset;
            }

            events.subMap(prev, false, now, true)
                    .forEach((k, v) -> v.accept(Market.this));

            valueBefore = value();
        }

        private long timeFocus() {
            //return r.nextLong(START, END); //FLAT
            return Util.lerpLong((float) (1 - Math.pow(rng().nextFloat(), 2)), START, END); // Biased towards future
        }

        /**
         * Corrected post() method with normalization
         */
        private void post() {
            var B = new ArrayRealVector(buy);
            var S = new ArrayRealVector(sell);

            BUYING.clear();
            SELLING.clear();

            var all = a.value(Market.this); // Entire portfolio value

            var buyAmountMax = buyRateMax * all;
            var sellAmountMax = sellRateMax * all;

            // ---- Normalization Step for Buy Signals ----
            // Sum all buy fractions
            double totalBuyFraction = 0.0;
            for (int i = 0; i < buy.length; i++) {
                totalBuyFraction += B.getEntry(i);
            }

            // If total buy fraction exceeds 1, scale down each buy fraction proportionally
            if (totalBuyFraction > 1.0) {
                double scale = 1.0 / totalBuyFraction;
                for (int i = 0; i < buy.length; i++) {
                    buy[i] = (float) (B.getEntry(i) * scale);
                }
                B = new ArrayRealVector(buy);
            }
            // ---------------------------------------------

            // ---- Normalization Step for Sell Signals ----
            // Sum all sell fractions
            double totalSellFraction = 0.0;
            for (int i = 0; i < sell.length; i++)
                totalSellFraction += S.getEntry(i);

            // If total sell fraction exceeds 1, scale down each sell fraction proportionally
            if (totalSellFraction > 1.0) {
                double scale = 1.0 / totalSellFraction;
                for (int i = 0; i < sell.length; i++)
                    sell[i] = (float) (S.getEntry(i) * scale);

                S = new ArrayRealVector(sell);
            }
            // ----------------------------------------------

            // Now, calculate buy/sell amounts based on normalized fractions
            for (int i = 0, portfolioSize = portfolio.size(); i < portfolioSize; i++) {
                var x = portfolio.get(i);
                var xPrice = x.price;
                var xBuyAmount = buyAmountMax * B.getEntry(i);
                var xSellAmount = sellAmountMax * S.getEntry(i);
                if (xBuyAmount >= xSellAmount) {
                    double q = (xBuyAmount - xSellAmount) / xPrice;
                    if (q > 0)
                        BUYING.put(i, q);
                } else {
                    double q = (xSellAmount - xBuyAmount) / xPrice;
                    if (q > 0)
                        SELLING.put(i, q);
                }
            }

            Arrays.fill(buyActual, 0);
            Arrays.fill(sellActual, 0);
            Arrays.fill(ownActual, 0.5f);

            // Sell first to liquidate to cash before buying
            SELLING.forEach(new BiConsumer<>() {
                int k = 0;

                @Override
                public void accept(Integer n, Double shares) {
                    var x = portfolio.get(n);
                    double S;
                    double sold;
                    if ((sold = sell(x, shares, a, feePerVolume)) > 0) {
                        if (trace)
                            System.out.println("\tsell " + x.id + " * " + n4(sold));
                        S = (sold * x.price) / sellAmountMax;
                    } else
                        S = 0;
                    sellActual[n] = S;
                    if (S > 0)
                        ownActual[n] = Fuzzy.unpolarize(-S);
                }
            });

            // Then buy with normalized fractions
            BUYING.forEach(new BiConsumer<>() {
                int k = 0;

                @Override
                public void accept(Integer n, Double shares) {
                    var x = portfolio.get(n);
                    double B_val;
                    double bought;
                    if ((bought = buy(x, shares, a, feePerVolume)) > 0) {
                        if (trace)
                            System.out.println("\tbuy " + x.id + " * " + n4(bought));
                        B_val = (bought * x.price) / buyAmountMax;
                    } else
                        B_val = 0;

                    buyActual[n] = B_val; // Corrected index from k to n
                    if(B_val > 0)
                        ownActual[n] = Fuzzy.unpolarize(B_val - 0);
                }
            });

            var p = performance();

            var valueAfter = p.value;
            if (Double.isFinite(valueAfter)) {
                if (trace)
                    System.out.println(t + "\tvalue=" + n4(valueAfter));
                view.add(p);
                view2.add(p);
            }

            iteration++;
        }

        /**
         * Buys fractional shares.
         *
         * @param shares The number of shares to buy (can be fractional).
         * @param a The account performing the purchase.
         * @return The number of shares successfully bought.
         */
        public synchronized double buy(Stock x, double shares, Account a, double feePerVolume) {
            if (!Double.isFinite(x.price)) {
                // Invalid price, cannot execute transaction
                return 0;
            }
            shares = Util.round(shares, x.minShare);
            if (shares <= 0)
                return 0; // Below minimum share threshold

            double vol = shares * x.price;
            if (a.cash < vol) {
                // Insufficient cash, this should not happen due to normalization
                // But as a safety net, we attempt to buy as much as possible
                double sharesBuyable = Util.round((buyRateMax * a.cash) / x.price, x.minShare);
                if (sharesBuyable <= 0) return 0;
                // Execute partial purchase
                x.own.merge(a, sharesBuyable, Double::sum);
                a.cash += -sharesBuyable * x.price - (sharesBuyable * x.price * feePerVolume);
                return sharesBuyable;
            }
            // Execute purchase
            x.own.merge(a, shares, Double::sum);
            a.cash += -vol - (vol * feePerVolume);
            return shares;
        }

        /**
         * Sells fractional shares.
         *
         * @param shares The number of shares to sell (can be fractional).
         * @param a The account performing the sale.
         * @return The number of shares successfully sold.
         */
        public synchronized double sell(Stock x, double shares, Account a, double feePerVolume) {
            if (!Double.isFinite(x.price)) {
                // Invalid price, cannot execute transaction
                return 0;
            }
            shares = Util.round(shares, x.minShare);
            if (shares <= 0)
                return 0; // Below minimum share threshold

            double ownedShares = x.owns(a);
            if (ownedShares < shares) {
                // Not enough shares to sell
                return 0;
            }
            // Execute sale
            x.own.put(a, ownedShares - shares);
            double vol = shares * x.price;
            a.cash += vol - (vol * feePerVolume);
            return shares;
        }

        private Performance performance() {
            return new Performance(t, value(), valueChange, iteration);
        }

        /**
         * Called at end of episode to track performance, and reset
         */
        private void episodeFinish() {
            if (trace || traceEpisode)
                a.print(Market.this, System.out);
        }

        private void reset() {
            sym.values().forEach(s -> {
                s.price = 0;
                s.volume = 0;
            });

            t = Long.MIN_VALUE; // TODO random
            a = new Account();
            a.cash = initialCash;
            reset = RESET_PERIOD;

            /** Choose two points for an interval */
            var s = timeFocus();
            var e = timeFocus();
            if (s > e) {
                var se = e;
                e = s;
                s = se;
            }
            t = s;
            marketEnd = e;
        }

        private double value() {
            if (reset > 0)
                return Double.NaN;
            else
                return a.value(Market.this);
        }

        /**
         * Vertical zoom
         */
        float vcMax = 0.1f;

        private float vy(double vc) {
            return Math.min(1, (float) Util.normalize(vc, -vcMax, +vcMax));
        }

        class MarketPerformanceCloud extends Surface {
            static final float performanceSamplingRate = 0.02f;
            static final int performanceSamplingCapacity = 512;
            final Deque<Performance> history = new ArrayDeque<>(performanceSamplingCapacity);

            @Override
            protected void render(ReSurface r) {
                var pr = 4 * (Math.min(w(), h()) / (1 + performanceSamplingCapacity));
                final var w = w(); // - 2 * pr;
                final var h = h(); // - 2 * pr;

                float timeRange = END - START;
                for (var p : history) {
                    final var pv = p.valueChange;
                    if (!Double.isFinite(pv)) continue;

                    var x = (p.time - START) / timeRange * w;
                    var y = vy(pv) * h;
                    r.gl.glPushMatrix();
                    r.gl.glTranslatef(x, y, 0);

                    var alpha = 1 / (1 + (iteration - p.iteration) / 5000f);

                    float re, g, bl;
                    if (p.valueChange >= 0) {
                        g = y;
                        re = 0;
                        bl = 0.5f;
                    } else {
                        re = y;
                        g = 0;
                        bl = 0.5f;
                    }
                    r.gl.glColor4f(re, g, bl, 0.25f * alpha);
                    Draw.poly(6, pr, true, r.gl);
                    r.gl.glPopMatrix();
                }
            }

            public void add(Performance p) {
                if (!rng().nextBoolean(performanceSamplingRate))
                    return;

                while (history.size() >= performanceSamplingCapacity)
                    history.removeFirst();
                history.add(p);
            }

        }

        class MarketPerformance extends Surface {

            static final int EWMA_BARS = 192;
            final FloatMeanEwma[] bars = Util.arrayOf(i -> new FloatMeanEwma().reset(0)
                    .period(Math.round(19000f / EWMA_BARS)), new FloatMeanEwma[EWMA_BARS]);

            public void add(Performance p) {
                if (Double.isFinite(p.valueChange)) {
                    var b = Util.bin((p.time - START) / ((float) (END - START)), bars.length);
                    bars[b].accept(p.valueChange);
                }
            }

            @Override
            protected void render(ReSurface r) {
                final float w = w(), h = h(); // - 2 * pr;

                var i = 0;
                for (var b : bars) {
                    var x = b.meanFloat();
                    var dx = 1 / (bars.length - 1f);
                    if (Float.isFinite(x)) {
                        var y = vy(x);

                        r.gl.glColor4f(x < 0 ? 1 : 0, x >= 0 ? 1 : 0, 0, 0.9f);

                        float xStart = i * dx, xEnd = xStart + dx;
                        float y1, y2;
                        if (x >= 0) {
                            y1 = 0.5f + (y - 0.5f);
                            y2 = 0.5f;
                        } else {
                            y1 = 0.5f;
                            y2 = 0.5f + (y - 0.5f);
                        }
                        Draw.rect(xStart * w, y1 * h, (xEnd - xStart) * w, (y2 - y1) * h, r.gl);
                    }
                    i++;
                }
            }

        }

    }


}
