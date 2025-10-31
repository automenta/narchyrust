/*
 * Variable.java
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
package nars.term.var;


import nars.NAL;
import nars.Op;
import nars.term.atom.IntrinAtomic;

import static nars.Op.*;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public final class NormalizedVariable extends Variable implements IntrinAtomic {

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    private static final NormalizedVariable[][] varCache = new NormalizedVariable[4][NAL.term.MAX_INTERNED_VARS];

    static {
        for (Op o : new Op[]{VAR_PATTERN, VAR_QUERY, VAR_DEP, VAR_INDEP}) {
            int t = opToVarIndex(o);
            for (byte i = 1; i < NAL.term.MAX_INTERNED_VARS; i++)
                varCache[t][i] = vNew(o, i);
        }
    }

    private NormalizedVariable(Op o, byte num) {
        super(o, num);
    }

    @Override
    public short intrin() {
        return (short)hash;
    }

    private static int opToVarIndex(Op o) {
        return opToVarIndex(o.id);
    }

    private static int opToVarIndex(byte oid) {
        return oid - VAR_PATTERN.id /* lowest, most specific */;
//        //TODO verify this is consistent with the variable's natural ordering
//        switch (o) {
//            case VAR_DEP:
//                return 0;
//            case VAR_INDEP:
//                return 1;
//            case VAR_QUERY:
//                return 2;
//            case VAR_PATTERN:
//                return 3;
//            default:
//                throw new UnsupportedOperationException();
//        }
    }

    /**
     * TODO move this to TermBuilder
     */
    private static NormalizedVariable vNew(Op type, byte id) {
        return new NormalizedVariable(switch (type) {
            case VAR_PATTERN -> VAR_PATTERN;
            case VAR_QUERY -> VAR_QUERY;
            case VAR_DEP -> VAR_DEP;
            case VAR_INDEP -> VAR_INDEP;
            default -> throw new UnsupportedOperationException();
        }, id);
    }

    public static NormalizedVariable varNorm(Op op, byte id) {
        return varNorm(op.id, id);
    }

    public static NormalizedVariable varNorm(byte op, int id) {
        return varNorm(op, (byte)id);
    }
    
    public static NormalizedVariable varNorm(byte op, byte id) {
        //assert(id > 0);
        return id < NAL.term.MAX_INTERNED_VARS ? varCached(op, id) : varNew(op, id);
    }

    private static NormalizedVariable varNew(byte op, byte id) {
        return vNew(Op.op(op), id);
    }

    private static NormalizedVariable varCached(byte op, int id) {
        return varCache[opToVarIndex(op)][id];
    }

    @Override
    public String toString() {
        return op().ch + Integer.toString(id());
    }

    @Override
    public final boolean equals(Object x) {
        return x == this; //TODO see NAL.term.MAX_INTERNED_VARS
//                   ||
//              (obj instanceof AnonID) && id==((AnonID)obj).id;
    }
}