//package nars.task.util;
//
//import jcog.event.Off;
//import nars.$;
//import nars.NAR;
//import nars.Task;
//import nars.Term;
//import nars.term.var.Variable;
//import nars.term.control.PREDICATE;
//
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
///**
// * Generic handler for matching individual Tasks's
// */
//public abstract class TaskMatch implements Consumer<Task>, Predicate<Task> {
//
//    protected final NAR nar;
//    private final Off on;
//    private PREDICATE<Term> term;
//
//    private PREDICATE<Byte> punctuation;
//
//
//    protected TaskMatch(NAR n) {
//        this.nar = n;
//        this.on = n.main().eventTask.on(this);
//    }
//
//    public void setTerm(PREDICATE<Term> term) {
//        this.term = term;
//    }
//
//    public void setPunctuation(PREDICATE<Byte> punctuation) {
//        this.punctuation = punctuation;
//    }
//
//    @Override
//    public String toString() {
//        return id().toString();
//    }
//
//    public Term id() {
//        return $.func(getClass().getSimpleName(),
//                (term.term()), (punctuation)
//        );
//    }
//
//    public void off() {
//        this.on.close();
//    }
//
//    @Override
//    public boolean test(Task t) {
//        return term == null || term.test(t.term());
//    }
//
//    @Override
//    public void accept(Task x) {
//
//        test(x);
//
//
//    }
//
//
////    protected void onError(SoftException e) {
////
////    }
//
//    /**
//     * accepts the next match
//     *
//     * @param task
//     * @param xy
//     * @return true for callee to continue matching, false to stop
//     */
//    protected abstract void accept(Task task, Map<Variable, Term> xy);
//
//
//}