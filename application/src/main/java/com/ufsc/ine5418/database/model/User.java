package com.ufsc.ine5418.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"user\"")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", unique = true, nullable = false)
	private String name;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}
}