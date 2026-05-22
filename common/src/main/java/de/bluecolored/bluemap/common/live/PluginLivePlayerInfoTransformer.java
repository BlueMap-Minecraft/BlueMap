package de.bluecolored.bluemap.common.live;

import de.bluecolored.bluemap.api.plugin.PlayerDisplayNameProvider;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.PluginState;
import de.bluecolored.bluemap.common.serverinterface.Player;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class PluginLivePlayerInfoTransformer implements LivePlayerInfoTransformer {

    private final Plugin plugin;

    @Getter @Setter @NonNull
    private PlayerDisplayNameProvider playerDisplayNameProvider = new DefaultDisplayNameProvider();

    public PluginLivePlayerInfoTransformer(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public LivePlayerInfo apply(Player player) {
        PluginState pluginState = plugin.getPluginState();
        if (pluginState.isPlayerHidden(player.getUuid())) return null;

        BlueMapService blueMapService = plugin.getBlueMap();
        if (blueMapService == null) return null;

        PluginConfig config = blueMapService.getConfig().getPluginConfig();
        if (config.isHideInvisible() && player.isInvisible()) return null;
        if (config.isHideVanished() && player.isVanished()) return null;
        if (config.isHideSneaking() && player.isSneaking()) return null;
        if (config.getHiddenGameModes().contains(player.getGamemode().getId())) return null;
        if (player.getSkyLight() < config.getHideBelowSkyLight() &&
                player.getBlockLight() < config.getHideBelowBlockLight()) return null;

        return new LivePlayerInfo(
                player.getUuid(),
                playerDisplayNameProvider.get(player.getUuid()),
                player.getPosition(),
                player.getRotation()
        );
    }

    private class DefaultDisplayNameProvider implements PlayerDisplayNameProvider {

        @Override
        public String get(UUID playerUUID) {
            Map<UUID, Player> onlinePlayerMap = plugin.getServerInterface().getOnlinePlayers();
            @Nullable Player player = onlinePlayerMap.get(playerUUID);
            if (player != null) return player.getName().toPlainString();
            return playerUUID.toString();
        }

    }

}
