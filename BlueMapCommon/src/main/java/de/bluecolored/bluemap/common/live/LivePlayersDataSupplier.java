package de.bluecolored.bluemap.common.live;

import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Supplier;

public class LivePlayersDataSupplier implements Supplier<String> {

    private final ServerInterface server;
    private final PluginConfig config;
    private final String worldId;

    public LivePlayersDataSupplier(ServerInterface server, PluginConfig config, String worldId) {
        this.server = server;
        this.config = config;
        this.worldId = worldId;
    }

    @Override
    public String get() {
        try (StringWriter jsonString = new StringWriter();
            JsonWriter json = new JsonWriter(jsonString)) {

            json.beginObject();
            json.name("players").beginArray();

            if (config.isLivePlayerMarkers()) {
                for (Player player : this.server.getOnlinePlayers()) {
                    if (!player.isOnline()) continue;

                    boolean isCorrectWorld = player.getWorld().equals(this.worldId);

                    if (config.isHideInvisible() && player.isInvisible()) continue;
                    if (config.isHideVanished() && player.isVanished()) continue;
                    if (config.isHideSneaking() && player.isSneaking()) continue;
                    if (config.getHiddenGameModes().contains(player.getGamemode().getId())) continue;
                    if (config.isHideDifferentWorld() && !isCorrectWorld) continue;

                    json.beginObject();
                    json.name("uuid").value(player.getUuid().toString());
                    json.name("name").value(player.getName().toPlainString());
                    json.name("foreign").value(!isCorrectWorld);

                    json.name("position").beginObject();
                    json.name("x").value(player.getPosition().getX());
                    json.name("y").value(player.getPosition().getY());
                    json.name("z").value(player.getPosition().getZ());
                    json.endObject();

                    json.name("rotation").beginObject();
                    json.name("pitch").value(player.getRotation().getX());
                    json.name("yaw").value(player.getRotation().getY());
                    json.name("roll").value(player.getRotation().getZ());
                    json.endObject();

                    json.endObject();
                }
            }

            json.endArray();
            json.endObject();

            json.flush();
            return jsonString.toString();
        } catch (IOException ex) {
            Logger.global.logError("Failed to write live/players json!", ex);
            return "BlueMap - Exception handling this request";
        }
    }

}
