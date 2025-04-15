package org.gms.net.netty;

import org.gms.client.Client; // 导入客户端类
import io.netty.channel.socket.SocketChannel; // 导入Netty的Socket通道类
import org.gms.net.PacketProcessor; // 导入数据包处理器类
import org.gms.net.server.coordinator.session.SessionCoordinator; // 导入会话协调器类
import org.gms.util.I18nUtil; // 导入国际化工具类
import org.gms.util.RateLimitUtil; // 导入速率限制工具类
import org.slf4j.Logger; // 导入SLF4J日志接口
import org.slf4j.LoggerFactory; // 导入SLF4J日志工厂类

/**
 * 登录服务器通道初始化器，用于处理新客户端连接的初始化流程
 * @extends ServerChannelInitializer 继承自服务器通道初始化基类
 * @author 开发者名称
 * @version 1.0
 */
public class LoginServerInitializer extends ServerChannelInitializer {
    private static final Logger log = LoggerFactory.getLogger(LoginServerInitializer.class);// 声明日志记录器实例

    /**
     * 初始化客户端通道的主方法
     * @param socketChannel 客户端Socket通道对象
     */
    @Override
    public void initChannel(SocketChannel socketChannel) {
        final String clientIp = socketChannel.remoteAddress().getHostString();// 获取客户端IP地址字符串
        log.info(I18nUtil.getLogMessage("LoginServerInitializer.initChannel.info1"), clientIp);// 记录客户端连接日志（使用国际化消息模板）
        PacketProcessor packetProcessor = PacketProcessor.getLoginServerProcessor();// 获取登录专用的数据包处理器实例
        final long clientSessionId = sessionId.getAndIncrement();// 生成原子递增的客户端会话ID
        final String remoteAddress = getRemoteAddress(socketChannel);// 获取格式化后的客户端地址（IP:Port）
        if (!RateLimitUtil.getInstance().check(remoteAddress)) {// 执行速率限制检查
            log.warn(I18nUtil.getLogMessage("LoginServerInitializer.initChannel.warn1"), remoteAddress);// 记录速率超限警告日志
            socketChannel.close();// 关闭不符合速率限制的连接
        }
        final Client client = Client.createLoginClient(// 创建登录客户端实例（工厂方法）
                clientSessionId,          // 传入会话ID
                remoteAddress,            // 客户端地址
                packetProcessor,          // 数据包处理器
                LoginServer.WORLD_ID,    // 游戏世界ID常量
                LoginServer.CHANNEL_ID    // 游戏频道ID常量
        );
        
        if (!SessionCoordinator.getInstance().canStartLoginSession(client)) {// 检查会话协调器是否允许新会话
            socketChannel.close();// 关闭未授权的连接
            return;// 终止初始化流程
        }

        initPipeline(socketChannel, client);// 初始化网络通信处理器链
    }
}
