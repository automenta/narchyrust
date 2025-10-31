//package nars;
//
//import java.io.File;
//import java.io.OutputStream;
//import java.io.PrintStream;
//
//public class Tester extends SelfTest {
//
//    private Tester() {
//
//    }
//
//    public static void main(String[] args) {
////        Stream.of(new File(".").listFiles()).forEach(System.out::println);
//
//        PrintStream out = System.out;
//        PrintStream err = System.err;
//        PrintStream nullPrinter = new PrintStream(OutputStream.nullOutputStream());
//        System.setOut(nullPrinter);
//        System.setErr(nullPrinter);
//        {
//            Tester s = new Tester();
//            s.addClassPath(new File("./nar/target/test-classes/"));
//            s.run(4);
//        }
//        System.setOut(out);
//        System.setErr(err);
//    }
//}
