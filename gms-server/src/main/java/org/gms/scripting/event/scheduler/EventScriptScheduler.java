/*
 This file is part of the HeavenMS MapleStory Server
 Copyleft (L) 2016 - 2019 RonanLana

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gms.scripting.event.scheduler;

import org.gms.config.GameConfig;
import org.gms.net.server.Server;
import org.gms.server.ThreadManager;
import org.gms.server.TimerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ronan
 */
public class EventScriptScheduler {
    private static final Logger log = LoggerFactory.getLogger(EventScriptScheduler.class);

    private boolean disposed = false;
    private int idleProcs = 0;
    private final Map<Runnable, Long> registeredEntries = new HashMap<>();

    private ScheduledFuture<?> schedulerTask = null;
    private final Lock schedulerLock = new ReentrantLock(true);

    private void runBaseSchedule() {
        List<Runnable> toRemove;
        Map<Runnable, Long> registeredEntriesCopy;

        schedulerLock.lock();
        try {
            if (registeredEntries.isEmpty()) {
                idleProcs++;

                if (idleProcs >= GameConfig.getServerInt("mob_status_monitor_idle")) {
                    if (schedulerTask != null) {
                        schedulerTask.cancel(false);
                        schedulerTask = null;
                    }
                }

                return;
            }

            idleProcs = 0;
            registeredEntriesCopy = new HashMap<>(registeredEntries);
        } finally {
            schedulerLock.unlock();
        }

        long timeNow = Server.getInstance().getCurrentTime();
        toRemove = new LinkedList<>();
        for (Entry<Runnable, Long> rmd : registeredEntriesCopy.entrySet()) {
            if (rmd.getValue() < timeNow) {
                Runnable r = rmd.getKey();

                try {
                    r.run();  // 运行计划任务
                } catch (Exception e) {
                    // 记录异常但继续处理其他任务
                    log.error("[事件脚本] 计划任务执行失败，任务: {}", r, e);
                } finally {
                    // 无论任务是否成功执行，都标记为需要移除
                    toRemove.add(r);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            schedulerLock.lock();
            try {
                for (Runnable r : toRemove) {
                    registeredEntries.remove(r);
                }
            } finally {
                schedulerLock.unlock();
            }
        }
    }

    public void registerEntry(final Runnable scheduledAction, final long duration) {

        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                idleProcs = 0;
                if (schedulerTask == null) {
                    if (disposed) {
                        return;
                    }

                    schedulerTask = TimerManager.getInstance().register(this::runBaseSchedule, GameConfig.getServerLong("mob_status_monitor_proc"), GameConfig.getServerLong("mob_status_monitor_proc"));
                }

                registeredEntries.put(scheduledAction, Server.getInstance().getCurrentTime() + duration);
            } finally {
                schedulerLock.unlock();
            }
        });
    }

    public void cancelEntry(final Runnable scheduledAction) {

        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                registeredEntries.remove(scheduledAction);
            } finally {
                schedulerLock.unlock();
            }
        });
    }

    public void dispose() {

        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                if (schedulerTask != null) {
                    schedulerTask.cancel(false);
                    schedulerTask = null;
                }

                registeredEntries.clear();
                disposed = true;
            } finally {
                schedulerLock.unlock();
            }
        });
    }
}
