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
package org.gms.client.command.commands.gm0;

import org.gms.client.BuffStat;
import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.client.command.Command;
import org.gms.config.GameConfig;
import org.gms.util.I18nUtil;

public class RatesCommand extends Command {
    {
        setDescription(I18nUtil.getMessage("RatesCommand.message1"));
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        player.updateCouponRates();//刷新倍率

        // 提前计算常用值和获取配置
        boolean useQuestRate = GameConfig.getServerBoolean("use_quest_rate");
        boolean allowExpCard = GameConfig.getServerBoolean("allow_exp_multiplier_card");
        boolean allowDropCard = GameConfig.getServerBoolean("allow_drop_multiplier_card");

        // 使用StringBuilder替代字符串拼接
        StringBuilder showMsg = new StringBuilder(300); // 预估初始容量
        showMsg.append("#e").append(I18nUtil.getMessage("RatesCommand.message2")).append("#n\r\n\r\n");

        // 计算经验倍率
        float exp_buff = player.getBuffedValue(BuffStat.EXP_BUFF) != null ? 2.0f : 1.0f;
        String noviceMsg = player.hasNoviceExpRate() ? I18nUtil.getMessage("ShowRatesCommand.message7") : "";
        float expRate = player.getExpRate() * exp_buff * player.getFamilyExp();
        showMsg.append(I18nUtil.getMessage("ShowRatesCommand.message6"))
                .append("#e#b").append(expRate).append("x#k#n ").append(noviceMsg).append("\r\n");

        // 怪物经验倍率（条件显示）
        float mobExpRate = player.getMobExpRate();
        if (mobExpRate > 1) {
            showMsg.append(I18nUtil.getMessage("RatesCommand.message4"))
                    .append("#e#b").append(Math.round(mobExpRate * 100f) / 100f).append("x#k#n\r\n");
        }

        // 直接追加其他倍率信息
        showMsg.append(I18nUtil.getMessage("ShowRatesCommand.message12"))
                .append("#e#b").append(player.getMesoRate()).append("x#k#n\r\n")
                .append(I18nUtil.getMessage("ShowRatesCommand.message17"))
                .append("#e#b").append(player.getDropRate() * player.getFamilyDrop()).append("x#k#n\r\n")
                .append(I18nUtil.getMessage("ShowRatesCommand.message22"))
                .append("#e#b").append(player.getBossDropRate()).append("x#k#n\r\n");

        // 任务倍率（条件显示）
        if (useQuestRate) {
            showMsg.append(I18nUtil.getMessage("RatesCommand.message3"))
                    .append("#e#b").append(c.getWorldServer().getQuestRate()).append("x#k#n\r\n");
        }

        // 倍率卡状态（条件显示）
        if (!allowExpCard) {
            showMsg.append("经验倍率卡：#e#r已禁用#k#n\r\n");
        }
        if (!allowDropCard) {
            showMsg.append("掉落倍率卡：#e#r已禁用#k#n\r\n");
        }

        player.showHint(showMsg.toString(), 150);
    }
}
