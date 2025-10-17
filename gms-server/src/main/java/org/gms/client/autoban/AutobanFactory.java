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

package org.gms.client.autoban;

import org.gms.client.Character;
import org.gms.config.GameConfig;
import org.gms.net.server.Server;
import org.gms.util.PacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author kevintjuh93
 */
public enum AutobanFactory {
    MOB_COUNT(1,SECONDS.toMillis(10),"篡改目标数"),
    GENERAL("通用检测"),
    FIX_DAMAGE(3,SECONDS.toMillis(10),"固定伤害"),
    DAMAGE_HACK(15, MINUTES.toMillis(1), "篡改伤害"),
    DISTANCE_HACK(10, MINUTES.toMillis(2), "篡改距离"),
    PORTAL_DISTANCE(5, SECONDS.toMillis(30), "传送门距离"),
    PACKET_EDIT("封包编辑"),
    ACC_HACK("账号黑客"),
    CREATION_GENERATOR("创建生成器"),
    HIGH_HP_HEALING("高血量恢复"),
    FAST_HP_HEALING(10, SECONDS.toMillis(2), "快速回血"),
    FAST_MP_HEALING(10, SECONDS.toMillis(2), "快速回蓝"),
    GACHA_EXP("扭蛋经验"),
    TUBI(20, SECONDS.toMillis(15), "图比"),
    SHORT_ITEM_VAC("短距离吸物"),
    ITEM_VAC("吸物"),
    FAST_ITEM_PICKUP(5, SECONDS.toMillis(30), "快速物品拾取"),
    FAST_ATTACK(10, MILLISECONDS.toMillis(200), "快速攻击"),
    MISS_HACK(7,SECONDS.toMillis(5), "MISS无敌"),
    MPCON(25, SECONDS.toMillis(30), "MP消耗");

    private static final Logger log = LoggerFactory.getLogger(AutobanFactory.class);
    private static final Set<Integer> ignoredChrIds = new HashSet<>();

    private final int points;
    private final long expiretime;
    private final String name;

    AutobanFactory() {
        this(1);
    }

    AutobanFactory(int points) {
        this(points, -1, null);
    }

    AutobanFactory(String name) {
        this(5, SECONDS.toMillis(30), name);
    }

    AutobanFactory(int points, long expire) {
        this(points, expire, null);
    }

    AutobanFactory(int points, String name) {
        this(points, SECONDS.toMillis(30), name);
    }

    AutobanFactory(int points, long expire, String name) {
        this.points = points;
        this.expiretime = expire;
        this.name = (name != null) ? name : name();
    }

    public int getMaximum() {
        return points;
    }

    public long getExpire() {
        return expiretime;
    }

    public String getName() {
        return name != null ? name : this.name();
    }

    /**
     * 添加违规点数
     * 当惩罚点数达到阈值时清零惩罚点数并增加封号点数
     * 当封号点数达到阈值时执行封号操作
     *
     * @param ban 违规类型工厂
     * @param reason 违规原因描述
     * @return 1-已封号, 0-需要惩罚, -1-未触发任何操作
     */
    public int addPoint(AutobanManager ban, String reason) {
        return ban.addPoint(this, reason);
    }

    public void alert(Character chr, String reason) {
        if (chr.getAutoBanManager().useAutoBan()) {
            if (isIgnored(chr.getId())) {
                return;
            }
        }
        if (chr.getAutoBanManager().useAutoBanLog()) {
//            Server.getInstance().broadcastGMMessage(chr.getWorld(), PacketCreator.sendYellowTip("[异常提示] 玩家 " + chr.getName() + " 在地图 " + chr.getMap().getMapName() + "(" + chr.getMapId() + ") 因触发 " + this.getName() + " - " + reason));
            log.warn("[异常提示] 玩家 {} 在地图 {}({}) 因触发 {} - {}", chr.getName(), chr.getMap().getMapName(),chr.getMapId(), this.getName(), reason);
        }
    }


    /**
     * 自动封禁玩家
     * 
     * @param chr 要被封禁的玩家角色对象
     * @param value 封禁的原因或相关信息
     */
    public void autoban(Character chr, String value) {
        if (chr.getAutoBanManager().useAutoBan()) {
            chr.autoBan("因 [" + this.getName() + "] 被自动封禁: " + value);
            //chr.sendPolice("You will be disconnected for (" + this.name() + ": " + value + ")");
        }
    }

    /**
     * 切换角色ID的忽略状态。
     * 被忽略的角色将不会触发GM警报。
     *
     * @param chrId 角色ID
     * @return 新的状态。如果chrId现在被忽略则返回true，否则返回false。
     */
    public static boolean toggleIgnored(int chrId) {
        if (ignoredChrIds.contains(chrId)) {
            ignoredChrIds.remove(chrId);
            return false;
        } else {
            ignoredChrIds.add(chrId);
            return true;
        }
    }

    /**
     * 检查指定角色ID是否被忽略
     *
     * @param chrId 角色ID
     * @return 如果角色被忽略则返回true，否则返回false
     */
    private static boolean isIgnored(int chrId) {
        return ignoredChrIds.contains(chrId);
    }

    /**
     * 获取所有被忽略的角色ID集合
     *
     * @return 包含所有被忽略角色ID的集合
     */
    public static Collection<Integer> getIgnoredChrIds() {
        return ignoredChrIds;
    }
}
