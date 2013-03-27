/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     6882376 6985460
 * @summary Test if java.util.logging.Logger is created before and after
 *          logging is enabled.  Also validate some basic PlatformLogger
 *          operations.  othervm mode to make sure java.util.logging
 *          is not initialized.
 *
 * @compile -XDignore.symbol.file PlatformLoggerTest.java
 * @run main/othervm PlatformLoggerTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.*;
import sun.util.logging.PlatformLogger;

public class PlatformLoggerTest {
    public static void main(String[] args) throws Exception {
        final String FOO_PLATFORM_LOGGER = "test.platformlogger.foo";
        final String BAR_PLATFORM_LOGGER = "test.platformlogger.bar";
        final String GOO_PLATFORM_LOGGER = "test.platformlogger.goo";
        final String BAR_LOGGER = "test.logger.bar";
        PlatformLogger goo = PlatformLogger.getLogger(GOO_PLATFORM_LOGGER);
        // test the PlatformLogger methods
        testLogMethods(goo);

        // Create a platform logger using the default
        PlatformLogger foo = PlatformLogger.getLogger(FOO_PLATFORM_LOGGER);
        checkPlatformLogger(foo, FOO_PLATFORM_LOGGER);

        // create a java.util.logging.Logger
        // now java.util.logging.Logger should be created for each platform logger
        Logger logger = Logger.getLogger(BAR_LOGGER);
        logger.setLevel(Level.WARNING);

        PlatformLogger bar = PlatformLogger.getLogger(BAR_PLATFORM_LOGGER);
        checkPlatformLogger(bar, BAR_PLATFORM_LOGGER);

        // test the PlatformLogger methods
        testLogMethods(goo);
        testLogMethods(bar);

        checkLogger(FOO_PLATFORM_LOGGER, Level.FINER);
        checkLogger(BAR_PLATFORM_LOGGER, Level.FINER);

        checkLogger(GOO_PLATFORM_LOGGER, null);
        checkLogger(BAR_LOGGER, Level.WARNING);

        foo.setLevel(PlatformLogger.SEVERE);
        checkLogger(FOO_PLATFORM_LOGGER, Level.SEVERE);

        checkPlatformLoggerLevelEntanglements(foo);
        checkPlatformLoggerLevelEntanglements(bar);
    }

    private static void checkPlatformLogger(PlatformLogger logger, String name) {
        if (!logger.getName().equals(name)) {
            throw new RuntimeException("Invalid logger's name " +
                logger.getName() + " but expected " + name);
        }

        if (logger.getLevel() != null) {
            throw new RuntimeException("Invalid default level for logger " +
                logger.getName() + ": " + logger.getLevel());
        }

        if (logger.isLoggable(PlatformLogger.FINE) != false) {
            throw new RuntimeException("isLoggerable(FINE) returns true for logger " +
                logger.getName() + " but expected false");
        }

        logger.setLevel(PlatformLogger.FINER);
        if (logger.getLevel() != PlatformLogger.FINER) {
            throw new RuntimeException("Invalid level for logger " +
                logger.getName() + " " + logger.getLevel());
        }

        if (logger.isLoggable(PlatformLogger.FINE) != true) {
            throw new RuntimeException("isLoggerable(FINE) returns false for logger " +
                logger.getName() + " but expected true");
        }

        logger.info("OK: Testing log message");
    }

    private static void checkLogger(String name, Level level) {
        Logger logger = LogManager.getLogManager().getLogger(name);
        if (logger == null) {
            throw new RuntimeException("Logger " + name +
                " does not exist");
        }

        if (logger.getLevel() != level) {
            throw new RuntimeException("Invalid level for logger " +
                logger.getName() + " " + logger.getLevel());
        }
    }

    private static void testLogMethods(PlatformLogger logger) {
        logger.severe("Test severe(String, Object...) {0} {1}", new Long(1), "string");
        // test Object[]
        logger.severe("Test severe(String, Object...) {0}", (Object[]) getPoints());
        logger.warning("Test warning(String, Throwable)", new Throwable("Testing"));
        logger.info("Test info(String)");
    }

    private static final List<Level> levels = new ArrayList<>();

    static {
        for (Field levelField : Level.class.getDeclaredFields()) {
            int modifiers = levelField.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) &&
                Modifier.isFinal(modifiers) && levelField.getType() == Level.class) {
                try {
                    levels.add((Level) levelField.get(null));
                }
                catch (IllegalAccessException e) {
                    throw (Error) new IllegalAccessError(e.getMessage()).initCause(e);
                }
            }
        }
        Collections.sort(levels, new Comparator<Level>() {
            @Override
            public int compare(Level l1, Level l2) {
                return l1.intValue() < l2.intValue() ? -1 : (l1.intValue() > l2.intValue() ? 1 : 0);
            }
        });
    }

    private static void checkPlatformLoggerLevelEntanglements(PlatformLogger logger) {

        // check mappings
        for (Level level : levels) {
            checkPlatformLoggerLevelMapping(logger, level);
        }

        // check order
        PlatformLogger.Level[] platformLevels = PlatformLogger.Level.values();

        if (levels.size() != platformLevels.length) {
            throw new RuntimeException("There are " + platformLevels.length + " PlatformLogger.Level members, but " +
                                       levels.size() + " standard java.util.logging levels - the numbers should be equal.");
        }

        for (int i = 0; i < levels.size(); i++) {
            if (!levels.get(i).getName().equals(platformLevels[i].name())) {
                throw new RuntimeException("The order of PlatformLogger.Level members: " + Arrays.toString(platformLevels) +
                                           " is not consistent with java.util.logging.Level.intValue() ordering: " + levels);
            }
        }
    }

    private static void checkPlatformLoggerLevelMapping(PlatformLogger logger, Level level) {
        Field platformLevelField;
        PlatformLogger.Level platformLevel;
        try {
            platformLevelField = PlatformLogger.class.getDeclaredField(level.getName());
            platformLevel = (PlatformLogger.Level) platformLevelField.get(null);
        }
        catch (Exception e) {
            throw new RuntimeException("No public static PlatformLogger." + level.getName() +
                                       " field", e);
        }

        if (!platformLevel.name().equals(level.getName()))
            throw new RuntimeException("The value of PlatformLogger." + level.getName() + ".name() is "
                                       + platformLevel.name() + " but expected " + level.getName());

        logger.setLevel(platformLevel);
        PlatformLogger.Level retrievedPlatformLevel = logger.getLevel();

        if (platformLevel != retrievedPlatformLevel)
            throw new RuntimeException("Retrieved PlatformLogger level " + retrievedPlatformLevel +
                                       " is not the same as set level " + platformLevel);

        Logger javaLogger = LogManager.getLogManager().getLogger(logger.getName());
        Level javaLevel = javaLogger.getLevel();
        if (javaLogger.getLevel() != level)
            throw new RuntimeException("Retrieved backing java.util.logging.Logger level " + javaLevel +
                                        " is not the expected " + level);
    }

    static Point[] getPoints() {
        Point[] res = new Point[3];
        res[0] = new Point(0,0);
        res[1] = new Point(1,1);
        res[2] = new Point(2,2);
        return res;
    }

    static class Point {
        final int x;
        final int y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public String toString() {
            return "{x="+x + ", y=" + y + "}";
        }
    }

}
