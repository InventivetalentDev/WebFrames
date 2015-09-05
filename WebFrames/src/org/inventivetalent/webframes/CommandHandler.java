package org.inventivetalent.webframes;

import de.inventivegames.animatedframes.AnimatedFrames;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.json.JSONObject;

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
				final WebFramesAPI api = WebFrames.getApi();

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

							JSONObject metaJson = new JSONObject();
							metaJson.put("siteURL", siteURL);
							metaJson.put("renderOptions", options.toJSON());
							AnimatedFrames.getApi().injectMeta(player, metaJson);
							api.startFrameCreation(player, value);
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
