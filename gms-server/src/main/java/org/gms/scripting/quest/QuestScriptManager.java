/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package org.gms.scripting.quest;

import org.gms.client.Client;
import org.gms.client.QuestStatus;
import org.gms.config.GameConfig;
import org.gms.constants.game.GameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gms.scripting.AbstractScriptManager;
import org.gms.server.quest.Quest;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author RMZero213
 */
public class QuestScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(QuestScriptManager.class);
    private static final QuestScriptManager instance = new QuestScriptManager();

    private final Map<Client, QuestActionManager> qms = new HashMap<>();
    private final Map<Client, Invocable> scripts = new HashMap<>();

    public static QuestScriptManager getInstance() {
        return instance;
    }

    private ScriptEngine getQuestScriptEngine(Client c, short questid) {
        ScriptEngine engine = getInvocableScriptEngine("quest/" + questid + ".js", c);
        if (engine == null && GameConstants.isMedalQuest(questid)) {
            engine = getInvocableScriptEngine("quest/medalQuest.js", c);   // start generic medal quest
        }

        return engine;
    }

    public void start(Client c, short questid, int npc) {
        Quest quest = Quest.getInstance(questid);
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, true);
            if (qms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                qms.put(c, qm);

                /*if (!quest.hasScriptRequirement(false)) {   // lack of scripted quest checks found thanks to Mali, Resinate
                    qm.dispose();
                    return;
                }*/

                ScriptEngine engine = getQuestScriptEngine(c, questid);
                if (engine == null) {
                    log.warn("玩家 {} 尝试开始任务 {}({}) ，但是脚本不存在。", c.getPlayer().getName(), quest.getName(),questid);
                    if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                        c.getPlayer().dropMessage(5,"任务脚本 " + quest.getName() + "(" + questid + ".js) 不存在。");
                    }
                    qm.dispose();
                    return;
                } else if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(0,"与任务脚本 " + quest.getName() + "(" + questid + ".js) 的 start() 建立了联系。");
                }

                engine.put("qm", qm);

                Invocable iv = (Invocable) engine;
                scripts.put(c, iv);
                c.setClickedNPC();
                iv.invokeFunction("start", (byte) 1, (byte) 0, 0);
            }
        } catch (final Throwable t) {
            log.error("执行任务脚本 {}.js 的 strat() 错误: ", questid, t);
            if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(5,"任务脚本 " + questid + ".js 执行出错。");
            }
            dispose(c);
        }
    }

    public void start(Client c, byte mode, byte type, int selection) {
        Invocable iv = scripts.get(c);
        if (iv != null) {
            try {
                c.setClickedNPC();
                iv.invokeFunction("start", mode, type, selection);
            } catch (final Exception e) {
                log.error("执行任务脚本 {} 的 strat({}, {} ,{}) 错误: ", getQM(c).getQuest(), mode, type, selection, e);
                if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5,"任务脚本 " +  getQM(c).getQuest() + ".js 执行出错。");
                }
                dispose(c);
            }
        }
    }

    public void end(Client c, short questid, int npc) {
        Quest quest = Quest.getInstance(questid);
        if (!c.getPlayer().getQuest(quest).getStatus().equals(QuestStatus.Status.STARTED) || (!c.getPlayer().getMap().containsNPC(npc) && !quest.isAutoComplete())) {
            dispose(c);
            return;
        }
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, false);
            if (qms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                qms.put(c, qm);

                /*if (!quest.hasScriptRequirement(true)) {
                    qm.dispose();
                    return;
                }*/

                ScriptEngine engine = getQuestScriptEngine(c, questid);
                if (engine == null) {
                    log.warn("玩家 {} 尝试完成任务 {}({}) ，但是脚本不存在。", c.getPlayer().getName(), quest.getName(),questid);
                    if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                        c.getPlayer().dropMessage(5,"任务脚本 " + quest.getName() + "(" + questid + ".js) 不存在。");
                    }
                    qm.dispose();
                    return;
                } else if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(0,"与任务脚本 " + quest.getName() + "(" + questid + ".js) 的 end() 建立了联系。");
                }

                engine.put("qm", qm);

                Invocable iv = (Invocable) engine;
                scripts.put(c, iv);
                c.setClickedNPC();
                iv.invokeFunction("end", (byte) 1, (byte) 0, 0);
            }
        } catch (final Throwable t) {
            log.error("执行任务脚本 {} 的 end() 错误: ", questid, t);
            if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(5,"任务脚本 " + questid + " 执行出错。");
            }
            dispose(c);
        }
    }

    public void end(Client c, byte mode, byte type, int selection) {
        Invocable iv = scripts.get(c);
        if (iv != null) {
            try {
                c.setClickedNPC();
                iv.invokeFunction("end", mode, type, selection);
            } catch (final Exception e) {
                log.error("执行任务脚本 {} 的 end({}, {} ,{}) 错误: ", getQM(c).getQuest(), mode, type, selection, e);
                if (GameConfig.getServerBoolean("use_debug")) {
                    if (c.getPlayer().isGM()) c.getPlayer().dropMessage(5,"任务脚本 " + getQM(c).getQuest() + " 执行出错。");
                }
                dispose(c);
            }
        }
    }

    public void raiseOpen(Client c, short questid, int npc) {
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, true);
            if (qms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                qms.put(c, qm);

                ScriptEngine engine = getQuestScriptEngine(c, questid);
                if (engine == null) {
                    log.warn("玩家 {} 尝试启动操作任务 {} ，但是脚本不存在。", c.getPlayer().getName(), questid);
                    if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                        c.getPlayer().dropMessage(5,"任务脚本 " + questid + " 不存在。");
                    }
                    //FilePrinter.printError(FilePrinter.QUEST_UNCODED, "RAISE Quest " + questid + " is uncoded.");
                    qm.dispose();
                    return;
                } else if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(0,"任务脚本 " + questid + ".js 启动操作了。");
                }

                engine.put("qm", qm);

                Invocable iv = (Invocable) engine;
                scripts.put(c, iv);
                c.setClickedNPC();
                iv.invokeFunction("raiseOpen");
            }
        } catch (final Throwable t) {
            log.error("执行任务脚本启动操作时出错（任务ID：{}）", questid, t);
            if (GameConfig.getServerBoolean("use_debug")) {
                if (c.getPlayer().isGM()) c.getPlayer().dropMessage(5,"任务脚本 " + questid + " 执行出错。");
            }
            dispose(c);
        }
    }

    public void dispose(QuestActionManager qm, Client c) {
        qms.remove(c);
        scripts.remove(c);
        c.getPlayer().setNpcCooldown(System.currentTimeMillis());
        resetContext("quest/" + qm.getQuest() + ".js", c);
        c.getPlayer().flushDelayedUpdateQuests();
    }

    public void dispose(Client c) {
        QuestActionManager qm = qms.get(c);
        if (qm != null) {
            dispose(qm, c);
        }
    }

    public QuestActionManager getQM(Client c) {
        return qms.get(c);
    }

    public void reloadQuestScripts() {
        scripts.clear();
        qms.clear();
    }

    public boolean checkFunctionExists(Client c, short questid, int npc, String functionName) {
        ScriptEngine engine = getQuestScriptEngine(c, questid);
        if (engine == null) {
            return false;
        }
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, false);
            engine.put("qm", qm);
            String script = "function checkFunction(funcName) { return typeof this[funcName] === 'function'; }";
            engine.eval(script);

            Invocable invocable = (Invocable) engine;
            boolean exists = (Boolean) invocable.invokeFunction("checkFunction", functionName);

            qm.dispose();
            return exists;
        } catch (ScriptException | NoSuchMethodException e) {
            e.printStackTrace();
            dispose(c);
        }
        return false;
    }


}
