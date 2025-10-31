/*
 * TruthValue.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http:
 */
package nars;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Is;
import jcog.Str;
import jcog.Util;
import nars.truth.AbstractMutableTruth;
import nars.truth.PreciseTruth;
import nars.truth.Truthed;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;

import static jcog.WTF.WTF;


/**
 * scalar (1D) truth value "frequency", stored as a floating point value
 *
 * floating-point precision testing tool:
 *      http://herbie.uwplse.org/demo/
 */
@Is({"Four-valued_logic", "Bennett%27s_laws"}) public abstract class Truth implements Truthed {

    public static void assertDithered(@Nullable Truth t, NAL n) {
        if (t != null) {
            Truth d = t.dither(n);
            if (!t.equals(d))
                throw WTF("not dithered");
        }
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accurate to 1%
     */
    public static StringBuilder appendString(StringBuilder sb, int decimals, float freq, float conf) {

        sb.ensureCapacity(3 + 2 * (2 + decimals));

        return sb
            .append(Op.TRUTH_VALUE_MARK)
            .append(Str.n(freq, decimals))
            .append(Op.VALUE_SEPARATOR)
            .append(Str.n(conf, decimals))
            .append(Op.TRUTH_VALUE_MARK);
    }


    public static float freq(float f, NAL n) {
        return freq(f, n.freqRes.floatValue());
    }
    public static double conf(double c, NAL n) {
        return conf(c, n.confRes.floatValue());
    }

    public static float freq(float f, float res) {
        return Util.unitizeSafe(Util.round(f, res));
    }

    public static double conf(double c, double res) {
		return Util.clampSafe(
                Util.round(c, res)
        , NAL.truth.CONF_DITHER_ROUND_UP ? res : 0, NAL.truth.CONF_MAX);
    }

    public static void write(Truth t, ByteArrayDataOutput o)  {
        o.writeFloat(t.freq());
		o.writeFloat((float) t.conf());
    }

    public static Truth read(DataInput in) throws IOException {
        float f = in.readFloat();
        float c = in.readFloat();
        return $.t(f, c);
    }

    /** even if the truths are .equals equivalent, but have different .evi() */
    public static Truth stronger(Truth x, Truth y) {
        return x==y || x.evi() >= y.evi() ? x : y;
    }

    public static float freqPN(float f, boolean neg) {
        return neg ? 1 - f : f;
    }

    private StringBuilder appendString(StringBuilder sb) {
		return appendString(sb, 2, freq(), (float) conf());
    }

    protected String truthString() {
        return appendString(new StringBuilder(7)).toString();
    }


    /**
     * the negated (1 - freq) of this truth value
     */
    public Truth neg() {
        return PreciseTruth.byEvi(freqNeg(), evi());
    }

    public boolean equals(@Nullable Truth x, float freqRes, double confRes) {
		return x == this || (x != null
            && equalConf(x, confRes)
            && equalFreq(x, freqRes)
        );
    }

    /** TODO actual TruthCurve equality beyond just hashcode=freq,evi */
    @Override public final boolean equals(Object that) {
        if (this instanceof AbstractMutableTruth || that instanceof AbstractMutableTruth)
            throw new UnsupportedOperationException();

        return this == that
            ||
            (that instanceof Truth t && hashCode() == t.hashCode());
    }

    @Override abstract public int hashCode();

    public boolean equalConf(Truth x, double confRes) {
        return equalConf(x.conf(), confRes);
    }
    public boolean equalEvi(Truth x, double res) {
        return Util.equals(evi(), x.evi(), res);
    }

    public boolean equalConf(double xc, double confRes) {
        return Util.equals(conf(), xc, confRes);
    }

    public boolean equalFreq(@Nullable Truth x, float freqRes) {
        return equalFreq(x.freq(), freqRes);
    }

    public boolean equalFreq(@Nullable Truth x, boolean neg, float freqRes) {
        return equalFreq(x.freqNegIf(neg), freqRes);
    }

    public boolean equalFreq(float xf, float freqRes) {
        return Util.equals(freq(), xf, freqRes);
    }

    public final boolean equals(@Nullable Truth x, float res) {
        return equals(x, res, res);
    }

//    public boolean equals(Truth x, @Nullable NAL n) {
//        return n == null ? equals(x) : equals(x, n.freqRes.floatValue(), n.confRes.floatValue());
//    }

    public boolean equalsStronger(@Nullable Truth x, boolean neg, NAL nar) {
        return (!neg && this == x) ||
               (equalFreq(x, neg, nar) && gteConf(x, nar));
    }

    public boolean equalFreq(@Nullable Truth x, boolean neg, NAL nar) {
        return (!neg && this == x) ||
               equalFreq(x, neg, nar.freqRes.floatValue());
    }

    public boolean gteConf(@Nullable Truth x, NAL nar) {
        return this==x || gteConf(x, nar.confRes.floatValue());
    }

    /** conf >= x.conf, within tolerance */
    public boolean gteConf(@Nullable Truth x, double confRes) {
        return conf() + confRes > x.conf()/2;
    }

    public Truth negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @Nullable public final Truth dither(NAL n) {
        return dither(n.freqRes.floatValue(), n.confRes.floatValue());
    }

    @Nullable public final Truth dither(float freqRes, double confRes) {
        return PreciseTruth.byConf(freq(freq(), freqRes), conf(conf(), confRes));
        //return PreciseTruth.byEvi(freq(freq(), freqRes), evi(evi(), confRes));
    }

    public @Nullable Truth cloneEviMult(double eFactor, double eviMin) {
        double e = evi() * eFactor;
        if (e < eviMin) return null;
        return PreciseTruth.byEvi(freq(), e);
    }

    public static class TruthException extends RuntimeException {
        public TruthException(String reason, double value) {
            super(reason + ": " + value);
        }
    }

}