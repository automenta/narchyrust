package nars.util;

import nars.Narsese;
import nars.Task;
import nars.test.TestNAR;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * test an invididual premise
 */
public interface RuleTest {
























	static TestNAR get(TestNAR test, String task, String belief, String result, float minFreq,
                                float maxFreq, float minConf, float maxConf) {
		test(
				
				test, task, belief, result, minFreq, maxFreq, minConf,
				maxConf);
		return test;
	}

	

	static void test(TestNAR test, String task, String belief, String result,
					 float minFreq, float maxFreq, float minConf, float maxConf) {
		try {
            test(test, Narsese.task(task, test.nar), Narsese.task(belief, test.nar), result, minFreq, maxFreq,
                    minConf, maxConf);
		} catch (Narsese.NarseseException e) {
			e.printStackTrace();
            fail(e);
		}

	}
	static void test(TestNAR test, Task task, Task belief, String result,
                     float minFreq, float maxFreq, float minConf, float maxConf) {

		test.nar.input(task);
		test.nar.input(belief);
		test.mustBelieve(25, result, minFreq, maxFreq, minConf, maxConf);

	}


}
