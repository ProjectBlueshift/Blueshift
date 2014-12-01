/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2014 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tridentsdk.server;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import net.tridentsdk.api.config.JsonConfig;
import net.tridentsdk.api.event.EventManager;
import net.tridentsdk.api.event.Listenable;
import net.tridentsdk.api.event.Listener;
import net.tridentsdk.api.factory.CollectFactory;
import net.tridentsdk.api.factory.ConfigFactory;
import net.tridentsdk.api.factory.Factories;
import net.tridentsdk.server.threads.ThreadsManager;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/*
Benchmark results: http://bit.ly/1B3psZv
 */
@State(Scope.Benchmark)
public class EventBusPerformance {
    private static final EventBus EVENT_BUS = new EventBus();
    private static final EventManager EVENT_MANAGER = new EventManager();

    private static final EventHandler HANDLER = new EventHandler();
    private static final Listener LISTENER = new EventListener();

    private static final Listenable EVENT = new Event();

    public static void main1(String[] args) {
        while (true) {
            EVENT_MANAGER.registerListener(LISTENER);
        }
    }

    public static void main0(String[] args) {
        EVENT_MANAGER.registerListener(LISTENER);
        EVENT_MANAGER.call(EVENT);
    }

    @Setup
    public void setUp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Factories.init(new ConfigFactory() {
                    @Override
                    public JsonConfig serverConfig() {
                        return new JsonConfig(Paths.get("/topkek"));
                    }
                });
                Factories.init(new CollectFactory() {
                    @Override
                    public <K, V> ConcurrentMap<K, V> createMap() {
                        return new ConcurrentHashMapV8<>();
                    }
                });
                Factories.init(new TridentScheduler());
                Factories.init(new ThreadsManager());
            }
        }).start();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + EventBusPerformance.class.getSimpleName() + ".*") // CLASS
                .timeUnit(TimeUnit.NANOSECONDS)
                .mode(Mode.AverageTime)
                .warmupIterations(20)
                .warmupTime(TimeValue.milliseconds(1))              // ALLOWED TIME
                .measurementIterations(5)
                .measurementTime(TimeValue.milliseconds(1))         // ALLOWED TIME
                .forks(1)                                           // FORKS
                .verbosity(VerboseMode.SILENT)                      // GRAPH
                .threads(4)                                         // THREADS
                .build();

        Benchmarks.chart(Benchmarks.parse(new Runner(opt).run()), "Event Dispatch performance"); // TITLE
    }

    @Param({"1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024"})
    private int cpuTokens;

    @Benchmark
    public void control() {
        Blackhole.consumeCPU(cpuTokens);
    }

    @Benchmark
    public void eventBusRegister() {
        Blackhole.consumeCPU(cpuTokens);
        EVENT_BUS.register(HANDLER);
    }

    @Benchmark
    public void aeventManagerRegister() {
        Blackhole.consumeCPU(cpuTokens);
        EVENT_MANAGER.registerListener(LISTENER);
    }

    @Benchmark
    public void eventBusDispatch() {
        Blackhole.consumeCPU(cpuTokens);
        EVENT_BUS.post(EVENT);
    }

    @Benchmark
    public void eventManagerDispatch() {
        Blackhole.consumeCPU(cpuTokens);
        EVENT_MANAGER.call(EVENT);
    }

    private static class Event extends Listenable {
    }

    private static class EventHandler {
        @Subscribe
        public void handle(Event event) {
        }
    }

    private static class EventListener implements Listener {
        public void onEvent(Event event) {
            System.out.println("LOL");
        }
    }
}
