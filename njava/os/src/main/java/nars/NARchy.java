//package nars;
//
//import nars.exe.impl.WorkerExec;
//import nars.func.language.NARHear;
//import nars.memory.CaffeineMemory;
//import nars.time.clock.RealTime;
//
//public class NARchy extends NARS {
//
//    //static final Logger logger = LoggerFactory.getLogger(NARchy.class);
//
//    public static NAR core() {
//        return core(Runtime.getRuntime().availableProcessors());
//    }
//
//    public static NAR core(int threads) {
//
//        NAR nar = new NARS()
//                .memory(CaffeineMemory.soft())
//                //.index(new HijackConceptIndex(32*1024, 4))
//                .exe(new WorkerExec(threads, false, (n)->Derivers.nal(1, 8, "motivation.nal").core().stm().temporalInduction().compile(n)))
//                .time(new RealTime.MS().durFPS(10f))
//                .get();
//
//        nar.timeRes.set(20);
//
//		//new Arithmeticize.ArithmeticIntroduction(nar, );
//
//        return nar;
//    }
//
//    public static NAR ui() {
//        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
//        NAR nar = core();
//        nar.throttle(0.1f);
//
//        var f = nar.main();
//
//        nar.runLater(()->{
//
//            //User u = User.the();
//
//            NARHear.readURL(f);
//
////            {
////                NARSpeak s = new NARSpeak(nar);
////                s.spoken.on(  s::speak);
////
////            }
//
////            InterNAR i = new InterNAR(nar);
////            i.fps(2);
//
//
//        });
//
//        return nar;
//    }
//
//
//}