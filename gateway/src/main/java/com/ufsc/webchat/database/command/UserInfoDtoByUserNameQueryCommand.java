package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QUser.user;

import com.querydsl.core.types.Projections;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.UserInfoDto;

public class UserInfoDtoByUserNameQueryCommand {

	public UserInfoDto execute(String identifier) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(Projections.constructor(UserInfoDto.class, user.id, user.passwordHash))
					.from(user)
					.where(user.name.eq(identifier))
					.fetchOne();
		}
	}
}
