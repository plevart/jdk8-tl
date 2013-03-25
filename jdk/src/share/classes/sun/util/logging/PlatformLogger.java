/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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


package sun.util.logging;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;

/**
 * Platform logger provides an API for the JRE components to log
 * messages.  This enables the runtime components to eliminate the
 * static dependency of the logging facility and also defers the
 * java.util.logging initialization until it is enabled.
 * In addition, the PlatformLogger API can be used if the logging
 * module does not exist.
 *
 * If the logging facility is not enabled, the platform loggers
 * will output log messages per the default logging configuration
 * (see below). In this implementation, it does not log the
 * the stack frame information issuing the log message.
 *
 * When the logging facility is enabled (at startup or runtime),
 * the java.util.logging.Logger will be created for each platform
 * logger and all log messages will be forwarded to the Logger
 * to handle.
 *
 * Logging facility is "enabled" when one of the following
 * conditions is met:
 * 1) a system property "java.util.logging.config.class" or
 *    "java.util.logging.config.file" is set
 * 2) java.util.logging.LogManager or java.util.logging.Logger
 *    is referenced that will trigger the logging initialization.
 *
 * Default logging configuration:
 *   global logging level = INFO
 *   handlers = java.util.logging.ConsoleHandler
 *   java.util.logging.ConsoleHandler.level = INFO
 *   java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
 *
 * Limitation:
 * <JAVA_HOME>/lib/logging.properties is the system-wide logging
 * configuration defined in the specification and read in the
 * default case to configure any java.util.logging.Logger instances.
 * Platform loggers will not detect if <JAVA_HOME>/lib/logging.properties
 * is modified. In other words, unless the java.util.logging API
 * is used at runtime or the logging system properties is set,
 * the platform loggers will use the default setting described above.
 * The platform loggers are designed for JDK developers use and
 * this limitation can be workaround with setting
 * -Djava.util.logging.config.file system property.
 *
 * @since 1.7
 */
public class PlatformLogger {
    // Same values as java.util.logging.Level for easy mapping
    public static final int OFF     = Integer.MAX_VALUE;
    public static final int SEVERE  = 1000;
    public static final int WARNING = 900;
    public static final int INFO    = 800;
    public static final int CONFIG  = 700;
    public static final int FINE    = 500;
    public static final int FINER   = 400;
    public static final int FINEST  = 300;
    public static final int ALL     = Integer.MIN_VALUE;

    /**
     * Private enum for converting among level names, PlatformLogger level values
     * and java.util.logging.Level objects
     */
    private static enum LevelEnum {
        OFF(PlatformLogger.OFF),
        SEVERE(PlatformLogger.SEVERE),
        WARNING(PlatformLogger.WARNING),
        INFO(PlatformLogger.INFO),
        CONFIG(PlatformLogger.CONFIG),
        FINE(PlatformLogger.FINE),
        FINER(PlatformLogger.FINER),
        FINEST(PlatformLogger.FINEST),
        ALL(PlatformLogger.ALL),
        UNKNOWN(0);

        /** PlatformLogger level as int */
        final int level;

        private LevelEnum(int level) {
            this.level = level;
        }

        static LevelEnum valueOf(int level) {
            // higher occurences first (finest, fine, finer, info)
            // based on isLoggable(level) calls (03/20/2013)
            // in jdk project only (including generated sources)
            switch (level) {
                case PlatformLogger.FINEST:  return FINEST;
                case PlatformLogger.FINE:    return FINE;
                case PlatformLogger.FINER:   return FINER;
                case PlatformLogger.INFO:    return INFO;
                case PlatformLogger.WARNING: return WARNING;
                case PlatformLogger.CONFIG:  return CONFIG;
                case PlatformLogger.SEVERE:  return SEVERE;
                case PlatformLogger.OFF:     return OFF;
                case PlatformLogger.ALL:     return ALL;
                default:                     return UNKNOWN;
            }
        }

