package nars.term.atom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomBytesTest {

	@Test void simple() {
		String s = "(((((a,b,c),(a,b,d)))))";
		BytesAtom abc = BytesAtom.atomBytes(s);
		assertEquals("\"" + s + "\"", abc.toString());
//		assertEquals(26, abc.bytes().length);
	}
}