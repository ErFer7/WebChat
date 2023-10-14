package com.ufsc.ine5418.database;

import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConnection {

	private HikariDataSource dataSource;

	public DatabaseConnection() {
		this.initializeDataSource();
	}

	private void initializeDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(System.getProperty("db.url"));
		config.setUsername(System.getProperty("db.user"));
		config.setPassword(System.getProperty("db.password"));
		this.dataSource = new HikariDataSource(config);
	}

	public void connect() {
		try {
			this.dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
