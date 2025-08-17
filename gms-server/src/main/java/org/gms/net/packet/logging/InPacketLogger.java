package org.gms.net.packet.logging;

import org.gms.config.GameConfig;
import org.gms.constants.net.OpcodeConstants;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.gms.net.packet.InPacket;
import org.gms.net.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gms.util.HexTool;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

@Sharable
public class InPacketLogger extends ChannelInboundHandlerAdapter implements PacketLogger {
    private static final Logger log = LoggerFactory.getLogger(InPacketLogger.class);
    private static final int LOG_CONTENT_THRESHOLD = 3_000;

    // 创建屏蔽列表（静态集合）
    private static final Set<String> BLOCKED_OPCODES = new HashSet<>();
    static {
        // 添加要屏蔽的包头，例如"6C"
//        BLOCKED_OPCODES.add("6C");
        // 可以添加更多需要屏蔽的包头
        // BLOCKED_OPCODES.add("1A");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof InPacket packet) {
            String clientIp = getClientIp(ctx);
            log(packet, clientIp);
        }
        ctx.fireChannelRead(msg);
    }

    private String getClientIp(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress remoteAddress) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "未知IP";
    }

    public void log(Packet packet, String clientIp) {
        final byte[] content = packet.getBytes();
        final int packetLength = content.length;

        if (packetLength <= LOG_CONTENT_THRESHOLD) {
            final short opcode = LoggingUtil.readFirstShort(content);
            final String opcodeHex = Integer.toHexString(opcode).toUpperCase();

            // 检查是否在屏蔽列表中
            if (BLOCKED_OPCODES.contains(opcodeHex)) {
                return; // 直接跳过，不记录日志
            }

            final String opcodeName = getRecvOpcodeName(opcode);
            final boolean isUnknownPacket = opcodeName == null;
            final boolean debugMode = GameConfig.getServerBoolean("use_debug_show_packet");

            // 决定是否记录完整封包
            final boolean shouldLogFull = debugMode || isUnknownPacket;

            if (shouldLogFull) {
                log.info("接收客户端 {} {} [0x{}] ({}字节) HEX: {} TEXT: {}",
                        clientIp,
                        isUnknownPacket ? "未知" : opcodeName,
                        opcodeHex,
                        packetLength,
                        HexTool.toHexString(content),
                        HexTool.toStringFromCharset(content));
            }
        } else {
            // 长封包处理（仍然显示IP和头部信息）
            log.info("接收客户端 {} 封包过长(>{}字节)，显示头部: {}...",
                    clientIp, LOG_CONTENT_THRESHOLD,
                    HexTool.toHexString(new byte[]{content[0], content[1]}));
        }
    }

    @Override
    public void log(Packet packet) {
        log(packet, "IP不可用");
    }

    private String getRecvOpcodeName(short opcode) {
        return OpcodeConstants.recvOpcodeNames.get((int) opcode);
    }

    // 添加方法用于动态管理屏蔽列表
    public static void addBlockedOpcode(String opcodeHex) {
        BLOCKED_OPCODES.add(opcodeHex.toUpperCase());
    }

    public static void removeBlockedOpcode(String opcodeHex) {
        BLOCKED_OPCODES.remove(opcodeHex.toUpperCase());
    }
}