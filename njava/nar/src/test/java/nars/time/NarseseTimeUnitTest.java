package nars.time;

import nars.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NarseseTimeUnitTest {

	/**
	 * milliseconds realtime
	 */
	private final NAR n = NARS.realtime(1f).get();

	@Test
	void testOccurence() throws Narsese.NarseseException {
		@Deprecated NALTask now = n.inputTask("(a-->b). :|:");


		NALTask now1 = n.inputTask("(a-->b). |");
		NALTask now1withTruth = n.inputTask("(a-->b). | %1.0;0.90%");
		NALTask now2 = n.inputTask("(a-->b). +0");
		NALTask next = n.inputTask("(a-->b). +1");
		NALTask prev = n.inputTask("(a-->b). -1");
		assertEquals(now1.start(), now2.start());
		assertEquals(now1.start(), now1withTruth.start());
		assertEquals(now1.start() + 1, next.start());
		assertEquals(now1.start() - 1, prev.start());
	}

	@Test
	void testCycleTimeOccurenceRange() throws Narsese.NarseseException {
//        @Deprecated NALTask now = task("(a-->b). :|:");

		NALTask now1 = n.inputTask("(a-->b). +1..2");
		assertEquals(1, now1.start());
		assertEquals(2, now1.end());
	}

	@Test
	void testRealtimeOccurrence() throws Narsese.NarseseException {
		NALTask day = n.inputTask("(a-->b). +1day");
		assertEquals(8.64E7, day.start() - n.time(), 10000);

		NALTask minusDay = n.inputTask("(a-->b). -1day");
		assertEquals(-8.64E7, minusDay.start() - n.time(), 10000);

		NALTask plusHour = n.inputTask("(a-->b). +1h");
		assertEquals(60 * 60 * 1000, plusHour.start() - n.time(), 1000);

		NALTask plusHour2 = n.inputTask("(a-->b). +1hr");
		assertEquals(plusHour2.toString(), plusHour.toString());

		NALTask minusHour = n.inputTask("(a-->b). -1h");
		assertEquals(-60 * 60 * 1000, minusHour.start() - n.time(), 10000);

		NALTask year = n.inputTask("(a-->b). +1year");
		assertEquals(3.15569521E10, year.start() - n.time(), 10000);

	}

	@Test
	void testRealtimeRelativeOccurrenceRange() throws Narsese.NarseseException {

		{
			for (String nowStr : new String[]{"|", "+0", ":|:"}) {
				String taskStr = "(a-->b). " + nowStr + "..+5m";
				NALTask x = n.inputTask(taskStr);
				System.out.println(taskStr + ' ' + x);
				assertEquals(0, x.start() - n.time(), 1000);
				assertEquals(5 * 60 * 1000, x.end() - n.time(), 1000);
			}
		}

		{
			NALTask x = n.inputTask("(a-->b). -2h..+5m");
			assertEquals(2 * 60 * 60 * 1000, n.time() - x.start(), 1000);
			assertEquals(5 * 60 * 1000, x.end() - n.time(), 1000);
		}

		{

			NALTask x = n.inputTask("(a-->b). +5h..-2m");
			assertEquals(2 * 60 * 1000, n.time() - x.start(), 1000);
			assertEquals(5 * 60 * 60 * 1000, x.end() - n.time(), 1000);
		}

		{
			NALTask x = n.inputTask("(a-->b). +2h..+7h");
			assertEquals(2 * 60 * 60 * 1000, x.start() - n.time(), 1000);
			assertEquals(7 * 60 * 60 * 1000, x.end() - n.time(), 1000);
		}
		{
			NALTask x = n.inputTask("(a-->b). -7h..-2h");
			assertEquals(-7 * 60 * 60 * 1000, x.start() - n.time(), 1000);
			assertEquals(-2 * 60 * 60 * 1000, x.end() - n.time(), 1000);
		}
		{
			NALTask x = n.inputTask("(a-->b). -2s..|");
			assertEquals(-2000, x.start() - n.time(), 100);
			assertEquals(0, x.end() - n.time(), 100);
		}

	}

	@Test
	void testTimeDeltaUnits() {

		assertEquals(
			"term(\"&&\",(a,b),(day,1))",
			$$("(a &&+1day b)").toString()
		);


		assertEquals("(a &&+86400000 b)",
			n.eval($$("(a &&+1day b)")).toString()
		);


		assertEquals("(a &&+129600000 b)",
			n.eval($$("(a &&+1.5days b)")).toString()
		);
	}

	@Test
	void testTimeDeltaUnits2() {
		assertEquals("((c &&+259200000 b) &&+604800000 a)",
			n.eval($$("(a &&-1week (b &&-3days c))")).toString()
		);

	}

	@Test
	void testTimeDeltaUnitsAndOccurrence() throws Narsese.NarseseException {
		StringBuilder sb = new StringBuilder();
		n.log(sb);
		List<Task> t = n.input("(a &&+1day b)! +5m..+2h %1.0;0.90%");
		n.run(1);

		assertTrue(sb.toString().contains("(a &&+86400000 b)! "));
		assertTrue(sb.toString().contains("%1.0;.90%"));

	}


}