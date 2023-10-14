package com.ufsc.ine5418.database;

import com.querydsl.jpa.impl.JPAQuery;

import jakarta.persistence.EntityManager;

public class JPAQueryFactory implements AutoCloseable {
	private final EntityManager entityManager;

	public JPAQueryFactory(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public <T> JPAQuery<T> createQuery() {
		return new JPAQuery<>(this.entityManager);
	}

	@Override public void close() {
		if (this.entityManager != null && this.entityManager.isOpen()) {
			this.entityManager.close();
		}
	}
}
