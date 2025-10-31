package nars.deriver;

import com.google.common.base.Joiner;
import nars.*;
import nars.deriver.reaction.PatternReaction;
import nars.deriver.reaction.Reaction;
import nars.deriver.reaction.ReactionModel;
import nars.deriver.reaction.Reactions;
import nars.term.Compound;
import nars.term.control.AND;
import nars.term.control.PREDICATE;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jcog.Str.n4;
import static nars.$.$$;
import static nars.Op.XTERNAL;
import static nars.term.util.Testing.assertEq;
import static nars.term.util.transform.Retemporalize.patternify;
import static nars.term.util.transform.Retemporalize.retemporalizeAllToXTERNAL;
import static nars.unify.constraint.TermMatch.Conds;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/7/15.
 */
class PatternReactionTest {

    final NAR nar = NARS.shell();

    private static PatternReaction assertConcPattern(String r, String s) throws Narsese.NarseseException {
        PatternReaction p = parse(r);
        p.compile();
        assertEq(s, p.pattern);
        return p;
    }

    @Deprecated
    public static PatternReaction parse(String ruleSrc) throws Narsese.NarseseException {
        return new PatternReaction(TruthFunctions.the, ruleSrc, null);
    }

    @Test
    void eqGuards() throws Narsese.NarseseException {
        PatternReaction p = parse(
                "X, X  |- /\\X, (Punctuation:Ask)"
        );
        p.compile();
        String s = p.toString();
        assertTrue(s.contains("equal(task,belief)"));
    }

    @Test
    void inhGuards() throws Narsese.NarseseException {
        PatternReaction p = parse(
                "(M --> P), (S --> M)  |- (S --> P), (Belief:Deduction)"
        );
        p.compile();

        String s = p.toString();
        //System.out.println(s);
        assertTrue(s.contains("(belief(0),(NotRecursiveSubtermOf,task(1)))"));
        assertTrue(s.contains("SubtermEquality(((task,(0)),(belief,(1))))"));
        assertTrue(s.contains("(belief(0),(NotEqual,task(1)))"));
//        assertTrue(s.contains("(task(1)==>(NotRecursiveSubtermOf,belief(0)))"));

        //({(belief(0)==>(NotEqual,task(1))),(belief(0)==>(NotRecursiveSubtermOf,task(1))),
        // Is(taskTerm,"-->"),VolMin(taskTerm,3),Is(beliefTerm,"-->"),VolMin(beliefTerm,3),punc("."),(--,(overlap)),(double)},(((%1-->%2),(%3-->%1),()),(%3-->%2),(("."),Deduction,()),Task))
    }

    @Test
    void singleElementUnwrap() throws Narsese.NarseseException {
        PatternReaction r = parse(
                "((X) --> (Y)), ((X) --> (Y)) |- (X --> Y), (Belief:StructuralReduction, Goal:StructuralReduction)"
        );
        r.compile();
        System.out.println(Joiner.on("\n").join(r.conditionsSortedByCost()));
        //System.out.println(r);

    }

    @Test
    void simGuards() throws Narsese.NarseseException {
        PatternReaction r = parse(
                "(P --> M), (S --> M) |- (S <-> P), (Belief:Comparison)"
        );
        r.compile();
        System.out.println(r);
        //TODO test conditions
    }

    @Test
    void extractionTest() throws Narsese.NarseseException {
        PatternReaction r = parse(
                "B,  (--X ==> C), --is(X,\"--\"), condPN(X,B), neq(B,C)  |-   C,  (Punctuation:AskAsk, Time:TaskRel)"
        );
        r.compile();
        System.out.println(r);
        //TODO test conditions

    }
    @Test
    void condTest() {
        ReactionModel d = new Reactions().rules(
            "A, (X ==> B), cond(B,   A), --var(X), --eqPN(X,A), --eqPN(X,B) |-    X, (Goal:PostPP, Time:TaskRel)"
        ).compile(nar);
        d.print();
        System.out.println(d.what);
    }
    @Test
    void condTest2() {
        ReactionModel d = new Reactions().rules(
            "C, X,   seq(C), condFirst(C,  X) |-    X, (Belief:StructuralDeduction, Time:Task)"
        ).compile(nar);
        d.print();
        System.out.println(d.what);
    }

