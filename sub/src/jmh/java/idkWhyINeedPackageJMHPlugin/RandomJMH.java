package idkWhyINeedPackageJMHPlugin;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.All)
public class RandomJMH {
    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder().addProfiler(GCProfiler.class).build()).run();
    }

    private static final char[] ALPHANUMERIC = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
        'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z',
    };

    private static String generateString(IntStream randomStream) {
        return randomStream
            .limit(10)
            .collect(
                StringBuffer::new,
                (acc, i) -> acc.append(RandomJMH.ALPHANUMERIC[i]),
                (__, ___) -> { throw new RuntimeException("Buggy code: Unexpected parallel stream"); }
            ).toString();
    }

    // Benchmark                                          (length)  Mode  Cnt  Score   Error  Units
    // idkWhyINeedPackageJMHPlugin.RandomJMH.superrandom        10  avgt   25  0.373 ± 0.005  ns/op
    // sure
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public String superrandom() {
        return "a";
    }

    // Benchmark                                       Mode  Cnt      Score    Error  Units
    // idkWhyINeedPackageJMHPlugin.RandomJMH.lcgprng   avgt   25    208.756 ±  2.334  ns/op
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public String lcgprng() {
        enum Local { ;
            static final Random PRNG = new Random(); // Turns out java.util.Random uses LCG
        }
        return RandomJMH.generateString(Local.PRNG.ints(0, RandomJMH.ALPHANUMERIC.length));
    }

    // lcg as written by a random undergrad trying to speedrun their cryptography homework, do not try this at home
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public String poormanlcg() {
        enum Local { ;
            static final int seed = new Random().nextInt(RandomJMH.ALPHANUMERIC.length);
            // Benchmark                                         Mode  Cnt    Score   Error  Units
            // idkWhyINeedPackageJMHPlugin.RandomJMH.poormanlcg  avgt   25  144.708 ± 0.773  ns/op
            static int poornext(int previous) {
                return (previous * 1337) % RandomJMH.ALPHANUMERIC.length;
            }

            // Benchmark                                         Mode  Cnt    Score   Error  Units
            // idkWhyINeedPackageJMHPlugin.RandomJMH.poormanlcg  avgt   20  143.954 ± 1.253  ns/op
            static int ultrapoornext(int previous) {
                final int value = (previous * 1337) & 0b11_1111; 
                return value >= RandomJMH.ALPHANUMERIC.length ? value - 3 : value;
            }
        }
        return RandomJMH.generateString(IntStream.iterate(Local.seed, Local::poornext));
    }

    // Benchmark                                          Mode  Cnt    Score   Error  Units
    // idkWhyINeedPackageJMHPlugin.RandomJMH.poormanlfsr  avgt   25  144.471 ± 1.374  ns/op
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public String poormanlfsr() {
        enum Local { ;
            static final int seed = new Random().nextInt(RandomJMH.ALPHANUMERIC.length);
            static int poorlfsr(int previous) {
                final int value = ((((previous >> 0) ^ (previous >> 1)) & 1) << 5) | (previous >> 1); // assuming im not drunk, this should represent tapping at x^6 + x^5, like what wiki said
                return value >= RandomJMH.ALPHANUMERIC.length ? value - 3 : value;
            }
        }
        return RandomJMH.generateString(IntStream.iterate(Local.seed, Local::poorlfsr));
    }


    // Benchmark                                       (length)  Mode  Cnt      Score    Error  Units
    // idkWhyINeedPackageJMHPlugin.RandomJMH.csrandom        10  avgt   25  12839.940 ± 24.002  ns/op
    // Kernel: Linux 6.16.12
    // understandable, csprng is expensive---but microservice dance is still more expensive!
    // havent check the profiler, but my guess it probably get blocked at lot waiting for entropy sources. cpu fan barely spinning, unlike other function
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public String csrandom() {
        enum Local { ;
            static final SecureRandom CSPRNG = new SecureRandom();
        }
        return RandomJMH.generateString(Local.CSPRNG.ints(0, RandomJMH.ALPHANUMERIC.length));
    }
}
