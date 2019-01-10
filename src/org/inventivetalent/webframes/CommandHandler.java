package org.inventivetalent.webframes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.MalformedURLException;
import java.net.URL;

public class CommandHandler implements CommandExecutor {

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

				final String size = args.length > 1 ? args[1]:"640x360";

				sender.sendMessage("§aRendering website...");
				api.preloadImage(siteURL, size, new Callback<String>() {
					@Override
					public void call(String value, Throwable error) {
						try {
							if (error != null) { throw error; }

							sender.sendMessage("§aSuccessfully rendered website");

							WebFrames.originalUrls.put(value, siteURL.toString());
							WebFrames.sizes.put(value, siteURL.toString());
							player.chat("/framecreate WF-" + siteURL.toString().replace("http:", "").replace("https:", "").replace("\\", "").replace("/", "") + " " + value);
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
