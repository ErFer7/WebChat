package com.ufsc.ine5418.protocol.utils;

import org.json.JSONObject;

public class FieldParser {

	public static String nullableFieldToString(JSONObject jsonObject, String field) {
		try {
			return jsonObject.getString(field);
		} catch (Exception ignored) {
			return null;
		}
	}

	public static JSONObject nullableFieldToJSONObject(JSONObject jsonObject, String field) {
		try {
			return jsonObject.getJSONObject(field);
		} catch (Exception ignored) {
			return null;
		}
	}
}
