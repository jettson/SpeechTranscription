import React from 'react';
import { Paper, Typography, Box, Button } from '@material-ui/core';
import { Save, FileCopy } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  transcriptionContainer: {
    padding: theme.spacing(3),
    backgroundColor: theme.palette.background.paper,
    borderRadius: theme.shape.borderRadius,
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    minHeight: 150,
    position: 'relative',
  },
  transcriptionText: {
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
    minHeight: 80,
  },
  placeholder: {
    color: theme.palette.text.secondary,
    fontStyle: 'italic',
  },
  buttonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: theme.spacing(2),
  },
  actionButton: {
    margin: theme.spacing(0, 0.5),
  },
  recordingIndicator: {
    position: 'absolute',
    top: theme.spacing(1),
    right: theme.spacing(1),
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(0.5, 1),
    backgroundColor: theme.palette.error.main,
    color: theme.palette.error.contrastText,
    borderRadius: theme.shape.borderRadius,
    fontSize: '0.75rem',
  },
}));

function TranscriptionDisplay({ text, isRecording, onSave }) {
  const classes = useStyles();

  // 复制文本到剪贴板
  const copyToClipboard = () => {
    if (text) {
      navigator.clipboard.writeText(text)
        .then(() => {
          alert('文本已复制到剪贴板');
        })
        .catch(err => {
          console.error('复制失败:', err);
          alert('复制失败，请手动复制');
        });
    }
  };

  return (
    <div>
      <Typography variant="h6" gutterBottom>
        转写结果
      </Typography>
      
      <Paper className={classes.transcriptionContainer}>
        {isRecording && (
          <div className={classes.recordingIndicator}>
            实时转写中...
          </div>
        )}
        
        <Box className={classes.transcriptionText}>
          {text ? (
            <Typography variant="body1">{text}</Typography>
          ) : (
            <Typography variant="body1" className={classes.placeholder}>
              {isRecording ? '正在聆听，请说话...' : '点击"开始录音"按钮开始语音转写'}
            </Typography>
          )}
        </Box>
        
        <Box className={classes.buttonContainer}>
          <Button
            variant="contained"
            color="primary"
            className={classes.actionButton}
            startIcon={<Save />}
            onClick={onSave}
            disabled={!text || isRecording}
          >
            保存
          </Button>
          <Button
            variant="outlined"
            color="primary"
            className={classes.actionButton}
            startIcon={<FileCopy />}
            onClick={copyToClipboard}
            disabled={!text}
          >
            复制
          </Button>
        </Box>
      </Paper>
    </div>
  );
}

export default TranscriptionDisplay;