package nars.experiment.market;

import jcog.Str;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Stock {

    public final String id;

    /**
     * total shares
     */
    private final double total;

    /**
     * share price
     */
    public double price; //, dayChange, yearHigh, yearLow;

    /**
     * available shares
     */
    public double available;

    /**
     * Define minimum share quantity based on the asset.
     * For example, Bitcoin uses satoshis (1e-8).
     */
    double minShare = 1;

    /**
     * owner -> owned shares
     */
    final Map<Account, Double> own = new ConcurrentHashMap<>(); // Changed to Double for fractional shares

    public double volume;


    public Stock(String id) {
        this.id = id;
        this.price = Double.NaN;
        this.total = Integer.MAX_VALUE;
        this.available = total;
    }

    @Override
    public String toString() {
        return id + "@" + Str.n2(price);
    }



    public double owns(Account a) {
        return own.getOrDefault(a, 0.0);
    }

}