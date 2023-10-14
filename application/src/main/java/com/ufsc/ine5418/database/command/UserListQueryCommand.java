package com.ufsc.ine5418.database.command;

import static com.ufsc.ine5418.database.model.QUser.user;

import java.util.List;

import com.querydsl.jpa.impl.JPAQuery;
import com.ufsc.ine5418.database.EntityManagerProvider;
import com.ufsc.ine5418.database.model.User;

import jakarta.persistence.EntityManager;

public class UserListQueryCommand {

	public List<User> execute() {
		EntityManager entityManager = EntityManagerProvider.getEntityManager();
		JPAQuery<User> query = new JPAQuery<>(entityManager);

		List<User> users = query.select(user).from(user).fetch();

		entityManager.close();
		return users;
	}
}
