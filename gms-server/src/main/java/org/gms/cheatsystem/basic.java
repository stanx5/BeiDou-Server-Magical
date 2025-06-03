package org.gms.cheatsystem;

import org.gms.net.server.Server;

/**
 * 基础功能方法
 */

public class basic {
    /**
     * 当前服务器时间
     * @return
     */
    protected static long currentServerTime() {
        return Server.getInstance().getCurrentTime();
    }
}
