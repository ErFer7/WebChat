import { AccountCircle } from '@mui/icons-material'
import LogoutIcon from '@mui/icons-material/Logout'
import { AppBar, Box, Button, Grid, IconButton, Toolbar, Typography } from '@mui/material'
import PropTypes from 'prop-types'
import { useAuth } from '../../../hooks/useAuth'

function Header({ username }) {
  const { logout } = useAuth()

  return (
    <Grid container sx={{ mb: 4 }}>
      <Box sx={{ flexGrow: 1 }}>
        <AppBar position='static'>
          <Toolbar>
            <Typography
              variant='h6'
              component='div'
              sx={{ flexGrow: 1, fontFamily: 'monospace', fontWeight: 700, letterSpacing: '.3rem' }}
            >
              WEBCHAT
            </Typography>
            <Typography>{username}</Typography>
            <IconButton size='large' color='inherit' sx={{ mr: 2 }}>
              <AccountCircle />
            </IconButton>
            <Button onClick={logout} variant='outlined' color='inherit' startIcon={<LogoutIcon />}>
              Sair
            </Button>
          </Toolbar>
        </AppBar>
      </Box>
    </Grid>
  )
}

Header.propTypes = {
  username: PropTypes.string,
}

export { Header }
