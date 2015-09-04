package org.inventivetalent.webframes;

import de.inventivegames.animatedframes.AnimatedFrames;
import de.inventivegames.animatedframes.api.LoadListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;
import org.mcstats.MetricsLite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class WebFrames extends JavaPlugin {

	public static WebFrames instance;

	static final String RENDER_URL = "https://webrender.inventivetalent.org/?url=%s%s";

	private static WebFramesAPI api;

	@Override
	public void onLoad() {
		AnimatedFrames.getApi().addLoadListener(new LoadListener() {
			@Override
			public boolean listenFor(URL url, int width, int height, JSONObject meta) {
				return url.toString().contains("webrender.inventivetalent.org/renders/");
			}

			@Override
			public void onLoadImage(@Nonnull URL url, @Nonnull final int width, @Nonnull final int height, final JSONObject meta, @Nonnull final LoadCallback loadCallback) {
				if (meta == null) {
					loadCallback.call(url, width, height, meta);
					return;
				}
				try {
					URL siteURL = new URL(meta.getString("siteURL"));
					RenderOptions renderOptions = new RenderOptions();
					renderOptions.loadJSON(meta.getJSONObject("renderOptions"));

					getApi().preloadImage(siteURL, renderOptions, new Callback<URL>() {
						@Override
						public void call(URL value, @Nullable Throwable error) {
							loadCallback.call(value, width, height, meta);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onEnable() {
		if (!Bukkit.getPluginManager().isPluginEnabled("AnimatedFrames")) {
			getLogger().severe("****************************************");
			getLogger().severe(" ");
			getLogger().severe("  This plugin depends on AnimatedFrames ");
			getLogger().severe("https://www.spigotmc.org/resources/5583/");
			getLogger().severe(" ");
			getLogger().severe("****************************************");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		instance = this;

		getCommand("webframecreate").setExecutor(new CommandHandler());

		api = new API();

		try {
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("Metrics started");
			}
		} catch (Exception e) {
		}
	}

	public static WebFramesAPI getApi() {
		return api;
	}

	class API implements WebFramesAPI {

		@Override
		public void preloadImage(@Nonnull final URL url, @Nonnull final RenderOptions options, @Nonnull final Callback<URL> callback) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						URL renderURL = new URL(String.format(RENDER_URL, url.toString(), options.toURLVar()));
						URLConnection connection = renderURL.openConnection();
						connection.setConnectTimeout(30000);
						JSONObject json = readInputJSON(connection.getInputStream());
						if (json.has("error")) {
							throw new RenderError(json.getJSONObject("error"));
						}
						URL imageURL = new URL(json.getString("image"));
						callback.call(imageURL, null);
					} catch (Throwable e) {
						callback.call(null, e);
					}
				}
			}).start();
		}

		@Override
		public boolean startFrameCreation(@Nonnull final Player player, @Nonnull final URL url) {
			if (!url.toString().contains("webrender.inventivetalent.org/renders/")) { throw new IllegalArgumentException("Invalid image URL"); }
			return AnimatedFrames.getApi().startFrameCreation(player, url);
		}
	}

	private String readInputString(InputStream stream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String content = "";

		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				content += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return content;
	}

	private JSONObject readInputJSON(InputStream stream) {
		return new JSONObject(readInputString(stream));
	}
}
