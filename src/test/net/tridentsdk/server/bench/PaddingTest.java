package net.tridentsdk.server.bench;

import net.tridentsdk.world.World;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

// THIS IS A TOY - DON'T TRY IMPLEMENTING IT UNLESS YOU TALK TO
// AGENTTROLL
public class PaddingTest {
    private static final Coordinates COORDINATES = new Coordinates(null, 0, 0, 0, 0, 0);
    private static final net.tridentsdk.Coordinates COOR = net.tridentsdk.Coordinates.create(null, 0, 0, 0);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(".*" + PaddingTest.class.getSimpleName() + ".*")
                .timeUnit(TimeUnit.NANOSECONDS)
                .mode(Mode.AverageTime)
                .warmupIterations(20)
                .measurementIterations(5)
                .warmupTime(TimeValue.nanoseconds(10))
                .measurementTime(TimeValue.nanoseconds(10))
                .forks(20)
                .threads(4)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public double normal() {
        return COOR.getX() + COOR.getY() + COOR.getZ();
    }

    @Benchmark
    public double padded() {
        return COORDINATES.x + COORDINATES.y + COORDINATES.z;
    }

    public static class Coordinates implements Cloneable {
        private volatile double x;
        private volatile double d0, d1, d2, d3, d4, d5, d6, d7 = 0;
        private volatile double y;
        private volatile double d8, d9, d10, d11, d12, d13, d14, d15 = 0;
        private volatile double z;

        private volatile World world;

        private volatile float yaw;
        private float f0, f1, f2, f3, f4, f5, f6, f7 = 0;
        private volatile float pitch;

        private volatile double po;

        private Coordinates(World world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;

            this.x = x;
            this.y = y;
            this.z = z;

            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
