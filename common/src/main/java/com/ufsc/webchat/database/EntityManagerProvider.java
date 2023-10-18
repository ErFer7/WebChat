package com.ufsc.webchat.database;

import static java.util.Objects.isNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class EntityManagerProvider {

	private static final String PERSISTENCE_UNIT_NAME = "webchat_db";
	private static EntityManagerFactory entityManagerFactory;

	private EntityManagerProvider() {
	}

	public static void init() {
		if (isNull(entityManagerFactory)) {
			entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
			Runtime.getRuntime().addShutdownHook(new Thread(EntityManagerProvider::close));
		}
	}

	public static EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	public static void close() {
		if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}
}