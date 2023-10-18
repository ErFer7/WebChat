package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QUser.user;

import java.util.List;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.User;

public class UserListByNameCommand {

	public List<User> execute(String identifier) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(user)
					.from(user)
					.where(user.name.eq(identifier))
					.fetch();
		}
	}
}
