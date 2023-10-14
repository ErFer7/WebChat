package com.ufsc.ine5418.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {

	public void loadProperties() {
		Properties props = new Properties();
		try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("config/application.properties")) {
			props.load(input);
			for (String key : props.stringPropertyNames()) {
				System.setProperty(key, props.getProperty(key));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
