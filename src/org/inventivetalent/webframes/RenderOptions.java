package org.inventivetalent.webframes;

import org.inventivetalent.animatedframes.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class RenderOptions {

	static final String URL_FORMAT = "&options=%s";

	Map<Option, String> options = new HashMap<>();

	public RenderOptions() {
	}

	public boolean parseArguments(String[] args) {
		this.options = Option.parseOptions(args);
		return true;
	}

	public void loadJSON(JsonObject jsonObject) {
		if (jsonObject == null) { return; }
		for (Option option : Option.values()) {
			if (jsonObject.has(option.name)) {
				options.put(option, jsonObject.get(option.name).getAsString());
			}
		}
	}

	protected JsonObject toJSON() {
		JsonObject json = new JsonObject();

		for (Map.Entry<Option, String> entry : options.entrySet()) {
			String key = entry.getKey().name;
			String value = entry.getValue();

			json.addProperty(key, value);
		}

		return json;
	}

	protected String toURLVar() {
		JsonObject json = toJSON();
		if (json.entrySet().size() == 0) { return ""; }
		return String.format(URL_FORMAT, json.toString());
	}

	enum Option {

		WIDTH("w", "width"),
		HEIGHT("h", "height"),
		QUALITY("q", "quality"),
		ZOOM("z", "zoom"),
		JAVASCRIPT_DELAY("d", "javascript-delay");

		String key;
		String name;

		Option(String key, String name) {
			this.key = key;
			this.name = name;
		}

		public String parseValue(String value) throws NumberFormatException {
			return String.valueOf(Integer.parseInt(value));
		}

		public static Option getForKey(String key) {
			for (Option option : values()) {
				if (option.key.equalsIgnoreCase(key)) {
					return option;
				}
			}
			return null;
		}

		public static Map<Option, String> parseOptions(String[] args) throws IllegalArgumentException {
			Map<Option, String> map = new HashMap<>();

			if (args.length % 2 != 0) {
				throw new IllegalArgumentException("Missing option argument");
			}

			for (int i = 0; i < args.length - 1; i++) {
				String key = args[i];
				if (!key.startsWith("-")) {
					throw new IllegalArgumentException("Invalid option key: " + key);
				}
				key = key.substring(1);
				Option option = getForKey(key);
				if (option == null) {
					throw new IllegalArgumentException("Unknown option key: " + key);
				}

				String value = args[i + 1];
				try {
					map.put(option, option.parseValue(value));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid number: " + value);
				} catch (Exception e) {
					throw new IllegalArgumentException(e.getMessage());
				}
				i++;
			}

			return map;
		}

	}

}