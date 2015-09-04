package org.inventivetalent.webframes;

import de.inventivegames.animatedframes.AnimatedFrames;
import org.json.JSONObject;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class RenderOptions {

	static final String URL_FORMAT = "&options=%s";

	Map<Option, Object> options = new HashMap<>();

	public RenderOptions() {
	}

	public boolean parseArguments(String[] args) {
		this.options = Option.parseOptions(args);
		return true;
	}

	public void loadJSON(JSONObject jsonObject) {
		for (Option option : Option.values()) {
			if (jsonObject.has(option.name)) {
				options.put(option, jsonObject.get(option.name));
			}
		}
	}

	protected JSONObject toJSON() {
		JSONObject json = new JSONObject();

		for (Map.Entry<Option, Object> entry : options.entrySet()) {
			String key = entry.getKey().name;
			Object value = entry.getValue();

			json.put(key, value);
		}

		return json;
	}

	protected String toURLVar() {
		JSONObject json = toJSON();
		if (json.length() == 0) { return ""; }
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

		public Object parseValue(String value) throws ScriptException {
			return AnimatedFrames.instance.scriptEngine.eval(value);
		}

		public static Option getForKey(String key) {
			for (Option option : values()) {
				if (option.key.equalsIgnoreCase(key)) {
					return option;
				}
			}
			return null;
		}

		public static Map<Option, Object> parseOptions(String[] args) throws IllegalArgumentException {
			Map<Option, Object> map = new HashMap<>();

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
					Object val = option.parseValue(value);
					map.put(option, val);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid number: " + value);
				} catch (ScriptException e) {
					throw new IllegalArgumentException(e.getMessage());
				}
				i++;
			}

			return map;
		}

	}

}
