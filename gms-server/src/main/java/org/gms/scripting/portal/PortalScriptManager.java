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
package org.gms.scripting.portal;

import org.gms.client.Client;
import org.gms.config.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gms.scripting.AbstractScriptManager;
import org.gms.server.maps.Portal;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class PortalScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(PortalScriptManager.class);
    private static final PortalScriptManager instance = new PortalScriptManager();

    private final Map<String, PortalScript> scripts = new HashMap<>();

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private PortalScript getPortalScript(String scriptName) throws ScriptException {
        String scriptPath = "portal/" + scriptName + ".js";
        PortalScript script = scripts.get(scriptPath);
        if (script != null) {
            return script;
        }

        ScriptEngine engine = getInvocableScriptEngine(scriptPath);
        if (!(engine instanceof Invocable iv)) {
            return null;
        }

        script = iv.getInterface(PortalScript.class);
        if (script == null) {
            throw new ScriptException(String.format("[传送门] 脚本 %s 未实现 PortalScript 接口", scriptName));
        }

        scripts.put(scriptPath, script);
        return script;
    }

    public boolean executePortalScript(Portal portal, Client c) {
        try {
            String strPortalName = portal.getScriptName();
            PortalScript script = getPortalScript(strPortalName);

            if (GameConfig.getServerBoolean("use_debug") && c.getPlayer().isGM())  {
                if (script != null) {
                    c.getPlayer().dropMessage("[传送门] 已与脚本 " + strPortalName + " 建立关联。");
                } else {
                    c.getPlayer().dropMessage(5,"[传送门] 脚本 " + strPortalName + " 不存在。");
                    log.warn("[传送门] 脚本 {} 不存在" ,strPortalName);
                }
            }
            if (script != null) {
                return script.enter(new PortalPlayerInteraction(c, portal));
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage(5,"[传送门] 脚本 " + portal.getScriptName() + " 执行错误。");
            log.warn("[传送门] 脚本 {} 执行出错 ", portal.getScriptName(), e);
        }
        return false;
    }

    public void reloadPortalScripts() {
        scripts.clear();
    }
}