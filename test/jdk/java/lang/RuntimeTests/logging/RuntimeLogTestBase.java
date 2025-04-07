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

import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RuntimeLogTestBase {
    protected static final String TEST_JDK = System.getProperty("test.jdk");
    protected static final String TEST_SRC = System.getProperty("test.src");
    protected static final String NEW_LINE = System.lineSeparator();
    protected static Object HOLD_LOGGER;

    /**
     * Check that the logger output of a launched process contains the expected message.
     * @param logProps The name of the log.properties file to set on the command line
     * @param expectMessage log should contain the message
     * @param className the name of the test class
          */
    protected void checkLogger(List<String> logProps, String expectMessage, String className) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(true);

        List<String> cmd = pb.command();
        cmd.add(Path.of(TEST_JDK, "bin", "java").toString());
        cmd.addAll(logProps);
        cmd.add(className);
        
        try {
            Process process = pb.start();
            try (BufferedReader reader = process.inputReader()) {
                checkLogContent(reader, expectMessage);
            } 
        } catch (IOException ex) {
            fail(ex);
        }
    }

    /**
     * Check that the logger output of a launched process contains the expected message.
     * @param logProps The name of the log.properties file to set on the command line
     * @param expectMessage log should contain the message
     * @param className the name of the test class
     * @param status the expected exit status of the process
     */
    protected void checkLogger(List<String> logProps, String expectMessage, String className, int status) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(true);

        List<String> cmd = pb.command();
        cmd.add(Path.of(TEST_JDK, "bin", "java").toString());
        cmd.addAll(logProps);
        cmd.add(className);
        cmd.add(Integer.toString(status));

        try {
            Process process = pb.start();
            try (BufferedReader reader = process.inputReader()) {
                checkLogContent(reader, expectMessage);
            } 
            int result = process.waitFor();
            assertEquals(status, result, "Exit status");
        } catch (IOException | InterruptedException ex) {
            fail(ex);
        }
    }

    private void checkLogContent(BufferedReader reader, String expectMessage) {
        List<String> lines = reader.lines().toList();
        boolean match = (expectMessage.isEmpty())
            ? lines.isEmpty()
            : String.join("\n", lines).matches(expectMessage);
        if (!match) {
            System.err.println("Expected pattern (line-break):");
            System.err.println(expectMessage.replaceAll("\\n", NEW_LINE));
            System.err.println("---- Actual output begin");
            lines.forEach(System.err::println);
            System.err.println("---- Actual output end");
            fail("Unexpected log contents");
        }
    }

    /**
     * A LoggingHandler that throws an Exception.
     */
    protected static class ThrowingHandler extends StreamHandler {

        // Install this handler for java.lang.Runtime
        public static Logger installHandler() {
            Logger logger = Logger.getLogger("java.lang.Runtime");
            logger.addHandler(new ThrowingHandler());
            return logger;
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            throw new RuntimeException("Exception in publish");
        }
    }
}
