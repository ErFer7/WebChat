package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QUser.user;

import java.util.List;

import com.querydsl.core.types.Projections;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.UserDto;

public class UserDtoListQueryCommand {

	public List<UserDto> execute() {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(Projections.constructor(UserDto.class, user.id, user.name))
					.from(user)
					.fetch();
		}
	}
}
