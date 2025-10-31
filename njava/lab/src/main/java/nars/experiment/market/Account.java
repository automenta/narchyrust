package nars.experiment.market;

import java.io.PrintStream;
import java.io.Serializable;
import java.text.DecimalFormat;

import static jcog.Str.n4;

public class Account implements Serializable {
    private final String name;
    public double cash;

    public Account() {
        this(null, 0);
    }

    private Account(String name, double cash) {
        this.name = name != null ? name :
                Integer.toString(System.identityHashCode(this), 36);
        this.cash = cash;
    }

    private static DecimalFormat df() {
        return new DecimalFormat("#.00");
    }

    @Override
    public String toString() {
        return name;
    }

    public double value(Market m) {
        return m.value(this) + cash;
    }

    public void print(Market m, PrintStream out) {
        out.println("Portfolio\tvalue=" + value(m));
        //TODO print profit, and profit rate (profit/time)
        //TODO count fees in its own accumulator
        out.println("Cash: " + cash);
        out.println("Shares:");
        m.sym.values().forEach(s -> {
            double o = s.owns(this);
            if (o > 0)
                out.println("\t" + s + ": " + n4(o));
        });
        out.println();
    }
}