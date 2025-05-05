/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

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
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.net.server.Server;
import org.gms.server.maps.MiniDungeonInfo;
import org.gms.util.PacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Flav
 */
public class EnterCashShopHandler extends AbstractPacketHandler {  //定义进入商城的数据包处理器类

    private static final Logger log = LoggerFactory.getLogger(EnterCashShopHandler.class);  //获取日志记录器实例

    @Override
    public void handlePacket(InPacket p, Client c) {  //处理数据包的核心方法
        try {
            Character mc = c.getPlayer();  //获取客户端对应的游戏角色对象

            if (mc.cannotEnterCashShop()) {  //检查角色是否被禁止进入商城
                c.sendPacket(PacketCreator.enableActions());  //发送允许操作的数据包
                return;  //终止处理
            }

            if (mc.getEventInstance() != null) {  //检查角色是否在活动副本中
                c.sendPacket(PacketCreator.serverNotice(5, "参加活动时无法进入商城。"));  //发送系统提示消息
                c.sendPacket(PacketCreator.enableActions());  //恢复角色操作
                return;
            }

            if (MiniDungeonInfo.isDungeonMap(mc.getMapId())) {  //检查是否在迷你副本地图
                c.sendPacket(PacketCreator.serverNotice(5, "在迷你副本中无法进入商城。"));  //发送系统提示
                c.sendPacket(PacketCreator.enableActions());  //恢复角色操作
                return;
            }

            if (mc.getCashShop().isOpened()) {  //检查商城是否已打开
                return;  //避免重复打开
            }
            /* 防止极端情况下点券为负数导致无法进入商城 */
            for (int i = 0; i < 3; i++) {
                int quantity = mc.getCashShop().getCash(i);
                if (quantity < 0) {
                    mc.getCashShop().gainCash(i,-quantity);
                }
            }

            mc.closePlayerInteractions();  //关闭所有玩家交互
            mc.closePartySearchInteractions();  //关闭组队搜索交互

            mc.unregisterChairBuff();  //移除椅子BUFF
            Server.getInstance().getPlayerBuffStorage().addBuffsToStorage(mc.getId(), mc.getAllBuffs());  //存储所有增益效果
            Server.getInstance().getPlayerBuffStorage().addDiseasesToStorage(mc.getId(), mc.getAllDiseases());  //存储所有减益效果
            mc.setAwayFromChannelWorld();  //标记角色离开频道世界
            mc.notifyMapTransferToPartner(-1);  //通知伴侣地图传送
            mc.removeIncomingInvites();  //移除所有收到的邀请
            mc.cancelAllBuffs(true);  //取消所有增益
            mc.cancelAllDebuffs();  //取消所有减益
            mc.cancelBuffExpireTask();  //取消BUFF到期任务
            mc.cancelDiseaseExpireTask();  //取消疾病到期任务
            mc.cancelSkillCooldownTask();  //取消技能冷却任务
            mc.cancelExpirationTask();  //取消过期任务

            mc.forfeitExpirableQuests();  //放弃会过期的任务
            mc.cancelQuestExpirationTask();  //取消任务过期检查

            c.sendPacket(PacketCreator.openCashShop(c, false));  //发送打开商城指令
            c.sendPacket(PacketCreator.showCashInventory(c));  //发送商城库存数据
            c.sendPacket(PacketCreator.showGifts(mc.getCashShop().loadGifts()));  //发送礼物数据
            c.sendPacket(PacketCreator.showWishList(mc, false));  //发送愿望单数据
            c.sendPacket(PacketCreator.showCash(mc));  //发送点券数据

            c.getChannelServer().removePlayer(mc);  //从频道移除玩家
            mc.getMap().removePlayer(mc);  //从地图移除玩家
            mc.getCashShop().open(true);  //标记商城为打开状态
            mc.saveCharToDB();  //保存角色数据到数据库
            log.info("客户端 {} 账号 {} 角色 {} 在频道 {} 进入了商城。",c.getRemoteAddress(),c.getAccountName(),c.getPlayer().getName(),c.getChannelServer().getId());  //记录日志
        } catch (Exception e) {
            e.printStackTrace();  //打印异常堆栈
        }
    }
}
