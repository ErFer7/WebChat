package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QUser.user;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;

public class UserNameByUserIdQueryCommand {

	public String execute(Long userId) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(user.name)
					.from(user)
					.where(user.id.eq(userId))
					.fetchOne();
		}
	}

}
