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

import com.google.gson.JsonObject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String s, String[] args) {
		if ("webframecreate".equalsIgnoreCase(command.getName())) {
			if (!sender.hasPermission("webframe.create")) {
				sender.sendMessage("§cNo permission");
				return false;
			}

			if (!(sender instanceof Player)) {
				sender.sendMessage("§cYou are not a player!");
				return false;
			}
			final Player player = (Player) sender;

			if (args.length == 0) {
				sender.sendMessage("§c/wfcreate <URL> [options]");
				return false;
			}
			try {
				final URL siteURL = new URL(args[0]);
				final WebFrames.API api = WebFrames.getApi();

				final RenderOptions options = new RenderOptions();

				try {
					options.parseArguments(Arrays.copyOfRange(args, 1, args.length));
				} catch (IllegalArgumentException ie) {
					sender.sendMessage("§c" + ie.getMessage());
					sender.sendMessage("§cAvailable options: ");
					sender.sendMessage("§c-w <width> | -h <height> | -q <quality> | -z <zoom> | -d <javascript delay>");
					return false;
				}

				sender.sendMessage("§aRendering website...");
				api.preloadImage(siteURL, options, new Callback<URL>() {
					@Override
					public void call(URL value, @Nullable Throwable error) {
						try {
							if (error != null) { throw error; }

							sender.sendMessage("§aSuccessfully rendered website");

							JsonObject metaJson = new JsonObject();
							metaJson.addProperty("siteURL", siteURL.toString());
							metaJson.add("renderOptions", options.toJSON());
							//							AnimatedFrames.getApi().injectMeta(player, metaJson);
							//							api.startFrameCreation(player, value);
							player.chat("/framecreate WF-" + siteURL.toString() +" "+ value.toString());
						} catch (RenderError error1) {
							handleRenderError(player, error1);
						} catch (Throwable ex) {
							handleException(player, ex);
						}
					}
				});
			} catch (RenderError error) {
				handleRenderError(player, error);
				return false;
			} catch (MalformedURLException ex) {
				sender.sendMessage("§cInvalid URL");
				return false;
			} catch (Throwable ex) {
				handleException(player, ex);
				return false;
			}
			return true;
		}

		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
		List<String> list = new ArrayList<>();

		if (args.length >= 2) {
			if (sender.hasPermission("webframe.create")) {
				for (RenderOptions.Option option : RenderOptions.Option.values()) {
					list.add("-" + option.key);
				}
			}
		}

		return list;
	}

	void handleRenderError(Player player, RenderError error) {
		player.sendMessage("§cAn error occurred while rendering the website:");
		player.sendMessage("§cError #" + error.getId() + ": " + error.getMessage());
		error.printStackTrace();
	}

	void handleException(Player player, Throwable throwable) {
		player.sendMessage("§cAn unexpected exception occurred: " + throwable.getMessage());
		player.sendMessage("§cSee console for details.");
		throwable.printStackTrace();
	}

}
