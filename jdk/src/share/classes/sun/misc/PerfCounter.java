/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.misc;

import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.security.AccessController;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Performance counter support for internal JRE classes.
 * This class defines a fixed list of counters for the platform
 * to use as an interim solution until RFE# 6209222 is implemented.
 * The perf counters will be created in the jvmstat perf buffer
 * that the HotSpot VM creates. The default size is 32K and thus
 * the number of counters is bounded.  You can alter the size
 * with -XX:PerfDataMemorySize=<bytes> option. If there is
 * insufficient memory in the jvmstat perf buffer, the C heap memory
 * will be used and thus the application will continue to run if
 * the counters added exceeds the buffer size but the counters
 * will be missing.
 *
 * See HotSpot jvmstat implementation for certain circumstances
 * that the jvmstat perf buffer is not supported.
 *
 */
public class PerfCounter {
    private static final Perf perf =
        AccessController.doPrivileged(new Perf.GetPerfAction());
    private static final Unsafe unsafe =
        Unsafe.getUnsafe();
    private static final boolean VM_SUPPORTS_LONG_CAS;
    private static final boolean BIG_ENDIAN_ORDER = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    static {
        try {
            Field f = AtomicLong.class.getDeclaredField("VM_SUPPORTS_LONG_CAS");
            f.setAccessible(true);
            VM_SUPPORTS_LONG_CAS = (boolean) f.get(null);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    // Must match values defined in hotspot/src/share/vm/runtime/perfdata.hpp
    private final static int V_Constant  = 1;
    private final static int V_Monotonic = 2;
    private final static int V_Variable  = 3;
    private final static int U_None      = 1;

    private final String name;
    private final LongBuffer lb;
    private final boolean isDirect;
    private final long address, loAddress, hiAddress;

    private PerfCounter(String name, int type) {
        this.name = name;
        ByteBuffer bb = perf.createLong(name, U_None, type, 0L);
        bb.order(ByteOrder.nativeOrder());
        this.lb = bb.asLongBuffer();
        if (bb instanceof DirectBuffer) {
            isDirect = true;
            address = ((DirectBuffer) bb).address();
            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                hiAddress = address;
                loAddress = address + 4;
            }
            else {
                hiAddress = address + 4;
                loAddress = address;
            }
        }
        else
        {
            isDirect = false;
            hiAddress = loAddress = address = 0L;
        }
    }

    static PerfCounter newPerfCounter(String name) {
        return new PerfCounter(name, V_Variable);
    }

    static PerfCounter newConstantPerfCounter(String name) {
        PerfCounter c = new PerfCounter(name, V_Constant);
        return c;
    }

    /**
     * Returns the current value of the perf counter.
     */
    public long get() {
        if (isDirect) {
            return unsafe.getLongVolatile(null, address);
        }
        else {
            synchronized (this) {
                return lb.get(0);
            }
        }
    }

    /**
     * Sets the value of the perf counter to the given newValue.
     */
    public void set(long newValue) {
        if (isDirect) {
            unsafe.putOrderedLong(null, address, newValue);
        }
        else {
            synchronized (this) {
                lb.put(0, newValue);
            }
        }
    }

    /**
     * Adds the given value to the perf counter.
     */
    public void add(long value) {
        if (isDirect) {
            if (VM_SUPPORTS_LONG_CAS) {
                unsafe.getAndAddLong(null, address, value);
            }
            else {
                int deltaLo = (int)(value);
                int oldLo;
                do {
                    oldLo = unsafe.getIntVolatile(null, loAddress);
                } while (!unsafe.compareAndSwapInt(null, loAddress, oldLo, oldLo + deltaLo));

                int deltaHi = (int)((value + ((long)oldLo & 0xFFFFFFFFL)) >>> 32);
                if (deltaHi != 0) {
                    int oldHi;
                    do {
                        oldHi = unsafe.getIntVolatile(null, hiAddress);
                    } while (!unsafe.compareAndSwapInt(null, hiAddress, oldHi, oldHi + deltaHi));
                }
            }
        }
        else {
            synchronized (this) {
                long res = lb.get(0) + value;
                lb.put(0, res);
            }
        }
    }

    /**
     * Increments the perf counter with 1.
     */
    public void increment() {
        add(1);
    }

    /**
     * Adds the given interval to the perf counter.
     */
    public void addTime(long interval) {
        add(interval);
    }

    /**
     * Adds the elapsed time from the given start time (ns) to the perf counter.
     */
    public void addElapsedTimeFrom(long startTime) {
        add(System.nanoTime() - startTime);
    }

    @Override
    public String toString() {
        return name + " = " + get();
    }

    static class CoreCounters {
        static final PerfCounter pdt   = newPerfCounter("sun.classloader.parentDelegationTime");
        static final PerfCounter lc    = newPerfCounter("sun.classloader.findClasses");
        static final PerfCounter lct   = newPerfCounter("sun.classloader.findClassTime");
        static final PerfCounter rcbt  = newPerfCounter("sun.urlClassLoader.readClassBytesTime");
        static final PerfCounter zfc   = newPerfCounter("sun.zip.zipFiles");
        static final PerfCounter zfot  = newPerfCounter("sun.zip.zipFile.openTime");
    }

    static class WindowsClientCounters {
        static final PerfCounter d3dAvailable = newConstantPerfCounter("sun.java2d.d3d.available");
    }

    /**
     * Number of findClass calls
     */
    public static PerfCounter getFindClasses() {
        return CoreCounters.lc;
    }

    /**
     * Time (ns) spent in finding classes that includes
     * lookup and read class bytes and defineClass
     */
    public static PerfCounter getFindClassTime() {
        return CoreCounters.lct;
    }

    /**
     * Time (ns) spent in finding classes
     */
    public static PerfCounter getReadClassBytesTime() {
        return CoreCounters.rcbt;
    }

    /**
     * Time (ns) spent in the parent delegation to
     * the parent of the defining class loader
     */
    public static PerfCounter getParentDelegationTime() {
        return CoreCounters.pdt;
    }

    /**
     * Number of zip files opened.
     */
    public static PerfCounter getZipFileCount() {
        return CoreCounters.zfc;
    }

    /**
     * Time (ns) spent in opening the zip files that
     * includes building the entries hash table
     */
    public static PerfCounter getZipFileOpenTime() {
        return CoreCounters.zfot;
    }

    /**
     * D3D graphic pipeline available
     */
    public static PerfCounter getD3DAvailable() {
        return WindowsClientCounters.d3dAvailable;
    }
}
