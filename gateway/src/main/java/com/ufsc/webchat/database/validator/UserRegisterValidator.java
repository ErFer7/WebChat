package com.ufsc.webchat.database.validator;

import static org.hibernate.internal.util.StringHelper.isEmpty;

import com.ufsc.webchat.database.command.UserListByNameQueryCommand;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.model.ValidationMessage;

public class UserRegisterValidator {

	private final UserListByNameQueryCommand userListByNameCommand = new UserListByNameQueryCommand();

	public ValidationMessage validate(UserDto userDto) {
		if (isEmpty(userDto.getUsername()) || isEmpty(userDto.getPassword())) {
			return new ValidationMessage("Erro ao cadastrar usuário: usuário ou senha vazios.", false);
		}
		if (!this.userListByNameCommand.execute(userDto.getUsername()).isEmpty()) {
			return new ValidationMessage("Erro ao cadastrar usuário: usuário já existe.", false);
		}
		return new ValidationMessage(null, true);
	}

}
