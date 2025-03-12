package com.speechtranslate.service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.UUID;

import lombok.Data;

/**
 * 会话上下文
 * 用于存储和管理单个会话的转写状态
 */
@Data
public class SessionContext {
    // 会话ID
    private final String sessionId;
    
    // 存储最新的中间识别结果
    private String latestTranscription = "";
    
    // 存储所有已完成的句子
    private final ConcurrentLinkedQueue<String> completedSentences = new ConcurrentLinkedQueue<>();
    
    /**
     * 默认构造函数，使用随机UUID作为会话ID
     */
    public SessionContext() {
        this.sessionId = UUID.randomUUID().toString();
    }
    
    /**
     * 带会话ID的构造函数
     * 
     * @param sessionId 外部传入的会话ID
     */
    public SessionContext(String sessionId) {
        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
    }
    
    /**
     * 设置最新的中间识别结果
     */
    public void setLatestTranscription(String transcription) {
        this.latestTranscription = transcription != null ? transcription : "";
    }
    
    /**
     * 添加一个完整的句子到历史记录
     */
    public void appendTranscription(String transcription) {
        if (transcription != null && !transcription.isEmpty()) {
            completedSentences.offer(transcription);
        }
    }
    
    /**
     * 获取最新的转写结果
     */
    public String getLatestTranscription() {
        return latestTranscription;
    }
    
    /**
     * 获取所有已完成的转写结果
     */
    public String getFullTranscription() {
        return completedSentences.stream()
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * 获取会话ID
     * @return 当前会话的唯一标识符
     */
    public String getSessionId() {
        return sessionId;
    }
}