package org.gms.net.client.handlers;

import org.gms.client.Client;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStartErrorHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ClientStartErrorHandler.class);
    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        log.warn("客户端 {} 启动游戏出错，原因：{}",c.getRemoteAddress(),p.readString());
    }
}
