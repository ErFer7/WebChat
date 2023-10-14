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
	private final Integer applicationPort;

	public NetworkConfiguration() {
		JSONObject config = this.readJSONFile();

		try {
			assert config != null;

			this.gatewayHost = config.getString("gatewayHost");
			this.gatewayPort = config.getInt("gatewayPort");
			this.applicationPort = config.getInt("applicationPort");
		} catch (Exception e) {
			Logger.log(this.getClass().getSimpleName(), "Error reading JSON file: " + e.getMessage());
			throw e;
		}
	}

	private JSONObject readJSONFile() {
		InputStream inputStream = null;

		try {
			File file = new File("network/config.json");
			inputStream = new FileInputStream(file);
			return new JSONObject(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
		} catch (Exception e) {
			Logger.log(this.getClass().getSimpleName(), "Error reading JSON file: " + e.getMessage());
			return null;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					Logger.log(this.getClass().getSimpleName(), "Error reading JSON file: " + e.getMessage());
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

	public Integer getApplicationPort() {
		return applicationPort;
	}
}
