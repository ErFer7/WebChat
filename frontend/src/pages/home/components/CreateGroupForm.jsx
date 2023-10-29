import {
  Alert,
  Box,
  Button,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  OutlinedInput,
  Select,
  TextField,
} from '@mui/material'
import PropTypes from 'prop-types'

const ITEM_HEIGHT = 48
const ITEM_PADDING_TOP = 8
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
}

function CreateGroupForm({ groupForm, setGroupForm, handleCreateGroup, alert, setAlert, userList }) {
  const handleGroupNameChange = (event) => {
    setGroupForm({ ...groupForm, groupName: event.target.value })
    alert && setAlert(null)
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    handleCreateGroup(groupForm)
    setGroupForm({ groupName: '', usernames: [] })
  }

  const handleChange = (event) => {
    const {
      target: { value },
    } = event

    setGroupForm({ ...groupForm, usernames: typeof value === 'string' ? value.split(',') : value })
  }

  return (
    <Box component='form' onSubmit={handleSubmit} display='flex' flexDirection='column'>
      <TextField label='Nome do grupo' value={groupForm.groupName} onChange={handleGroupNameChange} sx={{ mt: 2 }} />
      <FormControl sx={{ mt: 2 }}>
        <InputLabel id='demo-multiple-chip-label'>Usuários</InputLabel>
        <Select
          multiple
          value={groupForm.usernames}
          onChange={handleChange}
          input={<OutlinedInput label='Usuários' />}
          renderValue={(selected) => (
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              {selected.map((value) => (
                <Chip key={value} label={value} />
              ))}
            </Box>
          )}
          MenuProps={MenuProps}
        >
          {userList.map((user) => (
            <MenuItem key={user.id} value={user.username}>
              {user.username}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
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
  userList: PropTypes.arrayOf(PropTypes.object),
}

export { CreateGroupForm }
