import React from 'react';
import { Typography, Container, Box, Link } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  footer: {
    padding: theme.spacing(3, 2),
    marginTop: 'auto',
    backgroundColor: theme.palette.grey[200],
  },
}));

function Footer() {
  const classes = useStyles();
  const currentYear = new Date().getFullYear();

  return (
    <footer className={classes.footer}>
      <Container maxWidth="md">
        <Box py={3} display="flex" flexDirection="column" alignItems="center">
          <Typography variant="body2" color="textSecondary" align="center">
            {'© '}
            {currentYear}
            {' '}
            <Link color="inherit" href="#">
              实时语音转文字系统
            </Link>
          </Typography>
          <Typography variant="body2" color="textSecondary" align="center" style={{ marginTop: 8 }}>
            使用最先进的语音识别技术，为您提供高质量的实时转写服务
          </Typography>
        </Box>
      </Container>
    </footer>
  );
}

export default Footer;