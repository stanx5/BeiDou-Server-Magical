/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
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

/*
   @Author: Arthur L - Refactored command content into modules
*/
package org.gms.client.command.commands.gm3;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.client.command.Command;
import org.gms.net.server.Server;
import org.gms.server.TimerManager;
import org.gms.util.I18nUtil;
import org.gms.util.PacketCreator;

public class BanCommand extends Command {
    private static final int DISCONNECT_DELAY_MS = 5000;

    {
        setDescription(I18nUtil.getMessage("BanCommand.message1"));
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        // 参数验证
        if (params.length < 2) {
            player.yellowMessage(I18nUtil.getMessage("BanCommand.message2"));
            return;
        }

        String targetName = params[0];
        String reason = joinStringFrom(params, 1);
        Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);

        boolean success;

        if (target != null) {
            // 在线玩家封禁流程
            String readableTargetName = Character.makeMapleReadable(target.getName());
            String ip = target.getClient().getRemoteAddress();

            // 尝试封禁IP并通知GM结果
            if (!target.getClient().banIP()) {
                c.getPlayer().message(I18nUtil.getMessage("BanCommand.message3"));
                c.getPlayer().message(I18nUtil.getMessage("BanCommand.message4", target.getName(), ip));
            }
            // 向目标玩家发送封禁通知
            target.yellowMessage(I18nUtil.getMessage("BanCommand.message6", c.getPlayer().getName()) + "，" + I18nUtil.getMessage("BanCommand.message7", reason));
            target.dropMessage(1, "您已被GM封禁，5秒后将断开连接\r\n\r\n【原因】\r\n" + reason);
            String banReason = I18nUtil.getMessage("BanCommand.message5", c.getPlayer().getName(),readableTargetName, reason, ip, target.getClient().banMacs());
            target.ban(banReason);// 执行封禁操作
            // 延迟断开连接
            TimerManager.getInstance().schedule(() -> target.getClient().disconnect(false, false), DISCONNECT_DELAY_MS);
            // 广播封禁消息
            Server.getInstance().broadcastMessage(c.getWorld(),PacketCreator.serverNotice(6, I18nUtil.getMessage("BanCommand.message8", targetName)));

            success = true;
        } else {
            // 离线玩家封禁流程
            success = Character.ban(targetName, reason, false);
            if (success) {
                // 广播封禁消息
                Server.getInstance().broadcastMessage(c.getWorld(),PacketCreator.serverNotice(6, I18nUtil.getMessage("BanCommand.message8", targetName)));
            }
        }
        c.sendPacket(PacketCreator.getGMEffect(success ? 4 : 6, (byte) (success ? 0 : 1)));// 发送操作结果效果
    }
}