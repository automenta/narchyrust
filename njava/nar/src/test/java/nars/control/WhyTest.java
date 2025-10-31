//package nars.control;
//
//import nars.Term;
//import org.junit.jupiter.api.Test;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static nars.term.util.Testing.assertEq;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//class WhyTest {
//
//	private static final Term x = Why.why(5, ()->Why.why((short) 1),()-> Why.why(new short[]{(short) 2, (short) 3} , 5));
//
//	@Test
//	void testMerge() {
//		assertEq("1", Why.why((short) 1));
//		assertEq("{1,2}", Why.why(3, ()->Why.why((short) 1), ()->Why.why((short) 2)));
//
//		assertEq("{{2,3},1}", x);
//	}
//	@Test
//	void testForceLinearize() {
//		assertEq("{1,2,3}", Why.why(4, ()-> Why.why((short) 1), ()->Why.why(new short[]{(short) 2, (short) 3}, 4)));
//	}
//
//	@Test
//	void testForceSample() {
//		assertEquals(3, Why.why(3, ()->Why.why((short)1), ()-> Why.why(new short[]{(short)2, (short)3}, 3)).complexity());
//	}
//
//	@Test void testEval() {
//		Map m = new HashMap();
//		Why.eval(x, 1, null, (v, p, x) -> m.put(v,p));
//		assertEquals("{1=0.5, 2=0.25, 3=0.25}", m.toString());
//	}
//
//}