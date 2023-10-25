import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import { Box, Divider, List, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import PropTypes from 'prop-types'
import { useState } from 'react'

export function UserList({ users, handleClickUser }) {
  const [selectedIndex, setSelectedIndex] = useState(1)

  const handleListItemClick = (event, index, username) => {
    setSelectedIndex(index)
    handleClickUser(username)
  }

  return (
    <Box sx={{ width: '100%', bgcolor: 'background.paper' }}>
      <List component='nav' aria-label='main mailbox folders'>
        {users.map((user) => {
          return (
            <div key={user.id}>
              <ListItemButton
                selected={selectedIndex === user.id}
                onClick={(event) => handleListItemClick(event, user.id, user.username)}
              >
                <ListItemIcon>
                  <AccountCircleIcon />
                </ListItemIcon>
                <ListItemText primary={user.username} />
              </ListItemButton>
              <Divider />
            </div>
          )
        })}
      </List>
    </Box>
  )
}

UserList.propTypes = {
  users: PropTypes.arrayOf(PropTypes.object),
  handleClickUser: PropTypes.func,
}
