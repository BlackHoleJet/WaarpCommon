/**
   This file is part of GoldenGate Project (named also GoldenGate or GG).

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All GoldenGate Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   GoldenGate is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GoldenGate .  If not, see <http://www.gnu.org/licenses/>.
 */
package goldengate.cpu;

import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;

/**
 * @author bregier
 * 
 */
public class CpuManagementSysmon implements CpuManagementInterface {
    public static long delay = 1000;

    JavaSysMon sysMon;

    CpuTimes cpuTimesOld;
    CpuTimes cpuTimesOldNext;
    long time;
    
    /**
     * 
     * @throws UnsupportedOperationException
     *             if System Load Average is not supported
     */
    public CpuManagementSysmon() throws UnsupportedOperationException {
        sysMon = new JavaSysMon();
        cpuTimesOld = sysMon.cpuTimes();
        cpuTimesOldNext = cpuTimesOld;
        time = System.currentTimeMillis();
    }

    /**
     * 
     * @return the load average
     */
    public double getLoadAverage() {
        long newTime = System.currentTimeMillis();
        CpuTimes cpuTimes = sysMon.cpuTimes();
        double rate = cpuTimes.getCpuUsage(cpuTimesOld);
        long delta = newTime - time;
        if ((delta) > delay) {
            if ((delta) > 10*delay) {
                time = newTime;
                cpuTimesOldNext = cpuTimes;
                cpuTimesOld = cpuTimes;
            } else {
                time = newTime;
                cpuTimesOldNext = cpuTimes;
                cpuTimesOld = cpuTimesOldNext;
            }
        }
        return rate;
    }

    public static void main(String[] args) {
        long total = 0;
        CpuManagementSysmon cpuManagement = new CpuManagementSysmon();
        System.err.println("LA: " + cpuManagement.getLoadAverage());
        for (int i = 0; i < 1000 * 1000 * 1000; i ++) {
            // keep ourselves busy for a while ...
            // note: we had to add some "work" into the loop or Java 6
            // optimizes it away. Thanks to Daniel Einspanjer for
            // pointing that out.
            total += i;
            total *= 10;
        }
        System.err.println("LA: " + cpuManagement.getLoadAverage());

        total = 0;
        for (int i = 0; i < 1000 * 1000 * 1000; i ++) {
            // keep ourselves busy for a while ...
            // note: we had to add some "work" into the loop or Java 6
            // optimizes it away. Thanks to Daniel Einspanjer for
            // pointing that out.
            total += i;
            total *= 10;
        }
        System.err.println("LA: " + cpuManagement.getLoadAverage());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        System.err.println("LA: " + cpuManagement.getLoadAverage());

        total = 0;
        for (int i = 0; i < 1000 * 1000 * 1000; i ++) {
            // keep ourselves busy for a while ...
            // note: we had to add some "work" into the loop or Java 6
            // optimizes it away. Thanks to Daniel Einspanjer for
            // pointing that out.
            total += i;
            total *= 10;
        }
        System.err.println("LA: " + cpuManagement.getLoadAverage());
    }
}
