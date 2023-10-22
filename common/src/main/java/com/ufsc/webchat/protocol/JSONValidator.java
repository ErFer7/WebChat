package com.ufsc.webchat.protocol;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class JSONValidator {

	public static List<String> validate(JSONObject jsonObject, List<String> requiredFields) {
		List<String> missingFields = new ArrayList<>();
		for (String field : requiredFields) {
			if (!jsonObject.has(field)) {
				missingFields.add(field);
			}
		}
		return missingFields;
	}

	// TODO: Validate de jsonString? Antes de criar a packet?
	// TODO: Avaliar possibilidade de verificar tipos dos campos também.

}