    @Test
    void condTest3() {
        ReactionModel d = new Reactions().rules(
            "C, X,   seq(C), condFirst(C,  X) |-    X, (Belief:StructuralDeduction, Time:Task)",
            "C, X,   seq(C), condFirst(C,--X) |-  --X, (Belief:StructuralDeduction, Time:Task)"
        ).compile(nar);
        d.print();
        System.out.println(d.what);
    }
    @Disabled @Test
    void dtUnify() throws Narsese.NarseseException {
        PatternReaction r = parse(
                "(M ==> C), (M ==> S), cond(C,  S), --var({M,S}), dtUnify(), --seq(C)   |- (M ==> condWithoutAny(C,  S)), (Belief:SupposePN,  Time:Belief)"
        );
        r.compile();
        List<PREDICATE<Deriver>> c = r.conditionsSortedByCost();
        for (var cc : c)
            System.out.println(n4(cc.cost()) + " " + cc);

    }
    @Disabled
    @Test
    void simpleNAL1Rule() throws Narsese.NarseseException {
        PatternReaction p = assertConcPattern(
                "(M --> P), (S --> M), neqRCom(S,P)  |- (S --> P), (Belief:Deduction)",
                "(%3-->%2)");
//        NAR n = NARS.shell();
//        Deriver q = new SerialDeriver(new Reactions().add(p).compile(n), n);
//        q.everyCycle(n.main());
//        NALPremise.the(NALTask.task($$("(m-->p)"), BELIEF, $.t(1, 0.9f), ETERNAL, ETERNAL, n.evidence()), NALTask.task($$("(s-->m)"), BELIEF, $.t(1, 0.9f), ETERNAL, ETERNAL, n.evidence()), false).run(q);
    }

    @Test
    void testPatternCompoundWithXTERNAL() throws Narsese.NarseseException {
        Compound p = (Compound) patternify($.$("((x) ==>+- (y))")).term();
        assertEquals(XTERNAL, p.dt());

    }

    @Test
    void testPatternTermConjHasXTERNAL() {
        Term p = patternify($$("(x && y)"));
        assertEquals("(x &&+- y)", p.toString());

        Term r = patternify($$("(x || y)"));
        assertEquals(
                //"(--,((--,x) &&+- (--,y)))",
                "(x ||+- y)",
                //"(x ||+- y)", //TODO
                r.toString());
    }

    @Test
    void testNoNormalization() throws Narsese.NarseseException {

        String a = "<x --> #1>";
        String b = "<y --> #1>";
        Term p = $.p(
                Narsese.term(a),
                Narsese.term(b)
        );
        String expect = "((x-->#1),(y-->#1))";
        assertEquals(expect, p.toString());


    }

    @Test
    void testParser() throws Narsese.NarseseException {


        assertNotNull(Narsese.term("<A --> b>"), "metaparser can is a superset of narsese");


        assertEquals(0, Narsese.term("#A").complexityConstants());
        assertEquals(1, Narsese.term("#A").complexity());
        assertEquals(0, Narsese.term("%A").complexityConstants());
        assertEquals(1, Narsese.term("%A").complexity());

        assertEquals(3, Narsese.term("<A --> B>").complexityConstants());
        assertEquals(1, Narsese.term("<%A --> %B>").complexityConstants());

        {


            Reaction x = parse("A, A |- (A,A), (Belief:Intersection)");
            assertNotNull(x);


        }


        int vv = 19;
        {


            Reaction x = parse("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Intersection, Goal:Intersection)");

            //assertEquals(vv, x.id.volume());


        }
        {


            Reaction x = parse("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Intersection, Goal:Intersection)");

            //assertEquals(vv, x.id.volume());

        }
//        {
//
//
//
//
//            PremiseRuleSource x = PremiseRuleSource.parse(" <A --> B>, <B --> A>, task(\"!\") |- <A <-> (A,B)>,  (Belief:Intersection, Punctuation:Question)");
//
//            assertEquals(25, x.id.volume());
//
//        }


        Reaction x = parse("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Desire)");


        //assertEquals(vv, x.id.volume());

    }

    @Test
    void testMinSubsRulePredicate() {


        ReactionModel d = compile("(A-->B),B,is(B,\"[\"),subsMin(B,2) |- (A-->dropAnySet(B)), (Belief:StructuralReduction)");
        assertNotNull(d);
    }

    @Test
    void MissingPatternVar() {
        assertThrows(Throwable.class,
                () -> new Reactions().rules("X,Y |- (X,Z), (Belief:Analogy)"));
    }

