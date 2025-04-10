/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;


import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

/*
 * @test
 * @summary verify logging of call to System.gc or Runtime.gc.
 * @requires vm.flagless
 * @build RuntimeLogTestUtils
 * @run junit/othervm RuntimeGCLogTest
 */

public class RuntimeGCLogTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static Object HOLD_LOGGER;

    /**
     * Call System.gc().
     */
    public static void main(String[] args) throws InterruptedException {
        if (System.getProperty("ThrowingHandler") != null) {
            HOLD_LOGGER = RuntimeLogTestUtils.ThrowingHandler.installHandler();
        }
        System.gc();
    }

    /**
     * Generate a regular expression pattern that match the expected log output for a Runtime.gc() call.
     * The pattern includes the method call stack trace.
     * @return the regex pattern as a string
     */
    private static String generateStackTraceLogPattern() {
        return "(?s)^.+ java\\.lang\\.Runtime logRuntimeGC\\n" +
                ".*: Runtime\\.gc\\(\\) called\\n" +
                "java\\.lang\\.Throwable: Runtime\\.gc\\(\\)\\n" +
                "\\s+at java\\.base/java\\.lang\\.Runtime\\.logRuntimeGC\\(Runtime\\.java:\\d+\\)\\n" +
                "\\s+at(?: .+)";
    }

    /**
     * Test various log level settings, and none.
     * @return a stream of arguments for parameterized test
     */
    private static Stream<Arguments> logParamProvider() {
        return Stream.of(
                // Logging configuration using the java.util.logging.config.file property
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-ALL.properties").toString()),
                         generateStackTraceLogPattern()),
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-FINER.properties").toString()),
                         generateStackTraceLogPattern()),
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-FINE.properties").toString()),
                         generateStackTraceLogPattern()),
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-INFO.properties").toString()),
                         ""),
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-WARNING.properties").toString()),
                         ""),
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-SEVERE.properties").toString()),
                         ""),
                Arguments.of(List.of("-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-OFF.properties").toString()),
                         ""),

                // Logging configuration using the jdk.system.logger.level property
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=ALL"),
                        generateStackTraceLogPattern()),
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=TRACE"),
                        generateStackTraceLogPattern()),
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=DEBUG"),
                        generateStackTraceLogPattern()),
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=INFO"),
                        ""),
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=WARNING"),
                        ""),
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=ERROR"),
                        ""),
                Arguments.of(List.of("--limit-modules", "java.base",
                        "-Djdk.system.logger.level=OFF"),
                        ""),

                // level DEBUG, but -XX:+DisableExplicitGC disables logging
                Arguments.of(List.of("-XX:+DisableExplicitGC", "-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-FINE.properties").toString()),
                        ""),

                // Throwing Handler
                Arguments.of(List.of("-DThrowingHandler",
                        "-Djava.util.logging.config.file=" +
                        Path.of(TEST_SRC, "Logging-FINE.properties").toString()),
                        "Runtime\\.gc\\(\\) logging failed: Exception in publish"),

                // Default console logging configuration with no additional parameters
                Arguments.of(List.of(), "")
                );
    }

    @ParameterizedTest
    @MethodSource("logParamProvider")
    public void checkLogger(List<String> logProps, String expectMessage) {
        RuntimeLogTestUtils.checkLogger(logProps, expectMessage, this.getClass().getName(), -1);
    }
}
