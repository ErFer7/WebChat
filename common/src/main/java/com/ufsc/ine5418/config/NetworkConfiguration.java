package com.ufsc.ine5418.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import com.ufsc.ine5418.utils.Logger;

public class NetworkConfiguration {

	private final String gatewayHost;
	private final Integer gatewayPort;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final Integer applicationPort;

	public NetworkConfiguration() {
		JSONObject config = this.readJSONFile();

		try {
			assert config != null;

			this.gatewayHost = config.getString("gatewayHost");
			this.gatewayPort = config.getInt("gatewayPort");
			this.gatewayIdentifier = config.getString("gatewayIdentifier");
			this.gatewayPassword = config.getString("gatewayPassword");
			this.applicationPort = config.getInt("applicationPort");
		} catch (Exception exception) {
			Logger.log(this.getClass().getSimpleName(), "Error reading JSON file: " + exception.getMessage());
			throw exception;
		}
	}

	private JSONObject readJSONFile() {
		InputStream inputStream = null;

		try {
			File file = new File("network/config.json");
			inputStream = new FileInputStream(file);
			return new JSONObject(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
		} catch (Exception exception) {
			Logger.log(this.getClass().getSimpleName(), "Error reading JSON file: " + exception.getMessage());
			return null;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ioException) {
					Logger.log(this.getClass().getSimpleName(), "Error reading JSON file: " + ioException.getMessage());
				}
			}
		}
	}

	public String getGatewayHost() {
		return gatewayHost;
	}

	public Integer getGatewayPort() {
		return gatewayPort;
	}

	public String getGatewayIdentifier() {
		return gatewayIdentifier;
	}

	public String getGatewayPassword() {
		return gatewayPassword;
	}

	public Integer getApplicationPort() {
		return applicationPort;
	}
}
