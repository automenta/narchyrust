package nars.deriver.util;

/**
 * TODO
 * lossy derivations
 *
 * if the a derived task (or premise) is excessively complicated,
 * apply different strategies to produce a partial version
 * that represents some of the original content.
 *
 * 1. variable introduction.  repeated application of variable introduction
 * until complexity is below threshold or no more variables to introduce
 *
 * 2. structural decomposition (ex: &&, ==>) into components
 *
 * 3. erasure by (dependent or query) variable
 * 4. sequence to parallel
 * 5. drop events
 */
public class LossyDerive {

}