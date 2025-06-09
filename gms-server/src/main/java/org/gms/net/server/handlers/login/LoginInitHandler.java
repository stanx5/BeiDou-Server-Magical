package org.gms.net.server.handlers.login;

import org.gms.client.Client;
import org.gms.config.GameConfig;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.server.TimerManager;
import org.gms.util.PacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LoginInitHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginInitHandler.class);
    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        String universal = GameConfig.getServerString("deterred_universal_client");     //获取登录验证标识
        String str = null;
        //这里可以防万能
        try {
            if (!Objects.equals(universal, "")) str = p.readString();       //如果标识不为空则读取封包
        } catch (Exception ignored) {

        } finally {
            if (str != null && !Objects.equals(str, universal)) {    //如果封包不为空则比较标识是否一致，不一致则弹出提示，并且在几秒后踢出客户端
                log.warn("客户端 {} 尝试非法连接服务器，已被拦截",c.getRemoteAddress());
                c.setAccID(-5); //赋值-5，在登录函数里判断为-5则拦截登录
                c.sendPacket(PacketCreator.serverNotice(1,"服务器已限制非法方式进入游戏\r\n请使用服务器指定的方式进入游戏。"));
                TimerManager.getInstance().schedule(closeSession(c), 3 * 1000); //3秒后断开客户端，但是如果客户端在3秒内登录的话还是可以登录成功。有需要可以在登录函数里进行拦截
            }
        }
    }
    public final Runnable closeSession(Client c) {
        return () -> {
            c.disconnect(true, false);
            c.disconnectSession();
            c.closeSession();
        };
    }
}
