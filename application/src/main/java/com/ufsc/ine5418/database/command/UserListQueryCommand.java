package com.ufsc.ine5418.database.command;

import static com.ufsc.ine5418.database.model.QUser.user;

import java.util.List;

import com.ufsc.ine5418.database.EntityManagerProvider;
import com.ufsc.ine5418.database.JPAQueryFactory;
import com.ufsc.ine5418.database.model.User;

public class UserListQueryCommand {

	public List<User> execute() {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(user)
					.from(user)
					.fetch();
		}
	}
}
