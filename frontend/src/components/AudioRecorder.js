import React, { useState, useEffect, useRef } from 'react';
import { Button, Box, Typography, CircularProgress } from '@material-ui/core';
import { Mic, MicOff } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import webSocketService from '../services/websocket';

// 音频配置
const SAMPLE_RATE = 16000;
const BUFFER_SIZE = 4096;
const CHANNELS = 1;
const BIT_DEPTH = 16;

const useStyles = makeStyles((theme) => ({
  recorderContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: theme.spacing(3),
    backgroundColor: theme.palette.background.paper,
    borderRadius: theme.shape.borderRadius,
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  buttonContainer: {
    display: 'flex',
    justifyContent: 'center',
    marginTop: theme.spacing(2),
  },
  recordButton: {
    margin: theme.spacing(1),
    padding: theme.spacing(1, 3),
  },
  recordingIndicator: {
    display: 'flex',
    alignItems: 'center',
    marginTop: theme.spacing(2),
  },
  recordingDot: {
    height: 12,
    width: 12,
    backgroundColor: theme.palette.error.main,
    borderRadius: '50%',
    marginRight: theme.spacing(1),
    animation: '$pulse 1.5s infinite',
  },
  '@keyframes pulse': {
    '0%': {
      opacity: 1,
    },
    '50%': {
      opacity: 0.3,
    },
    '100%': {
      opacity: 1,
    },
  },
}));

function AudioRecorder({ onTranscriptionUpdate, onRecordingStateChange }) {
  const classes = useStyles();
  const [isRecording, setIsRecording] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isPreparing, setIsPreparing] = useState(false);
  const [error, setError] = useState(null);
  const mediaRecorderRef = useRef(null);
  const audioContextRef = useRef(null);
  const streamRef = useRef(null);
  
  // 预初始化音频资源
  const prepareAudioResources = async () => {
    if (streamRef.current) return; // 如果已经初始化，则直接返回
    
    try {
      setIsPreparing(true);
      setError(null);
      
      // 获取麦克风权限
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: CHANNELS,
          sampleRate: SAMPLE_RATE
        }
      });
      streamRef.current = stream;
      
      // 创建AudioContext
      const audioContext = new (window.AudioContext || window.webkitAudioContext)({
        sampleRate: SAMPLE_RATE
      });
      audioContextRef.current = audioContext;
      
      setIsPreparing(false);
      console.log('音频资源已预初始化');
    } catch (err) {
      console.error('预初始化音频资源时出错:', err);
      setError(err.message || '无法访问麦克风');
      setIsPreparing(false);
    }
  };
  
  // 组件挂载时预初始化音频资源
  useEffect(() => {
    prepareAudioResources();
    
    return () => {
      // 组件卸载时清理资源
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
        streamRef.current = null;
      }
      
      if (audioContextRef.current) {
        audioContextRef.current.close();
        audioContextRef.current = null;
      }
      
      webSocketService.disconnect();
    };
  }, []);
  
  // 开始录音
  const startRecording = async () => {
    try {
      setError(null);
      setIsConnecting(true);
      
      // 如果没有预初始化，则进行初始化
      if (!streamRef.current || !audioContextRef.current) {
        await prepareAudioResources();
      }
      
      // 创建音频源节点
      const sourceNode = audioContextRef.current.createMediaStreamSource(streamRef.current);
      
      // 创建ScriptProcessor节点
      const scriptNode = audioContextRef.current.createScriptProcessor(BUFFER_SIZE, CHANNELS, CHANNELS);
      mediaRecorderRef.current = {
        sourceNode,
        scriptNode
      };
      
      try {
        // 设置WebSocket消息回调
        webSocketService.setOnMessageCallback((data) => {
          try {
            const parsedData = JSON.parse(data);
            if (parsedData.transcription) {
              onTranscriptionUpdate(parsedData.transcription);
            }
          } catch (err) {
            console.error('解析WebSocket消息时出错:', err);
          }
        });
        
        // 连接WebSocket
        await webSocketService.connect();
        setIsConnecting(false);
        
        // 处理音频数据
        scriptNode.onaudioprocess = (audioProcessingEvent) => {
          if (webSocketService.isWebSocketConnected()) {
            const inputData = audioProcessingEvent.inputBuffer.getChannelData(0);
            
            // 将Float32Array转换为Int16Array
            const pcmData = new Int16Array(inputData.length);
            for (let i = 0; i < inputData.length; i++) {
              // 将-1到1的浮点数转换为16位整数
              const s = Math.max(-1, Math.min(1, inputData[i]));
              pcmData[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
            }
            
            // 发送PCM数据
            webSocketService.sendAudioData(pcmData.buffer);
          }
        };
        
        // 连接节点并开始处理
        sourceNode.connect(scriptNode);
        scriptNode.connect(audioContextRef.current.destination);
        setIsRecording(true);
        onRecordingStateChange(true);
        
      } catch (wsError) {
        console.error('WebSocket连接错误:', wsError);
        setError('无法连接到服务器: ' + wsError.message);
        setIsConnecting(false);
      }
      
    } catch (err) {
      console.error('开始录音时出错:', err);
      setError(err.message || '无法访问麦克风');
      setIsConnecting(false);
    }
  };
  
  // 停止录音
  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      const { sourceNode, scriptNode } = mediaRecorderRef.current;
      
      // 断开音频处理节点
      sourceNode.disconnect(scriptNode);
      scriptNode.disconnect(audioContextRef.current.destination);
      
      // 关闭WebSocket连接
      webSocketService.disconnect();
      
      setIsRecording(false);
      onRecordingStateChange(false);
      mediaRecorderRef.current = null;
    }
  };
  
  return (
    <Box className={classes.recorderContainer}>
      <Typography variant="h6" gutterBottom>
        语音录制
      </Typography>
      
      {error && (
        <Typography color="error" variant="body2" gutterBottom>
          错误: {error}
        </Typography>
      )}
      
      <Box className={classes.buttonContainer}>
        {!isRecording ? (
          <Button
            variant="contained"
            color="primary"
            className={classes.recordButton}
            startIcon={<Mic />}
            onClick={startRecording}
            disabled={isConnecting || isPreparing}
          >
            {isConnecting ? '连接中...' : isPreparing ? '准备中...' : '开始录音'}
          </Button>
        ) : (
          <Button
            variant="contained"
            color="secondary"
            className={classes.recordButton}
            startIcon={<MicOff />}
            onClick={stopRecording}
          >
            停止录音
          </Button>
        )}
      </Box>
      
      {(isConnecting || isPreparing) && (
        <Box mt={2} display="flex" alignItems="center">
          <CircularProgress size={20} />
          <Typography variant="body2" style={{ marginLeft: 10 }}>
            {isConnecting ? '正在连接服务器...' : '正在准备录音...'}
          </Typography>
        </Box>
      )}
      
      {isRecording && (
        <Box className={classes.recordingIndicator}>
          <div className={classes.recordingDot}></div>
          <Typography variant="body2" color="error">
            正在录音...
          </Typography>
        </Box>
      )}
    </Box>
  );
}

export default AudioRecorder;