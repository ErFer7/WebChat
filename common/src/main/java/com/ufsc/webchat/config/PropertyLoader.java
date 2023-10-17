package com.ufsc.webchat.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {

	private PropertyLoader() {
	}

	public static void loadAndSetSystemProperties(String resourcePath) {
		Properties props = new Properties();
		try (InputStream input = PropertyLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
			props.load(input);
			for (String key : props.stringPropertyNames()) {
				System.setProperty(key, props.getProperty(key));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
