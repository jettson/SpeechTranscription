package com.speechtranslate.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.speechtranslate.entity.TranscriptionRecord;
import com.speechtranslate.repository.TranscriptionRecordRepository;

import lombok.Data;

/**
 * 语音转文字服务
 * 负责处理音频数据并将其转换为文本
 */
@Service
public class SpeechToTextService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechToTextService.class);
    
    // 存储每个会话的转写状态
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();
    
    @Autowired
    private TranscriptionRecordRepository transcriptionRecordRepository;
    
    /**
     * 转写音频数据为文本
     * 
     * @param audioData 音频数据字节数组
     * @param sessionId WebSocket会话ID
     * @return 转写的文本结果
     */
    public String transcribeAudio(byte[] audioData, String sessionId) {
        // 获取或创建会话上下文
        SessionContext context = sessionContexts.computeIfAbsent(sessionId, id -> {
            SessionContext newContext = new SessionContext(id);
            return newContext;
        });
        
        try {
            // 调用AssemblyAI API进行语音识别
            String transcription = simulateSTT(audioData, context);
            
            // 如果有新的识别结果，记录并保存
            if (transcription != null && !transcription.isEmpty()) {
                logger.info("收到转写结果: {}", transcription);
                
                // 保存转写记录到数据库
                TranscriptionRecord record = new TranscriptionRecord();
                record.setSessionId(sessionId);
                record.setAudioData(audioData);
                record.setTranscriptionText(transcription);
                transcriptionRecordRepository.save(record);
                
                // 注意：不在这里添加到完整转写中，避免重复添加
                // 因为在onSentenceEnd回调中已经调用了appendTranscription
            } else if (context.getLatestTranscription() != null && !context.getLatestTranscription().isEmpty()) {
                // 如果没有新的识别结果，但有最新的识别结果，返回最新的识别结果
                transcription = context.getLatestTranscription();
                logger.debug("使用最新的识别结果: {}", transcription);
            }
            
            // 返回完整的转写结果
            return context.getFullTranscription();
        } catch (Exception e) {
            logger.error("转写音频时出错", e);
            throw new RuntimeException("转写音频失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理会话资源
     * 
     * @param sessionId WebSocket会话ID
     */
    // 此方法已被下方的完整实现替代
    
    // 阿里云语音识别配置
    @Value("${aliyun.access-key-id}")
    private String accessKeyId;
    
    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;
    
    @Value("${aliyun.nls.app-key}")
    private String appKey;
    
    @Value("${aliyun.nls.url}")
    private String nlsUrl;
    
    // 存储每个会话的NLS客户端
    private final Map<String, NlsClient> nlsClients = new ConcurrentHashMap<>();
    // 存储每个会话的语音转写器
    private final Map<String, SpeechTranscriber> speechTranscribers = new ConcurrentHashMap<>();

    private String simulateSTT(byte[] audioData, SessionContext context) {
        try {
            // 获取或创建语音转写器
            SpeechTranscriber transcriber = speechTranscribers.get(context.getSessionId());
            if (transcriber == null) {
                transcriber = createSpeechTranscriber(context);
                speechTranscribers.put(context.getSessionId(), transcriber);
                
                // 启动语音转写器
                transcriber.start();
                logger.info("已启动阿里云语音转写器，会话ID: {}", context.getSessionId());
            }
            
            // 发送音频数据
            if (audioData.length > 0) {
                InputStream audioStream = new ByteArrayInputStream(audioData);
                transcriber.send(audioStream);
              //Thread.sleep(50);
                logger.debug("发送音频数据: {} 字节", audioData.length);
            }
            // 返回最新的转写结果
            String result = context.getLatestTranscription();
            if (result == null || result.isEmpty()) {
                // 如果没有识别结果，返回空字符串，但记录日志表明音频数据已接收
                logger.info("接收到音频数据 {} 字节，但尚未产生识别结果，会话ID: {}", audioData.length, context.getSessionId());
                return "";
            }

            return result;
        } catch (Exception e) {
            logger.error("处理音频数据时出错", e);
            throw new RuntimeException("处理音频数据失败: " + e.getMessage(), e);
        }
    }
    
    private SpeechTranscriber createSpeechTranscriber(SessionContext context) {
        try {
            // 获取或创建NLS客户端
            NlsClient client = nlsClients.get(context.getSessionId());
            if (client == null) {
                // 创建AccessToken
                AccessToken token = new AccessToken(accessKeyId, accessKeySecret);
                token.apply();
                
                // 创建NLS客户端
                client = new NlsClient(token.getToken());
                nlsClients.put(context.getSessionId(), client);
                logger.info("已创建阿里云NLS客户端，会话ID: {}", context.getSessionId());
            }
            
            // 创建语音转写器
            SpeechTranscriber transcriber = new SpeechTranscriber(client, getTranscriberListener(context));
            
            // 设置参数
            transcriber.setAppKey(appKey);
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            transcriber.setEnableIntermediateResult(true); // 启用中间结果
            transcriber.setEnablePunctuation(true); // 启用标点符号
            transcriber.setEnableITN(true); // 启用ITN（数字和单位转换）
            
            logger.info("已创建阿里云语音转写器，会话ID: {}", context.getSessionId());
            return transcriber;
        } catch (Exception e) {
            logger.error("创建阿里云语音转写器时出错: {}", e.getMessage(), e);
            throw new RuntimeException("创建阿里云语音转写器失败: " + e.getMessage(), e);
        }
    }
    
    private SpeechTranscriberListener getTranscriberListener(final SessionContext context) {
        return new SpeechTranscriberListener() {
            // 识别出中间结果，服务端识别出一个字或词时会返回此消息
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                logger.debug("5555 task_id: " + response.getTaskId() +
                ", name: " + response.getName() +
                //状态码“20000000”表示正常识别。
                ", status: " + response.getStatus() +
                //句子编号，从1开始递增。
                ", index: " + response.getTransSentenceIndex() +
                //当前的识别结果。
                ", result: " + response.getTransSentenceText() +
                //置信度
                ", confidence: " + response.getConfidence() +
                //开始时间
                ", begin_time: " + response.getSentenceBeginTime() +
                //当前已处理的音频时长，单位为毫秒。
                ", time: " + response.getTransSentenceTime());
                String result = response.getTransSentenceText();
                logger.debug("收到中间转写结果: {}", result);
                context.setLatestTranscription(result);
            }
            
            // 一句话开始
            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {
                logger.debug("一句话开始，会话ID: {}", context.getSessionId());
            }
            
            // 识别出一句话，服务端会顺序返回多个结果
            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                String result = response.getTransSentenceText();
                logger.info("收到最终转写结果: {}", result);
                context.setLatestTranscription(result);
                context.appendTranscription(result);
            }
            
            // 识别完毕
            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                logger.info("转写完成，会话ID: {}", context.getSessionId());
            }
            
            // 识别失败
            @Override
            public void onFail(SpeechTranscriberResponse response) {
                logger.error("转写失败，错误信息: {}", response.getStatus());
            }
            
            // 转写器启动
            @Override
            public void onTranscriberStart(SpeechTranscriberResponse response) {
                logger.info("转写器启动，会话ID: {}", context.getSessionId());
            }
        };
    }
    
    /**
     * 清理会话资源
     * 当会话结束时，需要关闭并释放相关资源
     * 
     * @param sessionId WebSocket会话ID
     */
    public void cleanupSession(String sessionId) {
        // 关闭语音转写器
        SpeechTranscriber transcriber = speechTranscribers.remove(sessionId);
        if (transcriber != null) {
            try {
                transcriber.stop();
                logger.info("已停止阿里云语音转写器，会话ID: {}", sessionId);
            } catch (Exception e) {
                logger.error("停止阿里云语音转写器时出错: {}", e.getMessage());
            }
        }
        
        // 关闭NLS客户端
        NlsClient client = nlsClients.remove(sessionId);
        if (client != null) {
            try {
                // NlsClient没有close方法，使用shutdown方法替代
                client.shutdown();
                logger.info("已关闭阿里云NLS客户端，会话ID: {}", sessionId);
            } catch (Exception e) {
                logger.error("关闭阿里云NLS客户端时出错: {}", e.getMessage());
            }
        }
        
        // 移除会话上下文
        sessionContexts.remove(sessionId);
        logger.info("已清理会话 {} 的资源", sessionId);
    }
}