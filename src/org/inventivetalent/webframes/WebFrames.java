package org.inventivetalent.webframes;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.animatedframes.event.AsyncFrameCreationEvent;
import org.inventivetalent.animatedframes.event.AsyncFrameLoadEvent;
import org.inventivetalent.animatedframes.event.AsyncImageRequestEvent;
import org.inventivetalent.animatedframes.gson.JsonObject;
import org.inventivetalent.animatedframes.gson.JsonParser;
import org.inventivetalent.update.spiget.SpigetUpdate;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class WebFrames extends JavaPlugin {

	public static WebFrames instance;

	static final String RENDER_URL = "https://api.webrender.co/render?url=%s&format=png%s";

	static Map<String, String> originalUrls = new HashMap<>();

	static API api;
	SpigetUpdate spigetUpdate;

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
		getLogger().info("Powered by WebRender.co");

		instance = this;

		Bukkit.getPluginManager().registerEvents(new Listener() {

			@EventHandler
			public void on(final AsyncFrameCreationEvent event) throws MalformedURLException {
				if (event.getSource().toLowerCase().contains("webrender.co")) {
					final String site = originalUrls.get(event.getSource());
					event.getMeta().addProperty("siteURL", site);
				}
			}

			@EventHandler
			public void on(final AsyncFrameLoadEvent event) {
				if (event.getFrame().getImageSource().contains("img.webrender.co/")) {
					JsonObject meta = event.getFrame().getMeta();
					if (meta == null || !meta.has("siteURL")) { return; }
					try {
						URL siteURL = new URL(meta.get("siteURL").getAsString());
						RenderOptions renderOptions = new RenderOptions();
						renderOptions.loadJSON(meta.getAsJsonObject("renderOptions"));

						getApi().preloadImage(siteURL, renderOptions, new Callback<String>() {
							@Override
							public void call(String value, Throwable error) {
								event.getFrame().setImageSource(value);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@EventHandler
			public void on(AsyncImageRequestEvent event) {
				if (event.getSource().contains("webrender.co")) {
					event.setShouldDownload(true);
				}
			}

			@EventHandler
			public void on(final PlayerJoinEvent event) {
				if (event.getPlayer().hasPermission("webframes.updatecheck")) {
					spigetUpdate.checkForUpdate(new UpdateCallback() {
						@Override
						public void updateAvailable(String s, String s1, boolean b) {
							event.getPlayer().sendMessage("§aA new version for §6WebFrames §ais available (§7v" + s + "§a). Download it from https://r.spiget.org/11840");
						}

						@Override
						public void upToDate() {
						}
					});
				}
			}
		}, this);

		CommandHandler commandHandler;
		PluginCommand command = getCommand("webframecreate");
		command.setExecutor(commandHandler = new CommandHandler());
		command.setTabCompleter(commandHandler);

		api = new API();

		spigetUpdate = new SpigetUpdate(this, 11840)
				.setUserAgent("WebFrames/" + getDescription().getVersion());
		spigetUpdate.checkForUpdate(new UpdateCallback() {
			@Override
			public void updateAvailable(String s, String s1, boolean b) {
				getLogger().info("A new version is available (" + s + "). Download it from https://r.spiget.org/11840");
				getLogger().info("(If the above version is lower than the installed version, you are probably up-to-date)");
			}

			@Override
			public void upToDate() {
				getLogger().info("The plugin is up-to-date.");
			}
		});
	}

	public static API getApi() {
		return api;
	}

	public class API {

		public void preloadImage(final URL url, final RenderOptions options, final Callback<String> callback) {
			Bukkit.getScheduler().runTaskAsynchronously(WebFrames.this, new Runnable() {
				@Override
				public void run() {
					try {
						URL renderURL = new URL(String.format(RENDER_URL, url.toString(), options.toURLVar()));
						URLConnection connection = renderURL.openConnection();
						connection.setConnectTimeout(30000);
						connection.setRequestProperty("User-Agent", "WebFrames/" + getDescription().getVersion());
						final JsonObject json = readInputJSON(connection.getInputStream());
						if (json.has("error")) {
							throw new RenderError(json.getAsJsonObject("error"));
						}
						Bukkit.getScheduler().runTask(WebFrames.this, new Runnable() {
							@Override
							public void run() {
								callback.call(json.get("image").getAsString(), null);
							}
						});
					} catch (final Throwable e) {
						Bukkit.getScheduler().runTask(WebFrames.this, new Runnable() {
							@Override
							public void run() {
								callback.call(null, e);
							}
						});
					}
				}
			});
		}

		//		public boolean startFrameCreation(@Nonnull final Player player, @Nonnull final URL url) {
		//			if (!url.toString().contains("webrender.inventivetalent.org/renders/")) { throw new IllegalArgumentException("Invalid image URL"); }
		//			return AnimatedFrames.getApi().startFrameCreation(player, url);
		//		}
	}

	private JsonObject readInputJSON(InputStream stream) {
		return new JsonParser().parse(new InputStreamReader(stream)).getAsJsonObject();
	}
}