        /**
         * Associated java.util.logging.Level optionally initialized in
         * JavaLoggerProxy's static initializer and USED ONLY IN JavaLoggerProxy
         * (only once java.util.logging is available and enabled)
         */
        Object javaLevel;
    }

    private static final int defaultLevel = INFO;
    private static boolean loggingEnabled;
    static {
        loggingEnabled = AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    String cname = System.getProperty("java.util.logging.config.class");
                    String fname = System.getProperty("java.util.logging.config.file");
                    return (cname != null || fname != null);
                }
            });
    }

    // Table of known loggers.  Maps names to PlatformLoggers.
    private static Map<String,WeakReference<PlatformLogger>> loggers =
        new HashMap<>();

    /**
     * Returns a PlatformLogger of a given name.
     */
    public static synchronized PlatformLogger getLogger(String name) {
        PlatformLogger log = null;
        WeakReference<PlatformLogger> ref = loggers.get(name);
        if (ref != null) {
            log = ref.get();
        }
        if (log == null) {
            log = new PlatformLogger(name);
            loggers.put(name, new WeakReference<>(log));
        }
        return log;
    }

    /**
     * Initialize java.util.logging.Logger objects for all platform loggers.
     * This method is called from LogManager.readPrimordialConfiguration().
     */
    public static synchronized void redirectPlatformLoggers() {
        if (loggingEnabled || !LoggingSupport.isAvailable()) return;

        loggingEnabled = true;
        for (Map.Entry<String, WeakReference<PlatformLogger>> entry : loggers.entrySet()) {
            WeakReference<PlatformLogger> ref = entry.getValue();
            PlatformLogger plog = ref.get();
            if (plog != null) {
                plog.redirectToJavaLoggerProxy();
            }
        }
    }

    /**
     * Creates a new JavaLoggerProxy and redirects the platform logger to it
     */
    private void redirectToJavaLoggerProxy() {
        LoggerProxy loggerProxy = this.loggerProxy;
        JavaLoggerProxy javaLoggerProxy = new JavaLoggerProxy(loggerProxy.name, loggerProxy.effectiveLevel);
        // it is important to 1st set javaLoggerProxy (null -> not-null transition) ...
        this.javaLoggerProxy = javaLoggerProxy;
        // ...and only then change loggerProxy (not-null LoggerProxy -> not-null JavaLoggerProxy transition)
        // so that isLoggable is never called via isLoggableLoggerProxy call-site when loggerProxy references JavaLoggerProxy instance
        // and consequently doesn't thrash the monomorphic call-site.
        this.loggerProxy = javaLoggerProxy;
    }

    // LoggerProxy may be replaced with a JavaLoggerProxy object
    // when the logging facility is enabled
    private volatile LoggerProxy loggerProxy;
    // this field is initially null if logging is not enabled and is set
    // to JavaLoggerProxy object when the logging facility is enabled
    private volatile JavaLoggerProxy javaLoggerProxy;

    private PlatformLogger(String name) {
        if (loggingEnabled) {
            this.loggerProxy = this.javaLoggerProxy = new JavaLoggerProxy(name);
        } else {
            this.loggerProxy = new LoggerProxy(name);
        }
    }

    /**
     * A convenience method to test if the logger is turned off.
     * (i.e. its level is OFF).
     */
    public boolean isEnabled() {
        return loggerProxy.isEnabled();
    }

    /**
     * Gets the name for this platform logger.
     */
    public String getName() {
        return loggerProxy.name;
    }

    /**
     * Returns true if a message of the given level would actually
     * be logged by this logger.
     */
    public boolean isLoggable(int level) {
        try {
            return (boolean) isLoggableHM.invokeExact(javaLoggerProxy, loggerProxy, level);
        }
        catch (Throwable t) {
            throw unchecked(t);
        }
    }

    private static boolean isJavaLoggerProxyNotNull(JavaLoggerProxy javaLoggerProxy) {
        return javaLoggerProxy != null;
    }

    private static boolean isLoggableLoggerProxy(JavaLoggerProxy javaLoggerProxy, LoggerProxy loggerProxy, int level) {
        return loggerProxy.isLoggable(level);
    }

    private static boolean isLoggableJavaLoggerProxy(JavaLoggerProxy javaLoggerProxy, LoggerProxy loggerProxy, int level) {
        return javaLoggerProxy.isLoggable(level);
    }

    private static final MethodHandle isLoggableHM;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle isJavaLoggerProxyNotNullMH = lookup.findStatic(
                PlatformLogger.class,
                "isJavaLoggerProxyNotNull",
                MethodType.methodType(boolean.class, JavaLoggerProxy.class)
            );
            MethodType isLoggableMT = MethodType.methodType(boolean.class, JavaLoggerProxy.class, LoggerProxy.class, int.class);
            isLoggableHM = MethodHandles.guardWithTest(
                isJavaLoggerProxyNotNullMH,
                lookup.findStatic(PlatformLogger.class, "isLoggableJavaLoggerProxy", isLoggableMT),
                lookup.findStatic(PlatformLogger.class, "isLoggableLoggerProxy", isLoggableMT)
            );
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e.getMessage(), e);
        }
    }

    /**
     * Gets the current log level.  Returns 0 if the current effective level
     * is not set (equivalent to Logger.getLevel() returns null).
     */
    public int getLevel() {
        return loggerProxy.getLevel();
    }

    /**
     * Sets the log level.
     */
    public void setLevel(int newLevel) {
        loggerProxy.setLevel(newLevel);
    }

    /**
     * Logs a SEVERE message.
     */
    public void severe(String msg) {
        loggerProxy.doLog(SEVERE, msg);
    }

    public void severe(String msg, Throwable t) {
        loggerProxy.doLog(SEVERE, msg, t);
    }

    public void severe(String msg, Object... params) {
        loggerProxy.doLog(SEVERE, msg, params);
    }

    /**
     * Logs a WARNING message.
     */
    public void warning(String msg) {
        loggerProxy.doLog(WARNING, msg);
    }

    public void warning(String msg, Throwable t) {
        loggerProxy.doLog(WARNING, msg, t);
    }

    public void warning(String msg, Object... params) {
        loggerProxy.doLog(WARNING, msg, params);
    }

    /**
     * Logs an INFO message.
     */
    public void info(String msg) {
        loggerProxy.doLog(INFO, msg);
    }

    public void info(String msg, Throwable t) {
        loggerProxy.doLog(INFO, msg, t);
    }

    public void info(String msg, Object... params) {
        loggerProxy.doLog(INFO, msg, params);
    }

    /**
     * Logs a CONFIG message.
     */
    public void config(String msg) {
        loggerProxy.doLog(CONFIG, msg);
    }

    public void config(String msg, Throwable t) {
        loggerProxy.doLog(CONFIG, msg, t);
    }

    public void config(String msg, Object... params) {
        loggerProxy.doLog(CONFIG, msg, params);
    }

    /**
     * Logs a FINE message.
     */
    public void fine(String msg) {
        loggerProxy.doLog(FINE, msg);
    }

    public void fine(String msg, Throwable t) {
        loggerProxy.doLog(FINE, msg, t);
    }

    public void fine(String msg, Object... params) {
        loggerProxy.doLog(FINE, msg, params);
    }

    /**
     * Logs a FINER message.
     */
    public void finer(String msg) {
        loggerProxy.doLog(FINER, msg);
    }

    public void finer(String msg, Throwable t) {
        loggerProxy.doLog(FINER, msg, t);
    }

    public void finer(String msg, Object... params) {
        loggerProxy.doLog(FINER, msg, params);
    }

    /**
     * Logs a FINEST message.
     */
    public void finest(String msg) {
        loggerProxy.doLog(FINEST, msg);
    }

    public void finest(String msg, Throwable t) {
        loggerProxy.doLog(FINEST, msg, t);
    }

    public void finest(String msg, Object... params) {
        loggerProxy.doLog(FINEST, msg, params);
    }

    /**
     * Throws passed-in unchecked throwable or a checked throwable wrapped into UndeclaredThrowableException
     * @param t the {@link Throwable} to throw
     * @return nothing (never completes normally)
     */
    private static RuntimeException unchecked(Throwable t) {
        try {
            throw t;
        }
        catch (RuntimeException | Error unchecked) {
            throw unchecked;
        }
        catch (Throwable throwable) {
            throw new UndeclaredThrowableException(throwable);
        }
    }

    /**
     * Default platform logging support - output messages to
     * System.err - equivalent to ConsoleHandler with SimpleFormatter.
     */
    private static class LoggerProxy {
        private static final PrintStream defaultStream = System.err;

        final String name;
        volatile int levelValue;
        volatile int effectiveLevel = 0; // current effective level value

        LoggerProxy(String name) {
            this(name, defaultLevel);
        }

        LoggerProxy(String name, int level) {
            this.name = name;
            this.levelValue = (level == 0) ? defaultLevel : level;
        }

        boolean isEnabled() {
            return levelValue != OFF;
        }

        int getLevel() {
            return effectiveLevel;
        }

        void setLevel(int newLevel) {
            levelValue = newLevel;
            effectiveLevel = newLevel;
        }

        void doLog(int level, String msg) {
            if (!isLoggable(level)) {
                return;
            }
            defaultStream.print(format(level, msg, null));
        }

        void doLog(int level, String msg, Throwable thrown) {
            if (!isLoggable(level)) {
                return;
            }
            defaultStream.print(format(level, msg, thrown));
        }

        void doLog(int level, String msg, Object... params) {
            if (!isLoggable(level)) {
                return;
            }
            String newMsg = formatMessage(msg, params);
            defaultStream.print(format(level, newMsg, null));
        }

        public boolean isLoggable(int level) {
            int levelValue = this.levelValue;
            return level >= levelValue && levelValue != OFF;
        }

        // Copied from java.util.logging.Formatter.formatMessage
        private String formatMessage(String format, Object... parameters) {
            // Do the formatting.
            try {
                if (parameters == null || parameters.length == 0) {
                    // No parameters.  Just return format string.
                    return format;
                }
                // Is it a java.text style format?
                // Ideally we could match with
                // Pattern.compile("\\{\\d").matcher(format).find())
                // However the cost is 14% higher, so we cheaply check for
                // 1 of the first 4 parameters
                if (format.indexOf("{0") >= 0 || format.indexOf("{1") >=0 ||
                            format.indexOf("{2") >=0|| format.indexOf("{3") >=0) {
                    return java.text.MessageFormat.format(format, parameters);
                }
                return format;
            } catch (Exception ex) {
                // Formatting failed: use format string.
                return format;
            }
        }

        private static final String formatString =
            LoggingSupport.getSimpleFormat(false); // don't check logging.properties

        // minimize memory allocation
        private Date date = new Date();
        private synchronized String format(int level, String msg, Throwable thrown) {
            date.setTime(System.currentTimeMillis());
            String throwable = "";
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                thrown.printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }

            return String.format(formatString,
                date,
                getCallerInfo(),
                name,
                LevelEnum.valueOf(level).name(),
                msg,
                throwable);
        }

        // Returns the caller's class and method's name; best effort
        // if cannot infer, return the logger's name.
        private String getCallerInfo() {
            String sourceClassName = null;
            String sourceMethodName = null;

            JavaLangAccess access = SharedSecrets.getJavaLangAccess();
            Throwable throwable = new Throwable();
            int depth = access.getStackTraceDepth(throwable);

            String logClassName = "sun.util.logging.PlatformLogger";
            boolean lookingForLogger = true;
            for (int ix = 0; ix < depth; ix++) {
                // Calling getStackTraceElement directly prevents the VM
                // from paying the cost of building the entire stack frame.
                StackTraceElement frame =
                    access.getStackTraceElement(throwable, ix);
                String cname = frame.getClassName();
                if (lookingForLogger) {
                    // Skip all frames until we have found the first logger frame.
                    if (cname.equals(logClassName)) {
                        lookingForLogger = false;
                    }
                } else {
                    if (!cname.equals(logClassName)) {
                        // We've found the relevant frame.
                        sourceClassName = cname;
                        sourceMethodName = frame.getMethodName();
                        break;
                    }
                }
            }

            if (sourceClassName != null) {
                return sourceClassName + " " + sourceMethodName;
            } else {
                return name;
            }
        }
    }

    /**
     * JavaLoggerProxy forwards all the calls to its corresponding
     * java.util.logging.Logger object.
     */
    private static class JavaLoggerProxy extends LoggerProxy {

        /**
         * A map from java.util.logging.Level objects to LevelEnum members
         * used to speed-up {@link #getLevel()} method.
         */
        private static final Map<Object, LevelEnum> javaLevelToEnum = new IdentityHashMap<>();

        static {
            if (LoggingSupport.isAvailable()) {
                for (LevelEnum levelEnum : EnumSet.complementOf(EnumSet.of(LevelEnum.UNKNOWN))) {
                    Object javaLevel = LoggingSupport.parseLevel(levelEnum.name());
                    levelEnum.javaLevel = javaLevel;
                    javaLevelToEnum.put(javaLevel, levelEnum);
                }
            }
        }

        /** java.util.logging.Logger */
        private final Object javaLogger;

        JavaLoggerProxy(String name) {
            this(name, 0);
        }

        JavaLoggerProxy(String name, int level) {
            super(name, level);
            this.javaLogger = LoggingSupport.getLogger(name);
            if (level != 0) {
                // level has been updated and so set the Logger's level
                LoggingSupport.setLevel(javaLogger, javaLevel(level));
            }
        }

       /**
        * Let Logger.log() do the filtering since if the level of a
        * platform logger is altered directly from
        * java.util.logging.Logger.setLevel(), the levelValue will
        * not be updated.
        */
        void doLog(int level, String msg) {
            LoggingSupport.log(javaLogger, javaLevel(level), msg);
        }

        void doLog(int level, String msg, Throwable t) {
            LoggingSupport.log(javaLogger, javaLevel(level), msg, t);
        }

        void doLog(int level, String msg, Object... params) {
            if (!isLoggable(level)) {
                return;
            }
            // only pass String objects to the j.u.l.Logger which may
            // be created by untrusted code
            int len = (params != null) ? params.length : 0;
            Object[] sparams = new String[len];
            for (int i = 0; i < len; i++) {
                sparams [i] = String.valueOf(params[i]);
            }
            LoggingSupport.log(javaLogger, javaLevel(level), msg, sparams);
        }

        boolean isEnabled() {
            Object javaLevel = LoggingSupport.getLevel(javaLogger);
            return javaLevel == null || !javaLevel.equals(LevelEnum.OFF.javaLevel);
        }

        int getLevel() {
            Object javaLevel = LoggingSupport.getLevel(javaLogger);
            LevelEnum levelEnum = javaLevelToEnum.get(javaLevel);
            return levelEnum == null ? 0 : levelEnum.level;
        }

        void setLevel(int newLevel) {
            levelValue = newLevel;
            LoggingSupport.setLevel(javaLogger, javaLevel(newLevel));
        }

        public boolean isLoggable(int level) {
            return LoggingSupport.isLoggable(javaLogger, javaLevel(level));
        }

        private static Object javaLevel(int level) {
            return LevelEnum.valueOf(level).javaLevel;
        }
    }
}
