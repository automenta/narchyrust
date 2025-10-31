package nars.test.analyze;

import nars.*;
import nars.concept.TaskConcept;
import nars.table.EmptyBeliefTable;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.TruthWave;
import org.jetbrains.annotations.Nullable;

/** utility class for analyzing the belief/goal state of a concept */
public class BeliefAnalysis implements Termed {

	public final Term term;
	public final NAR nar;

	public BeliefAnalysis(NAR n, Term term) {
		this.nar = n;
		this.term = term;
	}

	public BeliefAnalysis(NAR n, String term) throws Narsese.NarseseException {
        this( n, $.$(term));
	}

	@Override
	public Term term() {
		return term;
	}


	public BeliefAnalysis goal(float freq, float conf) {
		nar.want(term, freq, conf);
		return this;
	}

	public BeliefAnalysis believe(float freq, float conf) {
		nar.believe(term, freq, conf);
		return this;
	}

	public BeliefAnalysis believe(float freq, float conf, Tense present) {
		nar.believe(term, present, freq, conf);
		return this;
	}
	public BeliefAnalysis believe(float pri, float freq, float conf, long when) {
		nar.believe(pri, term, when, freq, conf);
		return this;
	}

	public @Nullable TaskConcept concept() {
		return (TaskConcept) nar.concept(term);
	}

	public @Nullable BeliefTable beliefs() {
		Concept c = concept();
		return c == null ? EmptyBeliefTable.Empty : c.beliefs();
	}
	public @Nullable BeliefTable goals() {
		Concept c = concept();
		return c == null ? EmptyBeliefTable.Empty : c.goals();
	}

	public TruthWave wave() {
		return new TruthWave(beliefs());
	}

	public BeliefAnalysis run(int frames) {
		nar.run(frames);
		return this;
	}

	public void print() {
		print(true);
	}
	public void print(boolean beliefOrGoal) {
		BeliefTable table = table(beliefOrGoal);
		System.out.println((beliefOrGoal ? "Beliefs" : "Goals") + "[@" + nar.time() + "] " + table.taskCount());
		table.print(System.out);
		
	}

	public int size(boolean beliefOrGoal) {
		return table(beliefOrGoal).taskCount();
	}

	public @Nullable BeliefTable table(boolean beliefOrGoal) {
		return beliefOrGoal ? beliefs() : goals();
	}


	public BeliefAnalysis input(boolean beliefOrGoal, float f, float c) {
		if (beliefOrGoal)
			believe(f, c);
		else
			goal(f, c);
		return this;
	}


	public long time() {
		return nar.time();
	}

}