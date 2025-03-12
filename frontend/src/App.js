import React, { useState } from 'react';
import { ThemeProvider, createTheme } from '@material-ui/core/styles';
import { Container, CssBaseline, Paper, Typography, Box } from '@material-ui/core';
import AudioRecorder from './components/AudioRecorder';
import TranscriptionDisplay from './components/TranscriptionDisplay';
import Header from './components/Header';
import Footer from './components/Footer';

// 创建主题
const theme = createTheme({
  palette: {
    primary: {
      main: '#3f51b5',
    },
    secondary: {
      main: '#f50057',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
  },
});

function App() {
  // 状态管理
  const [transcription, setTranscription] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [history, setHistory] = useState([]);

  // 处理转写更新
  const handleTranscriptionUpdate = (text) => {
    setTranscription(text);
  };

  // 处理录音状态变化
  const handleRecordingStateChange = (recording) => {
    setIsRecording(recording);
  };

  // 保存转写历史
  const saveTranscription = () => {
    if (transcription.trim()) {
      const newEntry = {
        id: Date.now(),
        text: transcription,
        timestamp: new Date().toLocaleString(),
      };
      setHistory([newEntry, ...history]);
      setTranscription('');
    }
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Header />
      <Container maxWidth="md">
        <Box my={4}>
          <Paper elevation={3} style={{ padding: '2rem' }}>
            <Typography variant="h4" component="h1" gutterBottom align="center">
              实时语音转文字
            </Typography>
            
            <AudioRecorder 
              onTranscriptionUpdate={handleTranscriptionUpdate}
              onRecordingStateChange={handleRecordingStateChange}
            />
            
            <Box mt={3}>
              <TranscriptionDisplay 
                text={transcription} 
                isRecording={isRecording}
                onSave={saveTranscription}
              />
            </Box>
            
            {history.length > 0 && (
              <Box mt={4}>
                <Typography variant="h5" gutterBottom>
                  历史记录
                </Typography>
                {history.map((entry) => (
                  <Paper key={entry.id} elevation={1} style={{ padding: '1rem', marginBottom: '1rem' }}>
                    <Typography variant="body1">{entry.text}</Typography>
                    <Typography variant="caption" color="textSecondary">
                      {entry.timestamp}
                    </Typography>
                  </Paper>
                ))}
              </Box>
            )}
          </Paper>
        </Box>
      </Container>
      <Footer />
    </ThemeProvider>
  );
}

export default App;