    @Test
    void testAutoXternalInConj() throws Narsese.NarseseException {
        assertConcPattern("X,Y |- (X && Y), (Belief:Analogy)", "(%1 &&+- %2)");
    }
    @Test
    void testAutoXternalInConj2() {

        //assertEq("(%1,(%1 &&+- %2))", $$("(%1,(%1 &&+- %2))").root());
        assertEq("(%1,(%1 &&+- %2))",
                retemporalizeAllToXTERNAL.apply($$("(%1,(%1 &&+- %2))")));
    }

    @Test
    void testAutoXternalInConj3() throws Narsese.NarseseException {
        //assertEq("(%1,(%1 &&+- %2))", $$("(%1,(%1 && %2))").root());

        assertEq("(%1,(%1 &&+- %2))",
                retemporalizeAllToXTERNAL.apply($$("(%1,(%1 && %2))")));


        assertConcPattern("X,Y |- (X,(X && Y)), (Belief:Analogy)", "(%1,(%1 &&+- %2))");
//        assertConcPattern("{X,%A..+},Y |- (&&,X,%A..+), (Belief:Analogy)", "(%1 &&+- %2..+)");
//        assertConcPattern("{%A..+},Y |- (&&,%A..+), (Belief:Analogy)", "( &&+- ,%1..+)");
    }

    @Test
    void deduplicateSame() {
        String s = "X,Y |- (X && Y), (Belief:Analogy)";
        Reactions r = new Reactions().rules(s, s);
        assertEquals(1, r.size());
    }

