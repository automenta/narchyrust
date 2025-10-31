package nars.perf;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.random.RandomGenerator;

import static nars.perf.NARBenchmarks.perf;

@State(Scope.Thread)
@AuxCounters(AuxCounters.Type.EVENTS)
@Disabled
public class RandomBitsPerf {

    static final int iterations = 1024 * 8;

    @Param({"2","8", "64", "256", "512"})
    private String maxInt;



    public static void main(String[] args) throws RunnerException {
        perf(RandomBitsPerf.class, o -> {
            o.warmupIterations(1);
            o.measurementIterations(3);
            o.measurementTime(TimeValue.seconds(1));
            o.threads(1);
            o.forks(1);
        });
    }

    private static Random rng() {
        return new XoRoShiRo128PlusRandom(1);
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randBits() {
        RandomGenerator r = new RandomBits(rng());
        int mInt = Integer.parseInt(maxInt);
        //int bits = RandomBits.bits(Integer.parseInt(maxInt));
        for (int i = 0; i < iterations; i++) {
            int b = r.nextInt(mInt);
        }
    }
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void random() {
        Random r = rng();
        int maxInt = Integer.parseInt(this.maxInt);
        for (int i = 0; i < iterations; i++) {
            int b = r.nextInt(maxInt);
        }
    }

}