import DeleteIcon from '@mui/icons-material/Delete'
import { Alert, Box, Button, ListItem, ListItemButton, ListItemText, TextField } from '@mui/material'
import PropTypes from 'prop-types'
import { useState } from 'react'

export default function CreateGroupForm({ groupForm, setGroupForm, handleCreateGroup, alert, setAlert }) {
  const [username, setUserName] = useState('')

  const handleGroupNameChange = (event) => {
    setGroupForm({ ...groupForm, groupName: event.target.value })
    alert && setAlert(null)
  }

  const handleUsernameChange = (event) => {
    setUserName(event.target.value)
    alert && setAlert(null)
  }

  const handleAddUser = () => {
    if (username === '') return
    const prevUsernames = groupForm.usernames
    setGroupForm({ ...groupForm, usernames: [...prevUsernames, username] })
    setUserName('')
    alert && setAlert(null)
  }

  const handleRemoveUser = (e, username) => {
    const prevUsernames = groupForm.usernames
    setGroupForm({
      ...groupForm,
      usernames: prevUsernames.filter((prevUsername) => prevUsername !== username),
    })
    alert && setAlert(null)
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    handleCreateGroup(groupForm)
  }

  return (
    <Box component='form' onSubmit={handleSubmit} display='flex' flexDirection='column'>
      <TextField label='Nome do grupo' value={groupForm.groupName} onChange={handleGroupNameChange} sx={{ mt: 2 }} />
      <TextField label='Username' value={username} onChange={handleUsernameChange} sx={{ mt: 2 }} />
      <Button variant='outlined' color='primary' type='button' onClick={handleAddUser} sx={{ mt: 2 }}>
        Adicionar usuário
      </Button>
      {groupForm?.usernames?.map((username, i) => (
        <ListItem key={i} sx={{ mt: 2 }} disablePadding>
          <ListItemButton role={undefined} onClick={(e) => handleRemoveUser(e, username)} dense>
            <DeleteIcon sx={{ mr: 2 }} />
            <ListItemText primary={username} />
          </ListItemButton>
        </ListItem>
      ))}
      <Button
        variant='contained'
        color='primary'
        type='submit'
        sx={{ mt: 2 }}
        disabled={groupForm.usernames.length === 0}
      >
        Criar grupo
      </Button>
      {alert && (
        <Alert severity={alert.severity} sx={{ mt: 2 }}>
          {alert.message}
        </Alert>
      )}
    </Box>
  )
}

CreateGroupForm.propTypes = {
  groupForm: PropTypes.object,
  setGroupForm: PropTypes.func,
  handleCreateGroup: PropTypes.func,
  alert: PropTypes.object,
  setAlert: PropTypes.func,
}
