package org.inventivetalent.webframes;

import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.net.URL;

public interface WebFramesAPI {

	void preloadImage(@Nonnull final URL url, @Nonnull final RenderOptions options, @Nonnull final Callback<URL> callback);

	boolean startFrameCreation(@Nonnull final Player player, @Nonnull final URL url);

}
