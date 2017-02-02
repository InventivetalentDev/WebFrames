package org.inventivetalent.webframes;

import org.inventivetalent.animatedframes.gson.JsonObject;

public class RenderError extends RuntimeException {

	private int    id;
	private String message;

	public RenderError(JsonObject json) {
		if (json.has("error")) {
			parseJSON(json.getAsJsonObject("error"));
		} else {
			parseJSON(json);
		}
	}

	void parseJSON(JsonObject json) {
		if (json.has("id")) {
			id = json.get("id").getAsInt();
		}
		if (json.has("message")) {
			message = json.get("message").getAsString();
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
