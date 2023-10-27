import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  CssBaseline,
  Grid,
  Paper,
  TextField,
  Typography,
} from '@mui/material'

import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { gatewayLogin, gatewayRegister } from './ClientService'

export default function LoginPage() {
  const { isAuthenticated, login, clientId } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState()

  const handleUsernameChange = (event) => {
    setUsername(event.target.value)
    alert && setAlert(null)
  }
  const handlePasswordChange = (event) => {
    setPassword(event.target.value)
    alert && setAlert(null)
  }

  const handleLogin = () => {
    gatewayLogin({ clientId, username, password })
      .then((response) => login(response.data))
      .catch((error) => setAlert({ severity: 'error', message: error.response.data }))
      .finally(() => setLoading(false))
  }

  const handleRegister = () => {
    gatewayRegister({ clientId, username, password })
      .then((data) => data.status == '201' && setAlert({ severity: 'success', message: 'Usuário criado com sucesso!' }))
      .catch((error) => setAlert({ severity: 'error', message: error.response.data }))
      .finally(() => setLoading(false))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    const buttonName = event.nativeEvent.submitter.name
    if (buttonName == 'login') {
      setLoading(true)
      handleLogin()
    } else if (buttonName == 'register') {
      handleRegister()
      setLoading(true)
    }
  }

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
          backgroundImage:
            'url(https://images.unsplash.com/photo-1560983073-c29bff7438ef?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8d2FsbHBhcGVyc3x8fHx8fDE2OTgwMjEwOTY&ixlib=rb-4.0.3&q=80&utm_campaign=api-credit&utm_medium=referral&utm_source=unsplash_source&w=1080)',
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
            Webchat
          </Typography>
          <Box sx={{ width: '90%', flexGrow: 1, mt: 1 }} component='form' noValidate onSubmit={handleSubmit}>
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
            {loading && <CircularProgress />}
            {alert && (
              <Alert severity={alert.severity} sx={{ width: '100%', flexGrow: 1, mt: 2 }}>
                {alert.message}
              </Alert>
            )}
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
