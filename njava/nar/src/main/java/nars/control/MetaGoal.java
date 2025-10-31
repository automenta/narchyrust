//package nars.control;
//
//import com.google.common.collect.TreeBasedTable;
//import jcog.Research;
//import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
//import org.eclipse.collections.impl.map.mutable.primitive.ObjectDoubleHashMap;
//
//import java.io.PrintStream;
//
//import static jcog.Str.n4;
//
///**
// * high-level reasoner control parameters
// * neg: perceived as input, can be measured for partial and complete tasks
// * in their various stages of construction.
// * <p>
// * information is considered negative value by default (spam).
// * see: http:
// * <p>
// * satisfying other goals is necessary to compensate for the
// * perceptual cost.
// * <p>
// * there is also the "meta-goal" system which allows me to specify the high-level motivation of the system in terms of a few variables.  i can direct it to focus on action rather than belief, or throttle perception, or instead concentrate on answering questions.  more of these meta goals can be added but the total number needs to be kept small because each one is tracked for every cause in the system which could be hundreds or thousands.  so i settled on 5 basic ones so far you can see these in the EXE menu.  each can be controlled by a positive or negative factor, 0 being neutral and having no effect.  negative being inhibitory.  it uses these metagoals to to estimate the 'value' of each 'cause' the system is able to choose to apply throughout its operating cycles.  when it knows the relative value of a certain cause it can allocate an appropriate amount of cpu time for it in the scheduler.   these are indicated in the animated tree chart enabled by the 'CAN' button
// */
//@Research
//public enum MetaGoal {
//
//    Perceive,
//
//    Believe,
//
//    Goal,
//
//    Question
//
////    /** time consumed */
////    Wait
//
//    //Futile,
//
////    /**
////     * pos: anwers a question
////     */
////    Answer,
//
////    /**
////     * pos: actuated a goal concept
////     */
////    Action,
//
//    ;
//
//    public final int id = ordinal(); //just in case this isnt JIT'd
//
//
//    public static class Report extends ObjectDoubleHashMap<ObjectBytePair<Cause>> {
//
//        public TreeBasedTable<Cause, MetaGoal, Double> table() {
//            TreeBasedTable<Cause, MetaGoal, Double> tt = TreeBasedTable.create();
//            MetaGoal[] mv = MetaGoal.values();
//            synchronized (this) {
//                forEachKeyValue((k, v) -> {
//                    Cause c = k.getOne();
//                    MetaGoal m = mv[k.getTwo()];
//                    tt.put(c, m, v);
//                });
//            }
//            return tt;
//        }
//
//        public Report add(Report r) {
//            synchronized (this) {
//                r.forEachKeyValue(this::addToValue);
//            }
//            return this;
//        }
//
//        public Report add(Iterable<Cause> cc) {
//
//            for (Cause c : cc) {//                int i = 0;
////                MetaGoal[] values = MetaGoal.values();
//                //TODO
//
//            }
//            return this;
//        }
//
//        public synchronized void print(PrintStream out) {
//            keyValuesView().toSortedListBy(x -> -x.getTwo()).forEach(x ->
//                    out.println(
//                            n4(x.getTwo()) + '\t' + MetaGoal.values()[x.getOne().getTwo()] + '\t' + x.getOne().getOne()
//                    )
//            );
//        }
//    }
//
//}