    @Test
    void NoXternalInSect() throws Narsese.NarseseException {


        assertConcPattern("(X,Y), Z |- (Z-->(X&&Y)), (Belief:Intersection)", "(%3-->(%1&&%2))");
        assertConcPattern("(X,Y), Z |- (Z-->(X||Y)), (Belief:Intersection)", "(--,(%3-->((--,%1)&&(--,%2))))");

        assertConcPattern("(X,Y), Z |- (((X&&Y)-->Z),X,Y), (Belief:Intersection)", "(((%1&&%2)-->%3),%1,%2)");
        assertConcPattern("(X,Y), Z |- (((X||Y)-->Z),X,Y), (Belief:Intersection)", "((--,(((--,%1)&&(--,%2))-->%3)),%1,%2)");

        assertConcPattern("(P --> M), (S --> M),  neq(S,P), notSetsOrDifferentSets(S,P) |- (term(\"&\",(polarizeTask(P),polarizeBelief(S))) --> M), (Belief:IntersectionDD, Time:TaskRel)",
                "(term(\"&\",(polarizeTask(%1),polarizeBelief(%3)))-->%2)");


    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBelief() {

        ReactionModel d = new Reactions().rules(
                "X,Y |- (X&&Y), (Belief:Intersection)",
                "X,(Y) |- (X&&Y), (Belief:Intersection)",
                "X,(Y,Y) |- (X&&Y), (Belief:Intersection)"
        ).compile(nar);

        d.print();

        assertTrue(d.what.toString().contains("(double)"), d.what::toString);
        assertFalse(d.what.toString().contains("(--,(double))"), d.what::toString);
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleGoal() {

        ReactionModel d = compile("X,Y |- (X&&Y), (Goal:Intersection)");

        assertTrue(d.what.toString().contains("(double)"));
        assertFalse(d.what.toString().contains("(--,(double))"), d.what::toString);
    }

    @Test
    void patternEternalInhConj() {

        ReactionModel d = compile("(C-->X),(C-->Y) |- (C-->(X&&Y)), (Goal:Intersection)");

        assertFalse(d.what.toString().contains("&&+-"), d.what::toString);

    }

    @Test
    void EventOfNegImpliesHasNeg() {
        assertRuleContains(  "X,Y,cond(X,--Y) |- Y, (Goal:Intersection)",
                Conds.name().toString()
        );
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBeliefOrGoal() {
        assertRuleContains("X,Y |- (X&&Y), (Belief:Intersection,Goal:Intersection)",
                "(double)"
                //"punc({\"!\",\".\"}),(\"!\"==>double),(\".\"==>double)"
        );
    }

    @Test
    void testDoubleOnlyForSinglePremiseQuestWithGoalPunc() {
        assertRuleContains("G, B, task(\"@\")  |- (polarize(G,task) && polarize(B,belief)), (Goal:Post, Punctuation:Goal)",
                "(double),punc((\"@\",\"!\"))",
                "(double),punc(\"!\")");
    }

    void assertRuleContains(String r, String inc) {
        assertRuleContains(r, inc, null);
    }

    void assertRuleContains(String r, @Nullable String inc, @Nullable String exc) {
        ReactionModel d = compile(r);
        d.print();
        String rs = d.what.toString();
        if (inc != null)
            assertTrue(rs.contains(inc), rs);
        if (exc != null)
            assertFalse(rs.contains(exc), rs);
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleQuestionOverride() {
        compileContains("X,Y,task(\"?\") |- (X&&Y), (Punctuation:Belief,Belief:Intersection)", "(double)");
    }

    @Test
    void testInferQuestionPunctuationFromTaskRequirement() {

        compileContains("Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)", "punc(\"?\")");
    }

    @Test
    void testSubIfUnifyPrefilter() {

        compileContains("X,Y |- unisubst(what,X,Y), (Belief:Intersection)",
                "Unifiability");
    }

    @Test
    void testOpIsPreFilter() {
        compileContains("X,Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)", "Is(taskTerm,\"*\")");
    }

    @Test
    void testOpIsPreFilterSubPath() {
        compileContains("(Z,X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)",
                "Is(");
    }

    @Test
    void testOpIsPreFilterSubPathNot() {
        compileContains("((Z),X),Y, --is(X,\"{\") |- (X,Y), (Belief:Intersection)", "(--,Is(");
    }

    @Test
    void testOpIsPreFilterSubPathRepeatIsOKButChooseShortestPath() {
        compileContains("((X),X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)", "Is(");
    }

    @Test
    void testSubMinSuper() {
        compileContains("((X),X),Y,subsMin(Y,2) |- (X,Y), (Belief:Intersection)", "SubsMin(beliefTerm,2)");
    }

    @Test
    void testSubMinSub() {
        compileContains("((X),Z),Y,subsMin(X,2) |- (X,Y), (Belief:Intersection)", "SubsMin(taskTerm(0,0),2)");
    }

    private void compileContains(String rule, String contains) {
        ReactionModel d = compile(rule);
        String s = d.what.toString();
        assertTrue(s.contains(contains), s);
    }


    @Test
    void BidirectionalConstraint_Not_Repeated_AsPremisePredicate() {

        ReactionModel d = compile(
                "(M --> P), (M --> S), neq(S,P), --condPN(P,S),--condPN(S,P), notSetsOrDifferentSets(S,P) |- (M --> (polarizeTask(P) | polarizeBelief(S))), (Belief:UnionDD)"
        );
        List<? extends PREDICATE> c = ((AND<?>) d.what).conditions();
//        c.stream().map(Term::unneg)
//                .forEach(x -> System.out.println(x));

        assertEquals(3, c.stream().map(z -> z.term().unneg())
//                .filter(z -> !(z instanceof Action.DeferredAction))
                .filter(z -> z.toString().contains("Cond"))
                .count());


    }

    @Test
    void autoValidConclusionType() {
        ReactionModel d = compile("B, (X ==> A)  |- X, (Punctuation:AskAsk)");

        String ww = d.what.toString();
        assertTrue(ww.contains("(--,Is(beliefTerm(0),"));
    }

    private ReactionModel compile(String r) {
        return new Reactions().rules(r).compile(nar);
    }

    @Test
    void testTryFork() {

        ReactionModel d = new Reactions().rules(
                "X,Y |- (X&&Y), (Belief:Intersection)",
                "X,Y |- (||,X,Y), (Belief:Union)").compile(nar);
/*
TODO - share unification state for different truth/conclusions
    TruthFork {
      (Union,_):
      (Intersection,_):
         (unify...
         (
      and {
        truth(Union,_)
        unify(task,%1)
        unify(belief,%2) {
          and {
            derive((||,%1,%2))
            taskify(3)
          }
        }
      }
      and {
        truth(Intersection,_)
        unify(task,%1)
        unify(belief,%2) {
 */

    }


//    @Test
//    void printTermRecursive() throws Narsese.NarseseException {
//        Compound y = DeriveReaction.parse("(S --> P), S |- (P --> S), (Belief:Conversion)").id;
//        Terms.printRecursive(System.out, y);
//    }


}