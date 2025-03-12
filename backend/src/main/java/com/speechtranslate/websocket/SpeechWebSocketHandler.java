package com.speechtranslate.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speechtranslate.service.SpeechToTextService;

@Component
public class SpeechWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpeechWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpeechToTextService speechToTextService;
    
    public SpeechWebSocketHandler(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }
    
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒心跳间隔
    private static final long HEARTBEAT_TIMEOUT = 60000; // 60秒超时时间
    private static final int MAX_RETRY_ATTEMPTS = 3; // 最大重试次数
    private final Map<String, Integer> retryAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHeartbeatTimes = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接已建立: {}", session.getId());
        sessions.put(session.getId(), session);
        lastHeartbeatTimes.put(session.getId(), System.currentTimeMillis());
        
        // 启动心跳检测线程
        startHeartbeatCheck(session);
    }
    
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer buffer = message.getPayload();
        byte[] audioData = new byte[buffer.remaining()];
        buffer.get(audioData);
        
        logger.debug("收到二进制音频数据: {} 字节, 会话ID: {}", audioData.length, session.getId());
        
        // 检查音频数据是否为空或太小
        if (audioData.length == 0) {
            logger.warn("收到空的音频数据，会话ID: {}", session.getId());
            return;
        } else if (audioData.length < 100) {
            logger.warn("音频数据太小 ({} 字节)，可能不足以识别，会话ID: {}", audioData.length, session.getId());
        }
        
        // 处理音频数据并获取转写结果
        try {
            // 记录开始处理时间，用于性能分析
            long startTime = System.currentTimeMillis();
            
            String transcription = speechToTextService.transcribeAudio(audioData, session.getId());
            
            // 计算处理时间
            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("音频处理耗时: {}ms, 会话ID: {}", processingTime, session.getId());
            
            // 将转写结果发送回客户端
            if (transcription != null && !transcription.isEmpty()) {
                Map<String, String> response = Map.of("transcription", transcription);
                String jsonResponse = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(jsonResponse));
                logger.info("已发送转写结果: {}", transcription);
            } else {
                logger.warn("转写结果为空，会话ID: {}", session.getId());
                Map<String, String> response = Map.of(
                    "transcription", "",
                    "message", "未能识别语音内容，请检查麦克风并重试"
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        } catch (Exception e) {
            logger.error("处理音频数据时出错: {}", e.getMessage(), e);
            Map<String, String> errorResponse = Map.of("error", "处理音频时出错: " + e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            // 更新心跳时间戳
            lastHeartbeatTimes.put(session.getId(), System.currentTimeMillis());
            session.sendMessage(new TextMessage("pong"));
            logger.debug("收到客户端ping，已回复pong");
        } else if ("pong".equals(payload)) {
            // 客户端响应了我们的ping
            lastHeartbeatTimes.put(session.getId(), System.currentTimeMillis());
            logger.debug("收到客户端pong响应");
        } else {
            logger.info("收到文本消息: {}", payload);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: {}", exception.getMessage());
        Integer attempts = retryAttempts.getOrDefault(session.getId(), 0);
        if (attempts < MAX_RETRY_ATTEMPTS) {
            retryAttempts.put(session.getId(), attempts + 1);
            logger.info("尝试重新连接，当前重试次数: {}", attempts + 1);
            // 尝试重新建立连接
            try {
                if (!session.isOpen()) {
                    sessions.remove(session.getId());
                    lastHeartbeatTimes.remove(session.getId());
                    retryAttempts.remove(session.getId());
                }
            } catch (Exception e) {
                logger.error("重新连接失败: {}", e.getMessage());
            }
        } else {
            logger.error("达到最大重试次数，关闭连接");
            session.close(CloseStatus.SERVER_ERROR);
            sessions.remove(session.getId());
            lastHeartbeatTimes.remove(session.getId());
            retryAttempts.remove(session.getId());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket连接已关闭: {}, 状态: {}", session.getId(), status);
        sessions.remove(session.getId());
        lastHeartbeatTimes.remove(session.getId());
        retryAttempts.remove(session.getId());
        // 清理与此会话相关的资源
        speechToTextService.cleanupSession(session.getId());
    }

    private void startHeartbeatCheck(WebSocketSession session) {
        new Thread(() -> {
            try {
                // 给客户端一些时间来准备接收心跳
                Thread.sleep(2000);
                
                while (session.isOpen()) {
                    try {
                        Thread.sleep(HEARTBEAT_INTERVAL);
                        Long lastHeartbeat = lastHeartbeatTimes.get(session.getId());
                        if (lastHeartbeat == null || System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT) {
                            logger.warn("会话 {} 心跳超时 (最后心跳时间: {}, 当前时间: {})", 
                                session.getId(), 
                                lastHeartbeat != null ? new java.util.Date(lastHeartbeat) : "未知",
                                new java.util.Date());
                            session.close(CloseStatus.SESSION_NOT_RELIABLE);
                            break;
                        }
                        // 主动发送心跳
                        session.sendMessage(new TextMessage("ping"));
                        logger.debug("向会话 {} 发送心跳", session.getId());
                    } catch (Exception e) {
                        logger.error("心跳检测出错: {}, 会话ID: {}", e.getMessage(), session.getId());
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("心跳线程初始化错误: {}", e.getMessage());
            }
        }, "heartbeat-" + session.getId()).start();
    }
    
    /**
     * 向特定会话发送消息
     */
    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.error("发送消息到会话 {} 时出错", sessionId, e);
            }
        }
    }
    
    /**
     * 向所有活动会话广播消息
     */
    public void broadcastMessage(String message) {
        sessions.forEach((id, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.error("广播消息到会话 {} 时出错", id, e);
                }
            }
        });
    }
}