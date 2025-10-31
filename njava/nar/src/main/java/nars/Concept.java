/*
 * Concept.java
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

import jcog.data.map.MetaMap;
import jcog.pri.Deleteable;
import nars.table.question.QuestionTable;
import nars.term.Termed;
import nars.term.util.TermException;

import java.io.IOException;
import java.util.stream.Stream;

import static nars.Op.TIMELESS;

public abstract class Concept implements Termed, MetaMap, Deleteable {
    public final Term term;

    public long creation = TIMELESS;

    protected Concept(Term x) {
        if (!x.CONCEPTUALIZABLE())
            throw new TermException(Op.UNCONCEPTUALIZABLE, x);

        this.term = x;
    }

    public abstract BeliefTable beliefs();

    public abstract BeliefTable goals();

    public abstract QuestionTable questions();

    public abstract QuestionTable quests();

    public Op op() { return term().op(); }
    public int opID() { return term().opID(); }


    public void print() {
        print(System.out);
    }
    public String printToString() {
        StringBuilder sb = new StringBuilder(1024);
        print(sb);
        return sb.toString();
    }


    public <A extends Appendable> A print(A out) {
        print(out, true, true);
        return out;
    }


    public static final String printIndent = "  \t";

    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    public void print(Appendable out, boolean showbeliefs, boolean showgoals) {

        try {
            out.append("concept: ").append(toString()).append('\t').append(getClass().toString()).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract Stream<NALTask> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests);

    public final Stream<NALTask> tasks() {
        return tasks(true,true,true,true);
    }


}