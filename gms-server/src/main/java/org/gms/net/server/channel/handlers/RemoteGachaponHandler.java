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
package org.gms.net.server.channel.handlers;

import org.gms.client.Client;
import org.gms.client.autoban.AutobanFactory;
import org.gms.constants.id.ItemId;
import org.gms.constants.id.NpcId;
import org.gms.constants.inventory.ItemConstants;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.scripting.npc.NPCScriptManager;

/**
 * @author Generic<br>
 * 远程扭蛋券处理器类<br>
 * 处理客户端发送的远程扭蛋券使用请求
 */
public final class RemoteGachaponHandler extends AbstractPacketHandler {

    /**
     * 处理远程扭蛋券数据包
     * @param p 输入数据包
     * @param c 客户端连接
     */
    @Override
    public final void handlePacket(InPacket p, Client c) {
        // 读取数据包中的票券ID和扭蛋机类型
        int ticket = p.readInt();
        int gacha = p.readInt();
        // 验证票券ID是否合法
        if (ticket != ItemId.REMOTE_GACHAPON_TICKET) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), "尝试使用非法物品ID调用远程扭蛋处理器: " + ticket);
            c.disconnect(false, false);
            return;
        }
        // 验证扭蛋机类型是否在合法范围内(0-11)
        else if (gacha < 0 || gacha > 11) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), "尝试使用非法模式调用远程扭蛋处理器: " + gacha);
            c.disconnect(false, false);
            return;
        }
        // 验证玩家是否拥有扭蛋券
        else if (c.getPlayer().getInventory(ItemConstants.getInventoryType(ticket)).countById(ticket) < 1) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), "尝试在没有扭蛋券的情况下使用远程扭蛋处理器");
            c.disconnect(false, false);
            return;
        }
        // 根据扭蛋机类型确定对应的NPC ID
        int npcId = NpcId.GACHAPON_HENESYS; // 默认设置为弓箭手村的扭蛋机
        if (gacha != 8 && gacha != 9) {
            // 对于大多数类型，NPC ID是基础ID加上偏移量
            npcId += gacha;
        } else {
            // 特殊处理类型8和9
            npcId = gacha == 8 ? NpcId.GACHAPON_NLC : NpcId.GACHAPON_NAUTILUS;
        }

        // 启动对应的扭蛋机NPC脚本
        NPCScriptManager.getInstance().start(c, npcId, "gachaponRemote", null);
    }
}