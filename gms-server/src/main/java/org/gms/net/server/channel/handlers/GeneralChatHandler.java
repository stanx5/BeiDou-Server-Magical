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
import org.gms.client.autoban.AutobanFactory;
import org.gms.client.command.CommandsExecutor;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gms.server.ChatLogger;
import org.gms.util.PacketCreator;

public final class GeneralChatHandler extends AbstractPacketHandler {  //定义全局聊天处理器（final类不可继承）
    private static final Logger log = LoggerFactory.getLogger(GeneralChatHandler.class);  //获取当前类的日志记录器

    @Override
    public void handlePacket(InPacket p, Client c) {  //处理聊天数据包的核心方法
        String s = p.readString();  //从数据包读取聊天内容
        Character chr = c.getPlayer();  //获取发送聊天的玩家角色对象
        if (chr.getAutoBanManager().getLastSpam(7) + 200 > currentServerTime()) {// 检查聊天冷却时间（200ms内重复发言会被拦截）
            c.sendPacket(PacketCreator.enableActions());  //发送允许操作指令
            return;
        }
        if (s.length() > Byte.MAX_VALUE && !chr.isGM()) {// 检查非GM玩家是否发送超长消息（超过127字节）
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), "玩家" + c.getPlayer().getName() + " 尝试在全局聊天中篡改封包。");  //触发封包篡改警告
            log.warn("玩家 {} 尝试发送长度为 {} 的文本", c.getPlayer().getName(), s.length()); //记录警告日志
            c.disconnect(true, false);  //强制断开连接
            return;
        }
        char heading = s.charAt(0);  //获取消息首字符
        if (CommandsExecutor.isCommand(c, s)) {  //判断是否为命令（以'/'开头）
            CommandsExecutor.getInstance().handle(c, s);  //交给命令处理器执行
        } else if (heading != '/') {  //如果不是命令且不以'/'开头
            int show = p.readByte();  //读取消息显示模式
            if (chr.getMap().isMuted() && !chr.isGM()) {// 检查非GM玩家是否在禁言地图
                chr.dropMessage(5, "当前地图已被禁言，请稍后再试");
                return;
            }

            if (!chr.isHidden()) {  //如果玩家不是隐身状态
                chr.getMap().broadcastMessage(PacketCreator.getChatText(chr.getId(), s, chr.getWhiteChat(), show));// 向全地图广播普通聊天消息
                ChatLogger.log(c, "对所有人", s);  //记录普通聊天日志
            } else {  //GM隐身状态
                chr.getMap().broadcastGMMessage(PacketCreator.getChatText(chr.getId(), s, chr.getWhiteChat(), show));// 只向GM广播聊天消息
                ChatLogger.log(c, "GM 隐身消息", s);  //记录GM聊天日志
            }

            chr.getAutoBanManager().spam(7);  //更新聊天时间戳（7表示聊天行为类型）
        }
    }
}