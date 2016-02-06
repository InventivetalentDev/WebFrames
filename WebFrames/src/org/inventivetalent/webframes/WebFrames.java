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

import de.inventivegames.animatedframes.AnimatedFrames;
import de.inventivegames.animatedframes.api.LoadListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
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
						connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
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
