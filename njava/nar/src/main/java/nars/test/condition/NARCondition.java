package nars.test.condition;

import org.slf4j.Logger;

import java.io.Serializable;
import java.util.function.BooleanSupplier;

/**
 * a condition which can be observed as being true or false
 * in the observed behavior of a NAR
 */
public interface NARCondition extends Serializable, BooleanSupplier {

	/**
	 * max possible cycle time in which this condition could possibly be satisfied.
	 */
	long getFinalCycle();

	default void log(Logger logger) {
		String msg = getAsBoolean() ? "OK" : "ERR";
//		if (successCondition) {
			logger.info("{} {} {}", getClass(), msg, this);
//		} else {
//			logger.warn("{} {} {}", label, this, msg);
//		}
	}


}