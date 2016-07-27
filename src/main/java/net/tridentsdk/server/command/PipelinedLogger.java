/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2016 The TridentSDK Team
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
package net.tridentsdk.server.command;

import org.fusesource.jansi.AnsiConsole;

import java.io.OutputStream;

/**
 * This class is the superclass of every logger in the
 * pipeline.
 *
 * <p>The server pipeline usually looks something like
 * this:
 * <pre>{@code
 *              [Plugin Loggers]
 *                     ||
 *                     \/
 *                 FileLogger
 *                     ||
 *                     \/
 *             [Logger handlers]
 *                     ||
 *                    /  \
 *       NoDebugLogger ?? DebugLogger
 *                    \  /
 *                     ||
 *                     \/
 *               ColorizerLogger
 *                     ||
 *                     \/
 *                DefaultLogger
 * }</pre></p>
 */
public abstract class PipelinedLogger {
    /**
     * The next logger in the pipeline
     */
    protected final PipelinedLogger next;

    /**
     * The super constructor for the pipeline logger.
     *
     * @param next the next logger in the pipeline
     */
    public PipelinedLogger(PipelinedLogger next) {
        this.next = next;
    }

    /**
     * Initialization code
     */
    public static PipelinedLogger init(boolean verbose) throws Exception {
        AnsiConsole.systemInstall();
        System.setErr(System.out);
        // force error stream to
        // lock on System.out

        // tail of pipeline
        PipelinedLogger underlying = new DefaultLogger();
        PipelinedLogger colorizer = new ColorizerLogger(underlying);
        PipelinedLogger debugger = verbose ? DebugLogger.verbose(colorizer) : DebugLogger.noop(colorizer);
        PipelinedLogger handler = new LoggerHandlers(debugger);
        return FileLogger.init(handler); // head of pipeline
    }

    /**
     * Handles normal messages.
     *
     * @param msg the message
     * @return the same message
     */
    public abstract LogMessageImpl handle(LogMessageImpl msg);

    /**
     * Handles partial messages.
     *
     * @param msg the message
     * @return the same partial message
     */
    public abstract LogMessageImpl handlep(LogMessageImpl msg);

    /**
     * Obtains the next logger in the pipeline.
     *
     * @return the next logger
     */
    public PipelinedLogger next() {
        return next;
    }

    /**
     * Logs a message
     *
     * @param msg the message
     */
    public void log(LogMessageImpl msg) {
        if (msg == null) return;
        next.log(handle(msg));
    }

    /**
     * Logs a message without terminating the line
     *
     * @param msg the message
     */
    public void logp(LogMessageImpl msg) {
        if (msg == null) return;
        next.logp(handlep(msg));
    }

    /**
     * Logs a message
     *
     * @param msg the message
     */
    public void success(LogMessageImpl msg) {
        if (msg == null) return;
        next.success(handle(msg));
    }

    /**
     * Logs a message without terminating the line
     *
     * @param msg the message
     */
    public void successp(LogMessageImpl msg) {
        if (msg == null) return;
        next.successp(handlep(msg));
    }

    /**
     * Logs a message
     *
     * @param msg the message
     */
    public void warn(LogMessageImpl msg) {
        if (msg == null) return;
        next.warn(handle(msg));
    }

    /**
     * Logs a message without terminating the line
     *
     * @param msg the message
     */
    public void warnp(LogMessageImpl msg) {
        if (msg == null) return;
        next.warnp(handlep(msg));
    }

    /**
     * Logs a message
     *
     * @param msg the message
     */
    public void error(LogMessageImpl msg) {
        if (msg == null) return;
        next.error(handle(msg));
    }

    /**
     * Logs a message without terminating the line
     *
     * @param msg the message
     */
    public void errorp(LogMessageImpl msg) {
        if (msg == null) return;
        next.errorp(handlep(msg));
    }

    /**
     * Logs a message
     *
     * @param msg the message
     */
    public void debug(LogMessageImpl msg) {
        if (msg == null) return;
        next.debug(handle(msg));
    }

    /**
     * Obtains the underlying output.
     *
     * @return the output
     */
    public OutputStream out() {
        return next.out();
    }
}