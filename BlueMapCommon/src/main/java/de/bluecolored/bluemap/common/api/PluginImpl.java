package de.bluecolored.bluemap.common.api;


import de.bluecolored.bluemap.api.plugin.PlayerIconFactory;
import de.bluecolored.bluemap.api.plugin.SkinProvider;
import de.bluecolored.bluemap.common.plugin.Plugin;

public class PluginImpl implements de.bluecolored.bluemap.api.plugin.Plugin {

    private final Plugin plugin;

    public PluginImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SkinProvider getSkinProvider() {
        return plugin.getSkinUpdater().getSkinProvider();
    }

    @Override
    public void setSkinProvider(SkinProvider skinProvider) {
        plugin.getSkinUpdater().setSkinProvider(skinProvider);
    }

    @Override
    public PlayerIconFactory getPlayerMarkerIconFactory() {
        return plugin.getSkinUpdater().getPlayerMarkerIconFactory();
    }

    @Override
    public void setPlayerMarkerIconFactory(PlayerIconFactory playerMarkerIconFactory) {
        plugin.getSkinUpdater().setPlayerMarkerIconFactory(playerMarkerIconFactory);
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
