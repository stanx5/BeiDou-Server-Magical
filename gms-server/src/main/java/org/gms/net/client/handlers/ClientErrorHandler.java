package org.gms.net.client.handlers;

import org.gms.client.Client;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientErrorHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ClientErrorHandler.class);
    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(InPacket p, Client c) {    //未验证是否有效
        log.warn("客户端 {} 出现异常错误，原因：{}",c.getRemoteAddress(),p.readString());
    }
}
