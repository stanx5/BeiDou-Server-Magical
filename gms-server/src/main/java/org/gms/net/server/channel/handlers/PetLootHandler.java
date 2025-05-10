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

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.client.inventory.Pet;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.server.maps.MapItem;
import org.gms.server.maps.MapObject;
import org.gms.util.PacketCreator;

import java.util.Set;

/**
 * 处理宠物自动拾取物品的数据包处理器
 * @extends AbstractPacketHandler 继承自抽象数据包处理器
 */
public final class PetLootHandler extends AbstractPacketHandler {
    /**
     * 处理客户端发送的宠物拾取物品请求
     * @param {InPacket} p 输入数据包对象
     * @param {Client} c 客户端连接对象
     */
    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer(); // 获取当前玩家角色对象

        int petIndex = chr.getPetIndex(p.readInt()); // 从数据包读取宠物唯一标识并转换为槽位索引
        Pet pet = chr.getPet(petIndex);             // 根据索引获取宠物实例
        if (pet == null || !pet.isSummoned()) {     // 检查宠物是否存在且处于召唤状态
            c.sendPacket(PacketCreator.enableActions()); // 发送允许玩家行动的包
            return;                                  // 终止处理流程
        }

        p.skip(13);                    // 跳过13字节（可能包含坐标校验等附加信息）
        int oid = p.readInt();                      // 读取地图物品对象ID
        MapObject ob = chr.getMap().getMapObject(oid); // 根据ID获取地图物品对象
        try {
            MapItem mapitem = (MapItem) ob;          // 将地图对象强制转换为物品对象
            if (mapitem.getMeso() > 0) {            // 判断拾取的是游戏币（MESO）
                if (!chr.isEquippedMesoMagnet()) {   // 检查是否装备吸金币道具
                    c.sendPacket(PacketCreator.enableActions());
                    return;                          // 不允许拾取则提前返回
                }

                if (chr.isEquippedPetItemIgnore()) { // 检查是否启用道具过滤
                    final Set<Integer> petIgnore = chr.getExcludedItems(); // 获取过滤列表
                    if (!petIgnore.isEmpty() && petIgnore.contains(Integer.MAX_VALUE)) {// 检查是否过滤金币（Integer.MAX_VALUE为金币过滤标识）
                        c.sendPacket(PacketCreator.enableActions());
                        return;                      // 过滤金币时中断拾取
                    }
                }
            } else {                                // 处理普通物品拾取
                if (!chr.isEquippedItemPouch()) {    // 检查是否装备物品袋道具
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                if (chr.isEquippedPetItemIgnore()) { // 检查是否启用道具过滤
                    final Set<Integer> petIgnore = chr.getExcludedItems();
                    if (!petIgnore.isEmpty() && petIgnore.contains(mapitem.getItem().getItemId())) {// 检查当前物品是否在过滤列表中
                        c.sendPacket(PacketCreator.enableActions());
                        return;                      // 过滤该物品时中断拾取
                    }
                }
            }

            chr.pickupItem(ob, petIndex);           // 执行最终物品拾取逻辑
        } catch (NullPointerException | ClassCastException e) {// 捕获无效对象转换（当ob不是MapItem时）或空指针异常
            c.sendPacket(PacketCreator.enableActions()); // 异常发生时恢复玩家操作
        }
    }
}
