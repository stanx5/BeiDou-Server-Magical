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

/* @author RonanLana */
const BossLog = "每日BOSS-帕普拉图斯";
const MaxLog = 2;

function getBossLog(Player) {
    count = Player.getAbstractPlayerInteraction().getCharacterExtendValue(BossLog,true) || 0;
    return Number(count);
}

function setBossLog(Player,count) {
    count = (String) (count || 0);
    return Player.getAbstractPlayerInteraction().saveOrUpdateCharacterExtendValue(BossLog,count,true);
}

function enter(pi) {
    let player = pi.getPlayer();
    if (!((pi.isQuestStarted(6361) && pi.haveItem(4031870, 1)) || (pi.isQuestCompleted(6361) && !pi.isQuestCompleted(6363)))) {
        var em = pi.getEventManager("PapulatusBattle");

        if (pi.getParty() == null) {
            pi.playerMessage(5, "你当前未加入队伍，请创建队伍后再挑战BOSS。");
            return false;
        } else if (!pi.isLeader()) {
            pi.playerMessage(5, "必须由队长进入传送门才能开始战斗。");
            return false;
        } else  {
            var eli = em.getEligibleParty(pi.getParty());
            if (eli.size() > 0) {
                let noEntryPlayer = [];
                eli.forEach(p => {
                    p = p.getPlayer();
                    let count = getBossLog(p);
                    if (count >= MaxLog) {
                        p.dropMessage(5, "你今天的挑战次数已用完，无法进入。");
                        noEntryPlayer.push(p);
                    }
                })
                if (noEntryPlayer.length > 0) {
                    pi.npcTalk(2041024,`队伍里拢共有 ${noEntryPlayer.length} 名队员入场次数已耗尽：\r\n\r\n${noEntryPlayer.join("\r\n")}`);
                } else if (!em.startInstance(pi.getParty(), pi.getPlayer().getMap(), 1)) {
                    pi.playerMessage(5, "里面的战斗已经开始了，暂时无法进入。");
                    return false;
                }  else {
                    eli.forEach(p => {
                        p = p.getPlayer();
                        count = getBossLog(p) + 1;
                        setBossLog(p,count);
                        p.dropMessage(6,`${BossLog}  挑战次数：${count} / ${MaxLog}`);
                    });
                }
            } else {  //this should never appear
                pi.playerMessage(5, "你暂时无法开始这场战斗，可能是因为队伍人数不符合要求、部分队员未满足挑战条件或不在当前地图。");
                return false;
            }

            pi.playPortalSound();
            return true;
        }
    } else {
        pi.playPortalSound();
        pi.warp(922020300, 0);
        return true;
    }
}