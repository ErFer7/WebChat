package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QUser.user;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;

public class UserIdByNameQueryCommand {
	public Long execute(String username) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(user.id)
					.from(user)
					.where(user.name.eq(username))
					.fetchOne();
		}
	}
}
