package com.ufsc.webchat.database.validator;

import static org.hibernate.internal.util.StringHelper.isEmpty;

import com.ufsc.webchat.database.command.UserListByNameCommand;
import com.ufsc.webchat.database.model.UserDto;

public class UserRegisterValidator {

	private final UserListByNameCommand userListByNameCommand = new UserListByNameCommand();

	public boolean validate(UserDto userDto) {
		if (isEmpty(userDto.getIdentifier()) || isEmpty(userDto.getPassword())) {
			return false;
		}

		return this.userListByNameCommand.execute(userDto.getIdentifier()).isEmpty();
	}
}
