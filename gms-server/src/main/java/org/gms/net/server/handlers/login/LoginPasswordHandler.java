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
package org.gms.net.server.handlers.login;

import org.gms.client.Client;
import org.gms.client.DefaultDates;
import org.gms.config.GameConfig;
import org.gms.net.PacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.net.server.Server;
import org.gms.net.server.coordinator.session.Hwid;
import org.gms.util.BCrypt;
import org.gms.util.DatabaseConnection;
import org.gms.util.HexTool;
import org.gms.util.PacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class LoginPasswordHandler implements PacketHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginPasswordHandler.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");

    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        String remoteHost = c.getRemoteAddress();
        if (remoteHost.contentEquals("null")) {
            c.sendPacket(PacketCreator.getLoginFailed(14));          // thanks Alchemist for noting remoteHost could be null
            return;
        } else if (c.getAccID() == -5) {
            c.sendPacket(PacketCreator.serverNotice(1,"服务器已限制非法方式进入游戏\r\n请使用服务器指定的方式进入游戏。"));
            return;
        }

        String login = p.readString();
        String pwd = p.readString();
        c.setAccountName(login);

        p.skip(6);   // localhost masked the initial part with zeroes...
        byte[] hwidNibbles = p.readBytes(4);
        Hwid hwid = new Hwid(HexTool.toCompactHexString(hwidNibbles));
        int loginok = c.login(login, pwd, hwid);

        if (GameConfig.getServerBoolean("automatic_register") && loginok == 5) {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (name, password, birthday, tempban) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) { //Jayd: Added birthday, tempban
                ps.setString(1, login);
                ps.setString(2, GameConfig.getServerBoolean("bcrypt_migration") ? BCrypt.hashpw(pwd, BCrypt.gensalt(12)) : BCrypt.hashpwSHA512(pwd));
                ps.setDate(3, Date.valueOf(DefaultDates.getBirthday()));
                ps.setTimestamp(4, Timestamp.valueOf(DefaultDates.getTempban()));
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    c.setAccID(rs.getInt(1));
                }
            } catch (SQLException | NoSuchAlgorithmException e) {
                c.setAccID(-1);
                e.printStackTrace();
            } finally {
                loginok = c.login(login, pwd, hwid);
            }
        }

        if (GameConfig.getServerBoolean("bcrypt_migration") && (loginok <= -10)) { // -10 means migration to bcrypt, -23 means TOS wasn't accepted
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE accounts SET password = ? WHERE name = ?;")) {
                ps.setString(1, BCrypt.hashpw(pwd, BCrypt.gensalt(12)));
                ps.setString(2, login);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                loginok = (loginok == -10) ? 0 : 23;    //发送协议确认
            }
        }

        if (c.hasBannedIP() || c.hasBannedMac() || c.hasBannedHWID()) {
            log.warn("客户端 {} 尝试登录账号 {}，但是IP / 设备码已被封禁，无法登录{}{}{}。",
                    c.getRemoteAddress(),
                    login,
                    (c.hasBannedIP() ? "，IP：[" + c.getRemoteAddress() + "] 被封禁" : ""),
                    (c.hasBannedMac() ? "，Mac：[" + c.getMacs() + "] 被封禁" : ""),
                    (c.hasBannedHWID() ? "，HWID：[" + hwid + "]，被封禁" : "")
            );
            c.sendPacket(PacketCreator.serverNotice(1,"您的设备已被禁止进入游戏。\r\n如有疑问，请联系GM处理。"));
            c.sendPacket(PacketCreator.getLoginFailed(1));//启用客户端操作
            return;
        }

        Calendar tempban = c.getTempBanCalendarFromDB();
        String banreason = c.getBanreasonFromDB();
        if (tempban != null) {
            if (tempban.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
                String tmpbanstr = sdf.format(tempban.getTime());
                c.sendPacket(PacketCreator.serverNotice(1, "您的账号已被临时封停至 " + tmpbanstr + (!banreason.isEmpty() ? "\r\n\r\n【原因】\r\n" : "") + banreason));   //发送临时封禁时间和原因
                c.sendPacket(PacketCreator.getLoginFailed(1));          //通知客户端恢复操作
                log.warn("客户端 {} 尝试登录账号 {} ，但是被临时封停至 {} ，原因：{}",c.getRemoteAddress(),login,tmpbanstr,banreason);
                return;
            }
        }
        int loginCountMax = GameConfig.getServerInt("login_client_limit");
        if (loginCountMax > 0) {
            int loginCount = Math.max(c.getActiveRecordCount(c.getRemoteAddress()),c.getActiveRecordCount(hwid.hwid()));
            if (loginCount >= loginCountMax || loginCount >= loginCountMax) {
                c.sendPacket(PacketCreator.serverNotice(1,"您的设备当前已登录账号数已超过服务端允许，无法继续登录账号。"));
                c.sendPacket(PacketCreator.getLoginFailed(1));          //通知客户端恢复操作
                log.warn("客户端 {} 尝试登录账号 {} ，已登录数量 {} ，最大允许登录数量 {} ，已限制登录。",c.getRemoteAddress(),login,loginCount,loginCountMax);
                return;
            }
        }

        loginCountMax = GameConfig.getServerInt("login_client_limit_today");
        if (loginCountMax > 0) {
            int loginCount = Math.max(c.getTodayLoginCount(c.getRemoteAddress()),c.getTodayLoginCount(hwid.hwid()));
            if (loginCount >= loginCountMax || loginCount >= loginCountMax) {
                c.sendPacket(PacketCreator.serverNotice(1,"您的设备今天累计登录账号数已超过服务端允许，无法继续登录账号。"));
                c.sendPacket(PacketCreator.getLoginFailed(1));          //通知客户端恢复操作
                log.warn("客户端 {} 尝试登录账号 {} ，已乐基登录数量 {} ，最大允许登录数量 {} ，已限制登录。",c.getRemoteAddress(),login,loginCount,loginCountMax);
                return;
            }
        }

        if (loginok == 3) {
            c.sendPacket(PacketCreator.getPermBan(c.getGReason()));//crashes but idc :D
            log.warn("客户端 {} 尝试登录账号 {} ，但是账号已被封禁",c.getRemoteAddress(),login);
            return;
        } else if (loginok != 0) {
            c.sendPacket(PacketCreator.getLoginFailed(loginok));    //通知客户端密码错误
            log.warn("客户端 {} 尝试登录账号 {} ，但是登录失败：{}",c.getRemoteAddress(),login,loginok);
            return;
        }
        if (c.finishLogin() == 0) {
            c.checkChar(c.getAccID());
            login(c);
            log.info("客户端 {} 成功登录账号 {} 。",c.getRemoteAddress(),login);
        } else {
            c.sendPacket(PacketCreator.getLoginFailed(7));
        }
    }

    private static void login(Client c) {
        c.sendPacket(PacketCreator.getAuthSuccess(c));//why the fk did I do c.getAccountName()?
        Server.getInstance().registerLoginState(c);
    }
}
