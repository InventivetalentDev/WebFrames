/*
 * Copyright 2015-2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

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
import org.mcstats.MetricsLite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class WebFrames extends JavaPlugin {

	public static WebFrames instance;

	static final String RENDER_URL = "https://webrender.inventivetalent.org/?url=%s%s";

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

		instance = this;

		Bukkit.getPluginManager().registerEvents(new Listener() {

			@EventHandler
			public void on(final AsyncFrameCreationEvent event) throws MalformedURLException {
				if (event.getSource().toLowerCase().contains("webrender.inventivetalent.org")) {
					final String site = originalUrls.get(event.getSource());
					event.getMeta().addProperty("siteURL", site);
				}
			}

			@EventHandler
			public void on(final AsyncFrameLoadEvent event) {
				if (event.getFrame().getImageSource().contains("webrender.inventivetalent.org/renders/")) {
					JsonObject meta = event.getFrame().getMeta();
					if (meta == null || !meta.has("siteURL")) { return; }
					try {
						URL siteURL = new URL(meta.get("siteURL").getAsString());
						RenderOptions renderOptions = new RenderOptions();
						renderOptions.loadJSON(meta.getAsJsonObject("renderOptions"));

						getApi().preloadImage(siteURL, renderOptions, new Callback<String>() {
							@Override
							public void call(String value, @Nullable Throwable error) {
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
				if (event.getSource().contains("webrender.inventivetalent.org")) {
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

		try {
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("Metrics started");
			}

			spigetUpdate = new SpigetUpdate(this, 11840).setUserAgent("WebFrames/" + getDescription().getVersion());
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
		} catch (Exception e) {
		}
	}

	public static API getApi() {
		return api;
	}

	public class API {

		public void preloadImage(@Nonnull final URL url, @Nonnull final RenderOptions options, @Nonnull final Callback<String> callback) {
			try {
				URL renderURL = new URL(String.format(RENDER_URL, url.toString(), options.toURLVar()));
				URLConnection connection = renderURL.openConnection();
				connection.setConnectTimeout(30000);
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
				JsonObject json = readInputJSON(connection.getInputStream());
				if (json.has("error")) {
					throw new RenderError(json.getAsJsonObject("error"));
				}
				callback.call(json.get("image").getAsString(), null);
			} catch (Throwable e) {
				callback.call(null, e);
			}
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
