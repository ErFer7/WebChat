import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import { Avatar, Box, Button, CssBaseline, Grid, Paper, TextField, Typography } from '@mui/material'

import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import useWebSocket from 'react-use-websocket'
import { useAuth } from '../../hooks/useAuth'

function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const { sendJsonMessage, lastJsonMessage } = useWebSocket('ws://127.0.0.1:8080', {
    onOpen: () => console.log(`Connected to App WS`),
    onClose: () => console.log('Connection Closed'),
  })

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [gatewayConnectionInfo, setGatewayConnectionInfo] = useState({})

  const handleUsernameChange = (event) => setUsername(event.target.value)
  const handlePasswordChange = (event) => setPassword(event.target.value)

  const handleSubmit = (event) => {
    event.preventDefault()
    const buttonName = event.nativeEvent.submitter.name
    if (buttonName == 'login') {
      const loginPacket = {
        id: gatewayConnectionInfo.id,
        hostType: 'CLIENT',
        token: null,
        status: null,
        operationType: 'REQUEST',
        payloadType: 'ROUTING',
        payload: {
          username: username,
          password: password,
          host: gatewayConnectionInfo.host,
        },
      }
      sendJsonMessage(loginPacket)
    } else if (buttonName == 'register') {
      // TODO: send register packet and receive register packet
    }
  }

  useEffect(() => {
    if (lastJsonMessage) {
      const data = lastJsonMessage
      if (data?.operationType == 'INFO' && data?.payloadType == 'HOST') {
        setGatewayConnectionInfo({
          id: data?.id,
          host: data?.payload.host,
        })
      } else if (data?.payloadType == 'ROUTING' && data?.status == 'OK') {
        login(data)
      }
      console.log(data)
    }
  }, [lastJsonMessage, login])

  return isAuthenticated ? (
    <Navigate to='/' />
  ) : (
    <Grid container component='main' sx={{ height: '100vh' }}>
      <CssBaseline />
      <Grid
        item
        xs={false}
        sm={4}
        md={7}
        sx={{
          backgroundImage: 'url(https://source.unsplash.com/random?wallpapers)',
          backgroundRepeat: 'no-repeat',
          backgroundColor: (t) => (t.palette.mode === 'light' ? t.palette.grey[50] : t.palette.grey[900]),
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      />
      <Grid item xs={12} sm={8} md={5} component={Paper} elevation={6} square>
        <Box
          sx={{
            my: 8,
            mx: 4,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}
        >
          <Avatar sx={{ m: 1, mb: 2, bgcolor: 'secondary.main' }}>
            <LockOutlinedIcon />
          </Avatar>
          <Typography component='h1' variant='h5'>
            WebChat
          </Typography>
          <Box component='form' noValidate onSubmit={handleSubmit} sx={{ mt: 1 }}>
            <TextField
              margin='normal'
              required
              fullWidth
              id='username'
              label='Username'
              name='username'
              autoComplete='username'
              autoFocus
              onChange={handleUsernameChange}
            />
            <TextField
              margin='normal'
              required
              fullWidth
              name='password'
              label='Password'
              type='password'
              id='password'
              autoComplete='current-password'
              onChange={handlePasswordChange}
            />
            <Button type='submit' name='login' fullWidth variant='contained' sx={{ mt: 3, mb: 2 }}>
              Login
            </Button>
            <Button type='submit' name='register' fullWidth variant='outlined' sx={{ mb: 2 }}>
              Register
            </Button>
          </Box>
        </Box>
      </Grid>
    </Grid>
  )
}

export default LoginPage
