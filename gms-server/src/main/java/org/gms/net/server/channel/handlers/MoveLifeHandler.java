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
import org.gms.config.GameConfig;
import org.gms.net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gms.server.life.MobSkill;
import org.gms.server.life.MobSkillFactory;
import org.gms.server.life.MobSkillId;
import org.gms.server.life.MobSkillType;
import org.gms.server.life.Monster;
import org.gms.server.life.MonsterInformationProvider;
import org.gms.server.maps.MapObject;
import org.gms.server.maps.MapObjectType;
import org.gms.server.maps.MapleMap;
import org.gms.util.PacketCreator;
import org.gms.exception.EmptyMovementException;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Danny (Leifde)
 * @author ExtremeDevilz
 * @author Ronan (HeavenMS)
 */
public final class MoveLifeHandler extends AbstractMovementPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(MoveLifeHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character player = c.getPlayer();
        MapleMap map = player.getMap();

        // 如果玩家正在切换地图，则不处理怪物移动，避免不同地图间怪物OID冲突
        if (player.isChangingMaps()) {  // 感谢 Lame 注意到在地图切换时发生的怪物移动混乱问题（不同地图上的怪物OID）
            return;
        }

        // 读取怪物对象ID和移动ID
        int objectid = p.readInt();
        short moveid = p.readShort();
        MapObject mmo = map.getMapObject(objectid);// 根据对象ID获取地图对象
        if (mmo == null || mmo.getType() != MapObjectType.MONSTER) {// 如果对象不存在或不是怪物类型，则返回
            return;
        }

        Monster monster = (Monster) mmo;// 将对象转换为怪物对象
        List<Character> banishPlayers = null; // 存储被驱逐的玩家列表

        // 读取怪物移动相关的各种参数
        byte pNibbles = p.readByte();      // 移动标志位
        byte rawActivity = p.readByte();   // 原始活动状态
        int skillId = p.readByte() & 0xff; // 技能ID
        int skillLv = p.readByte() & 0xff; // 技能等级
        short pOption = p.readShort();     // 选项参数
        p.skip(8); // 跳过8个字节的未知数据

        if (rawActivity >= 0) {// 处理原始活动状态值
            rawActivity = (byte) (rawActivity & 0xFF >> 1);
        }

        boolean isAttack = inRangeInclusive(rawActivity, 24, 41);// 判断是否为攻击行为(活动状态在24-41范围内)
        boolean isSkill = inRangeInclusive(rawActivity, 42, 59);// 判断是否为技能使用(活动状态在42-59范围内)

        int useSkillId = 0;    // 实际使用的技能ID
        int useSkillLevel = 0; // 实际使用的技能等级

        if (isSkill) {// 如果是技能使用
            useSkillId = skillId;
            useSkillLevel = skillLv;

            if (monster.hasSkill(useSkillId, useSkillLevel)) {// 检查怪物是否拥有该技能
                // 获取对应的怪物技能类型和技能实例
                MobSkillType mobSkillType = MobSkillType.from(useSkillId).orElseThrow();
                MobSkill toUse = MobSkillFactory.getMobSkillOrThrow(mobSkillType, useSkillLevel);

                if (monster.canUseSkill(toUse, true)) {// 检查怪物是否可以使用该技能
                    int animationTime = MonsterInformationProvider.getInstance().getMobSkillAnimationTime(toUse);// 获取技能动画时间
                    if (animationTime > 0 && toUse.getType() != MobSkillType.BANISH) {// 根据技能类型和动画时间决定如何应用效果
                        toUse.applyDelayedEffect(player, monster, true, animationTime);// 延迟应用技能效果
                    } else {// 立即应用技能效果，特别是驱逐类技能
                        banishPlayers = new LinkedList<>();
                        toUse.applyEffect(player, monster, true, banishPlayers);
                    }
                }
            }
        } else {// 如果不是技能使用，则检查攻击行为
            int castPos = (rawActivity - 24) / 2; // 计算攻击位置
            int atkStatus = monster.canUseAttack(castPos, isSkill); // 检查能否使用攻击
            if (atkStatus < 1) {// 如果不能使用攻击，则重置活动状态和选项
                rawActivity = -1;
                pOption = 0;
            }
        }

        // 判断下次移动是否可能使用技能
        boolean nextMovementCouldBeSkill = !(isSkill || (pNibbles != 0));
        MobSkill nextUse = null;    // 下次可能使用的技能
        int nextSkillId = 0;        // 下次可能使用的技能ID
        int nextSkillLevel = 0;     // 下次可能使用的技能等级
        int mobMp = monster.getMp(); // 怪物当前MP值
        if (nextMovementCouldBeSkill && monster.hasAnySkill()) {// 如果下次移动可能使用技能且怪物拥有技能
            // 随机获取一个技能
            MobSkillId skillToUse = monster.getRandomSkill();
            nextSkillId = skillToUse.type().getId();
            nextSkillLevel = skillToUse.level();
            nextUse = MobSkillFactory.getMobSkillOrThrow(skillToUse.type(), skillToUse.level());

            // 检查技能使用条件：技能存在、可以使用、HP条件满足、MP足够
            if (!(nextUse != null && monster.canUseSkill(nextUse, false) && nextUse.getHP() >= (int) (((float) monster.getHp() / monster.getMaxHp()) * 100) && mobMp >= nextUse.getMpCon())) {
                // 感谢 OishiiKawaiiDesu 注意到怪物试图施放它们不应该拥有的技能
                // 如果条件不满足，则重置技能相关变量
                nextSkillId = 0;
                nextSkillLevel = 0;
                nextUse = null;
            }
        }

        // 读取起始坐标相关数据
        p.readByte();
        p.readInt(); // 未知用途的数据
        short start_x = p.readShort(); // 起始X坐标
        short start_y = p.readShort(); // 起始Y坐标
        Point startPos = new Point(start_x, start_y - 2); // 调整后的起始位置
        Point serverStartPos = new Point(monster.getPosition()); // 服务器端的起始位置

        // 更新怪物的仇恨状态
        Boolean aggro = monster.aggroMoveLifeUpdate(player);
        if (aggro == null) {
            return; // 如果更新失败则返回
        }

        // 向客户端发送怪物移动响应包，包含可能的下次技能信息
        if (nextUse != null) {
            c.sendPacket(PacketCreator.moveMonsterResponse(objectid, moveid, mobMp, aggro, nextSkillId, nextSkillLevel));
        } else {
            c.sendPacket(PacketCreator.moveMonsterResponse(objectid, moveid, mobMp, aggro));
        }


        try {
            int movementDataStart = p.getPosition();// 记录移动数据起始位置
            updatePosition(p, monster, -2);// 更新怪物位置，-2是Y轴偏移量  // 感谢 Doodle & ZERO傑洛 注意到在未应用偏移量的情况下，基于海绵的Boss会移出舞台
            long movementDataLength = p.getPosition() - movementDataStart;// 计算移动数据长度 // updatePosition读取的字节数
            p.seek(movementDataStart);// 重新定位到移动数据起始位置

            // 如果启用了移动调试日志，则记录相关信息
            if (GameConfig.getServerBoolean("use_debug_show_life_move")) {
                log.info("{} 原始活动: {}, 选项: {}, 技能ID: {}, 技能等级: {}, 允许技能: {}, 怪物MP: {}",
                        isSkill ? "技能" : (isAttack ? "攻击" : ""), rawActivity, pOption, useSkillId,
                        useSkillLevel, nextMovementCouldBeSkill, mobMp);
            }

//            log.info("怪物 {}({})(oid:{}) 移动到 {} , {}",monster.getName(), monster.getId(),monster.getObjectId(), monster.getPosition().getX(), monster.getPosition().getY());
            map.broadcastMessage(player, PacketCreator.moveMonster(objectid, nextMovementCouldBeSkill, rawActivity, useSkillId, useSkillLevel, pOption, startPos, p, movementDataLength), serverStartPos);// 广播怪物移动消息给地图上的其他玩家
            map.moveMonster(monster, monster.getPosition());// 更新地图中怪物的位置
        } catch (EmptyMovementException e) {// 捕获空移动异常，不做特殊处理
        }

        // 如果有需要驱逐的玩家，则执行驱逐操作
        if (banishPlayers != null) {
            for (Character chr : banishPlayers) {
                chr.changeMapBanish(monster.getBanish().getMap(), monster.getBanish().getPortal(), monster.getBanish().getMsg());
            }
        }
    }

    // 判断数值是否在指定范围内（包含边界）
    private static boolean inRangeInclusive(Byte pVal, Integer pMin, Integer pMax) {
        return !(pVal < pMin) || (pVal > pMax);
    }
}
