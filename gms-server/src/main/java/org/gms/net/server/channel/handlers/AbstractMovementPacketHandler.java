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

import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gms.server.maps.AnimatedMapObject;
import org.gms.server.movement.AbsoluteLifeMovement;
import org.gms.server.movement.ChangeEquip;
import org.gms.server.movement.JumpDownMovement;
import org.gms.server.movement.LifeMovementFragment;
import org.gms.server.movement.RelativeLifeMovement;
import org.gms.server.movement.TeleportMovement;
import org.gms.exception.EmptyMovementException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMovementPacketHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractMovementPacketHandler.class); // 日志记录器

    /**
     * 解析移动数据包中的移动片段
     * @param p 输入数据包
     * @return 移动片段列表
     * @throws EmptyMovementException 当没有有效移动数据时抛出
     */
    protected List<LifeMovementFragment> parseMovement(InPacket p) throws EmptyMovementException {
        List<LifeMovementFragment> res = new ArrayList<>(); // 存储解析结果的列表
        byte numCommands = p.readByte(); // 读取移动命令数量
        if (numCommands < 1) {
            throw new EmptyMovementException(p); // 无移动命令时抛出异常
        }
        for (byte i = 0; i < numCommands; i++) {// 遍历所有移动命令
            byte command = p.readByte(); // 读取移动命令类型
            switch (command) {
                case 0: // 普通移动
                case 5: // 未知移动类型
                case 17: { // 漂浮移动
                    short xpos = p.readShort(); // 读取X坐标
                    short ypos = p.readShort(); // 读取Y坐标
                    short xwobble = p.readShort(); // 读取X轴抖动
                    short ywobble = p.readShort(); // 读取Y轴抖动
                    short fh = p.readShort(); // 读取脚部高度(foot hold)
                    byte newstate = p.readByte(); // 读取新状态
                    short duration = p.readShort(); // 读取持续时间
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);// 创建绝对移动对象
                    alm.setFh(fh); // 设置脚部高度
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble)); // 设置每秒移动像素
                    res.add(alm); // 添加到结果列表
                    break;
                }
                case 1: // 跳跃
                case 2: // 击退
                case 6: // 二段跳
                case 12: // 未知移动类型
                case 13: // 射击后退
                case 16: // 漂浮
                case 18: // 未知移动类型
                case 19: // 地图弹簧
                case 20: // 战神连击步
                case 22: { // 未知移动类型
                    short xpos = p.readShort(); // 读取X坐标
                    short ypos = p.readShort(); // 读取Y坐标
                    byte newstate = p.readByte(); // 读取新状态
                    short duration = p.readShort(); // 读取持续时间
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xpos, ypos), duration, newstate);// 创建相对移动对象
                    res.add(rlm); // 添加到结果列表
                    break;
                }
                case 3: // 传送消失
                case 4: // 传送出现
                case 7: // 冲锋
                case 8: // 暗杀
                case 9: // 突进
                case 11: // 椅子
                {
//                case 14: {
                    short xpos = p.readShort(); // 读取X坐标
                    short ypos = p.readShort(); // 读取Y坐标
                    short xwobble = p.readShort(); // 读取X轴抖动
                    short ywobble = p.readShort(); // 读取Y轴抖动
                    byte newstate = p.readByte(); // 读取新状态
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);// 创建传送移动对象
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble)); // 设置每秒移动像素
                    res.add(tm); // 添加到结果列表
                    break;
                }
                case 14: // 跳下
                    p.skip(9); // 跳过不需要的数据
                    break;
                case 10: // 更换装备
                    res.add(new ChangeEquip(p.readByte())); // 添加装备变更对象
                    break;
                /*case 11: { // Chair
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setFh(fh);
                    res.add(cm);
                    break;
                }*/
                case 15: { // 跳下移动
                    short xpos = p.readShort(); // 读取X坐标
                    short ypos = p.readShort(); // 读取Y坐标
                    short xwobble = p.readShort(); // 读取X轴抖动
                    short ywobble = p.readShort(); // 读取Y轴抖动
                    short fh = p.readShort(); // 读取脚部高度
                    short ofh = p.readShort(); // 读取原始脚部高度
                    byte newstate = p.readByte(); // 读取新状态
                    short duration = p.readShort(); // 读取持续时间
                    JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);// 创建跳下移动对象
                    jdm.setFh(fh); // 设置脚部高度
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble)); // 设置每秒移动像素
                    jdm.setOriginFh(ofh); // 设置原始脚部高度
                    res.add(jdm); // 添加到结果列表
                    break;
                }
                case 21: { // 战神特殊攻击移动
                    /*byte newstate = lea.readByte();
                     short unk = lea.readShort();
                     AranMovement am = new AranMovement(command, null, unk, newstate);
                     res.add(am);*/
                    p.skip(3); // 跳过不需要的数据
                    break;
                }
                default:
                    log.warn("Unhandled case: {}", command); // 记录未处理的移动类型
                    throw new EmptyMovementException(p); // 抛出异常
            }
        }

        if (res.isEmpty()) {
            throw new EmptyMovementException(p); // 如果没有解析出任何移动则抛出异常
        }
        return res; // 返回解析结果
    }

    /**
     * 更新角色位置信息
     * @param p 输入数据包
     * @param target 要更新的动画地图对象
     * @param yOffset Y轴偏移量
     * @throws EmptyMovementException 当没有有效移动数据时抛出
     */
    protected void updatePosition(InPacket p, AnimatedMapObject target, int yOffset) throws EmptyMovementException {

        byte numCommands = p.readByte(); // 读取移动命令数量
        if (numCommands < 1) {
            throw new EmptyMovementException(p); // 无移动命令时抛出异常
        }
        for (byte i = 0; i < numCommands; i++) {// 遍历所有移动命令
            byte command = p.readByte(); // 读取移动命令类型
            switch (command) {
                case 0: // 普通移动
                case 5: // 未知移动类型
                case 17: { // 漂浮移动
                    //Absolute movement - only this is important for the server, other movement can be passed to the client
                    short xpos = p.readShort(); // 读取X坐标
                    short ypos = p.readShort(); // 读取Y坐标
                    target.setPosition(new Point(xpos, ypos + yOffset)); // 更新位置(应用Y偏移)
                    p.skip(6); // 跳过X抖动、Y抖动和脚部高度
                    byte newstate = p.readByte(); // 读取新状态
                    target.setStance(newstate); // 更新角色姿态
                    p.readShort(); // 跳过持续时间
                    break;
                }
                case 1: // 跳跃
                case 2: // 击退
                case 6: // 二段跳
                case 12: // 未知移动类型
                case 13: // 射击后退
                case 16: // 漂浮
                case 18: // 未知移动类型
                case 19: // 地图弹簧
                case 20: // 战神连击步
                case 22: { // 未知移动类型
                    //Relative movement - server only cares about stance
                    p.skip(4); // 跳过X和Y坐标
                    byte newstate = p.readByte(); // 读取新状态
                    target.setStance(newstate); // 更新角色姿态
                    p.readShort(); // 跳过持续时间
                    break;
                }
                case 3: // 传送消失
                case 4: // 传送出现
                case 7: // 冲锋
                case 8: // 暗杀
                case 9: // 突进
                case 11: // 椅子
                {
//                case 14: {
                    //Teleport movement - same as above
                    p.skip(8); // 跳过X、Y坐标和抖动数据
                    byte newstate = p.readByte(); // 读取新状态
                    target.setStance(newstate); // 更新角色姿态
                    break;
                }
                case 14: // 跳下
                    p.skip(9); // 跳过不需要的数据
                    break;
                case 10: // 更换装备
                    //ignored server-side
                    p.readByte(); // 跳过装备变更数据
                    break;
                /*case 11: { // Chair
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setFh(fh);
                    res.add(cm);
                    break;
                }*/
                case 15: { // 跳下移动
                    //Jump down movement - stance only
                    p.skip(12); // 跳过坐标、抖动和脚部高度数据
                    byte newstate = p.readByte(); // 读取新状态
                    target.setStance(newstate); // 更新角色姿态
                    p.readShort(); // 跳过持续时间
                    break;
                }
                case 21: { // 战神特殊攻击移动
                    /*byte newstate = lea.readByte();
                     short unk = lea.readShort();
                     AranMovement am = new AranMovement(command, null, unk, newstate);
                     res.add(am);*/
                    p.skip(3); // 跳过不需要的数据
                    break;
                }
                default:
                    log.warn("Unhandled Case: {}", command); // 记录未处理的移动类型
                    throw new EmptyMovementException(p); // 抛出异常
            }
        }
    }
}
