//package nars.term.compound;
//
//import nars.Op;
//import nars.subterm.Subterms;
//import nars.term.Term;
//import org.jetbrains.annotations.Nullable;
//
///**
// * seems used only if op==CONJ
// */
//@Deprecated
//public abstract class PatternCompound extends CachedCompound.TemporalCachedCompound {
//
//    PatternCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {
//        super(op, dt, subterms);
//    }
//
////    /*@NotNull*/
////    public static PatternCompound ellipsis(/*@NotNull*/ Compound seed, /*@NotNull*/ Subterms v, /*@NotNull*/ Ellipsis e) {
////        Op op = seed.op();
////        int dt = seed.dt();
////
////        if ((op.commutative)) {
////            return new PatternCompoundEllipsisCommutive(op, dt, e, v);
////        } else {
////            //return PatternCompoundEllipsisLinear.the(op, dt, e, v);
////            throw new UnsupportedOperationException();
////        }
////
////    }
//
//    @Override
//    public final boolean the() {
//        return false;
//    }
//
//    @Override
//    public final @Nullable Term normalize(byte varOffset) {
//        throw new UnsupportedOperationException("normalize before patternify");
//    }
//
////    @Deprecated public abstract static class PatternCompoundWithEllipsis extends PatternCompound {
////
////        final Ellipsis ellipsis;
////
////        PatternCompoundWithEllipsis(/*@NotNull*/ Op seed, int dt, Ellipsis ellipsis, Subterms subterms) {
////            super(seed, dt, subterms);
////            this.ellipsis = ellipsis;
////        }
////
////        @Deprecated public abstract boolean unifyWithEllipsis(Compound y, Unify u);
////    }
//
//
////    public static final class PatternCompoundEllipsisLinear extends PatternCompoundWithEllipsis {
////
////        public static PatternCompoundEllipsisLinear the(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
//////            if (op.statement) {
//////                //HACK
//////                Term x = subterms.sub(0);
//////                Term y = subterms.sub(1);
//////                if (x instanceof Ellipsislike) {
//////                    //raw ellipsis, the conjunction got removed somewhere. HACK re-add it
//////                    x = CONJ.the(x);
//////                }
//////                if (y instanceof Ellipsislike) {
//////                    //raw ellipsis, the conjunction got removed somewhere. HACK re-add it
//////                    y = CONJ.the(y);
//////                }
//////                subterms = new BiSubterm(x, y); //avoid interning
//////            }
////            return new PatternCompoundEllipsisLinear(op, dt, ellipsis, subterms);
////        }
////
////        private PatternCompoundEllipsisLinear(/*@NotNull*/ Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
////            super(op, dt, ellipsis, subterms);
////            if (op.statement && subterms.OR(x -> x instanceof Ellipsislike))
////                throw new WTF("raw ellipsis subj/pred makes no sense here");
////        }
////
////        /**
////         * non-commutive compound match
////         * X will contain at least one ellipsis
////         * <p>
////         * match subterms in sequence
////         * <p>
////         * WARNING this implementation only works if there is one ellipse in the subterms
////         * this is not tested for either
////         */
////        @Override
////        public boolean unifySubterms(Compound y, Unify u) {
////            Subterms Y = y.subterms();
////            int xi = 0, yi = 0;
////            int xsize = subs();
////            int ysize = Y.subs();
////
////
////            while (xi < xsize) {
////                Term x = sub(xi++);
////
////                if (x instanceof Ellipsis) {
////                    int available = ysize - yi;
////
////                    Term xResolved = u.resolveTerm(x);
////                    if (xResolved == x) {
////
////
////                        if (xi == xsize) {
////                            //the ellipsis is at the right edge so capture the remainder
////                            return
////                                    ellipsis.validSize(available) &&
////                                    ellipsis.unify(Fragment.fragment(Y, yi, yi + available), u);
////
////
////                        } else {
////                            //TODO ellipsis is in the center or beginning
////                            throw new TODO();
////                        }
////                    } else {
////                        if (xResolved instanceof Fragment) {
////                            Fragment xe = (Fragment) xResolved;
////                            if (!xe.linearMatch(Y, yi, u))
////                                return false;
////                            yi += xe.subs();
////                        } else {
////                            if (!sub(yi).unify(xResolved, u))
////                                yi++;
////                        }
////                    }
////
////
////                } else {
////                    if (ysize <= yi || !x.unify(Y.sub(yi++), u))
////                        return false;
////                }
////            }
////
////            return true;
////        }
////
////
////    }
//
//
////    public static final class PatternCompoundEllipsisCommutive extends PatternCompoundWithEllipsis {
////
////
////        PatternCompoundEllipsisCommutive(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
////            super(op, dt, ellipsis, subterms);
////        }
////
////        /**
////         * commutive compound match: Y into X which contains one ellipsis
////         * <p>
////         * X pattern contains:
////         * <p>
////         * one unmatched ellipsis (identified)
////         * <p>
////         * <p>
////         * zero or more "constant" (non-pattern var) terms
////         * all of which Y must containFunction<(\w+),\1>
////         * <p>
////         * zero or more (non-ellipsis) pattern variables,
////         * each of which may be matched or not.
////         * matched variables whose resolved values that Y must contain
////         * unmatched variables determine the amount of permutations/combinations:
////         * <p>
////         * if the number of matches available to the ellipse is incompatible with the ellipse requirements, fail
////         * <p>
////         * (total eligible terms) Choose (total - #normal variables)
////         * these are then matched in revertable frames.
////         * <p>
////         * *        proceed to collect the remaining zero or more terms as the ellipse's match using a predicate filter
////         *
////         * @param Y the compound being matched to this
////         */
////        @Override
////        public boolean unifyWithEllipsis(Compound Y, Unify u) {
////            //            if ((dt != XTERNAL) && Y.TEMPORAL() && !Y.isCommutative())
//////                throw new TODO();
////
////
////            //                @Nullable Versioned<MatchConstraint> uc = u.constraints(ellipsis);
////
////            //xFixed is effectively sorte unless eMatch!=nulld
////
////
////
////            //uc==null ? y.toSetSorted() : y.toSetSorted(yy -> MatchConstraint.valid(yy, uc, u));
////            //y.toSetSorted();
////            boolean seq = CONJ() && dt() == XTERNAL && Conj.isSeq(Y);
////            SortedSet<Term> yFree = seq ? Y.eventSet() : Y.subterms().toSetSorted(u::resolveTerm);
////
////            Subterms xx = subterms();
////            SortedSet<Term> xMatch = xx.toSetSorted();
////
////            Ellipsis ellipsis = this.ellipsis;
////            boolean finished = false;
////            for (Iterator<Term> iterator = xMatch.iterator(); iterator.hasNext(); ) {
////                Term xk = iterator.next();
////
////                Term xxk = u.resolveTermRecurse(xk);
////
////                if (xk.equals(ellipsis)) {
////                    if (xxk.equals(xk))
////                        continue; //unassigned ellipsis
////
////                    ellipsis = null;
//////                    if (xxk instanceof Fragment) {
//////                        for (Term ex : xxk.subterms()) {
//////                            if (!include(ex, xMatch, yFree, u)) {
//////                                finished = true;
//////                                break;
//////                            } //TODO else: something got eliminated
//////                        }
//////                        if (finished) break;
//////                        continue;
//////                    }
//////                    //else it is ellipsis that matched a single term, continue below:
////
////                }
////
////                //test for necessary constant subterm
////                if (!u.varIn(xxk)) {
////                    if (include(xxk, yFree, u)) {
////                        iterator.remove(); //eliminated
////                    } else {
////                        finished = true;
////                        break;
////                    }
////                }
////
////            }
////            boolean result = false;
////            if (!finished) {
////                int xs = xMatch.size();
////                int ys = yFree.size();
////
////                if (ellipsis == null) {
////                    //ellipsis assigned already; match the remainder as usual
////                    if (xs == ys) {
////                        switch (xs) {
////                            case 0:
////                                result = true;
////                                finished = true;
////                                break;
////                            case 1:
////                                result = xMatch.first().unify(yFree.first(), u);
////                                finished = true;
////                                break;
////                            default:
////                                result = Unifier.unifyCommute(new TermList(xMatch), new TermList(yFree), u);
////                                finished = true;
////                                break;
////                        }
////                    } else {
////                        //arity mismatch
////                        finished = true;
////                    }
////                }
////                if (!finished) {
////                    int numRemainingForEllipsis = ys - xs;
////                    if (ellipsis.validSize(numRemainingForEllipsis)) {
////                        if (xs > 0 && ys > 0) {
////                            int vars = u.vars;
////                            int dur = u.dur;
////                            //test matches against the one constant target
////                            for (Iterator<Term> xi = xMatch.iterator(); xi.hasNext(); ) {
////                                Term ix = xi.next();
////                                if (u.varIn(ix)) continue;
////
////                                boolean canMatch = false;
////                                Term onlyY = null;
////
////                                for (Term yy : yFree) {
////                                    if (Unify.isPossible(ix, yy, vars, dur)) {
////                                        canMatch = true;
////                                        if (onlyY == null)
////                                            onlyY = yy; //first found and only so far
////                                        else {
////                                            onlyY = null;
////                                            break; //found > 1 so stop
////                                        }
////                                    }
////                                }
////
////                                if (canMatch) {
////                                    if (onlyY != null) {
////                                        if (ix.unify(onlyY, u)) {
////                                            xi.remove();
////                                            yFree.remove(onlyY);
////                                            xs--;
////                                            ys--;
////                                        } else {
////                                            finished = true;
////                                            break;//impossible
////                                        }
////                                    } //else: continue
////
////                                } else {
////                                    finished = true;
////                                    break;//nothing from yFree could match xFixed
////                                }
////                            }
////                        }
////                        if (!finished) {
////                            switch (xs) {
////                                case 0:
////                                    result = ellipsis.unify(ys > 0 ? Fragment.fragment(yFree) : Fragment.EmptyFragment, u);
////                                    break;
////
////                                case 1:
////                                    Term xMatchFirst = xMatch.first();
////                                    if (xs == ys) {
////                                        result = xMatchFirst.unify(yFree.first(), u) && ellipsis.unify(Fragment.EmptyFragment, u);
////                                    } else {
////                                        //no matches possible but need one
////                                        if (ys >= 1) {
////                                            Termutator t = Choose1.choose1(ellipsis, xMatchFirst, yFree, u);
////                                            if (t != null) {
////                                                if (t != Termutator.ELIDE)
////                                                    u.termute(t);
////                                                result = true;
////                                            }
////                                        }
////                                    }
////                                    break;
////
////                                case 2: {
////                                    if (ys >= 2) {
////                                        Termutator t = Choose2.choose2(ellipsis, xMatch, yFree, u);
////                                        if (t != null) {
////                                            if (t != Termutator.ELIDE)
////                                                u.termute(t);
////                                            result = true;
////                                        }
////                                        break;
////                                    }
////                                    break;
////                                }
////
////                                default:
////                                    throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
////                            }
////                        }
////                    }
////
////
////                }
////
////            }
////
////
////            return result;
////        }
////
////        private static boolean include(Term x, SortedSet<Term> yFree, Unify u) {
////
////            if (!yFree.isEmpty()) {
////                boolean rem = yFree.remove(x);
////                if (rem) {
////                    return true;
////                }
////
////                if (x.TEMPORALABLE()) {
////                    for (Iterator<Term> iterator = yFree.iterator(); iterator.hasNext(); ) {
////                        Term y = iterator.next();
////                        if (!u.varIn(y)) {
////                            //at this point volume, structure, etc can be compared
////                            if (x.unify(y, u)) {
////                                iterator.remove();
////                                return true;
////                            }
////                        }
////                    }
////                }
////            }
////
////            return false;
////
////        }
////
////    }
//
//
//}
