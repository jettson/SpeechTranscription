import React from 'react';
import { AppBar, Toolbar, Typography, Container } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
  },
  title: {
    flexGrow: 1,
    fontWeight: 600,
  },
  appBar: {
    marginBottom: theme.spacing(2),
  },
}));

function Header() {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <AppBar position="static" className={classes.appBar}>
        <Container>
          <Toolbar>
            <Typography variant="h6" className={classes.title}>
              实时语音转文字系统
            </Typography>
          </Toolbar>
        </Container>
      </AppBar>
    </div>
  );
}

export default Header;