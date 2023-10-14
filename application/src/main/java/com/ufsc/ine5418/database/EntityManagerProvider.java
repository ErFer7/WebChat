package com.ufsc.ine5418.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class EntityManagerProvider {

	private static final String PERSISTENCE_UNIT_NAME = "EntityManager";
	private static final EntityManagerFactory entityManagerFactory;

	static {
		entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
	}

	public static EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	public static void closeEntityManagerFactory() {
		if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}
}