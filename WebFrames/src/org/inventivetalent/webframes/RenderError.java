package org.inventivetalent.webframes;

import org.json.JSONObject;

public class RenderError extends RuntimeException {

	private int    id;
	private String message;

	public RenderError(JSONObject json) {
		if (json.has("error")) {
			parseJSON(json.getJSONObject("error"));
		} else {
			parseJSON(json);
		}
	}

	void parseJSON(JSONObject json) {
		if (json.has("id")) {
			id = json.getInt("id");
		}
		if (json.has("message")) {
			message = json.getString("message");
		}
	}

	public int getId() {
		return id;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "RenderError[#" + id + ":" + message + "]";
	}
}
