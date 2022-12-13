package de.bluecolored.bluemap.common.plugin.skins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.bluecolored.bluemap.api.plugin.SkinProvider;
import de.bluecolored.bluemap.core.logger.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class MojangSkinProvider implements SkinProvider {

    @Override
    public Optional<BufferedImage> load(UUID playerUUID) throws IOException {
        try (Reader reader = requestProfileJson(playerUUID)) {
            JsonParser parser = new JsonParser();
            String textureInfoJson = readTextureInfoJson(parser.parse(reader));
            String textureUrl = readTextureUrl(parser.parse(textureInfoJson));
            return Optional.of(ImageIO.read(new URL(textureUrl)));
        } catch (IOException ex) {
            Logger.global.logDebug("Failed to load skin from mojang for player: '" + playerUUID + "' - " + ex);
            return Optional.empty();
        }
    }

    private Reader requestProfileJson(UUID playerUUID) throws IOException {
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + playerUUID);
        return new InputStreamReader(url.openStream());
    }

    private String readTextureInfoJson(JsonElement json) throws IOException {
        try {
            JsonArray properties = json.getAsJsonObject().getAsJsonArray("properties");

            for (JsonElement element : properties) {
                if (element.getAsJsonObject().get("name").getAsString().equals("textures")) {
                    return new String(Base64.getDecoder().decode(element.getAsJsonObject().get("value").getAsString().getBytes()));
                }
            }

            throw new IOException("No texture info found!");
        } catch (NullPointerException | IllegalStateException | ClassCastException e) {
            throw new IOException(e);
        }

    }

    private String readTextureUrl(JsonElement json) throws IOException {
        try {
            return json.getAsJsonObject()
                    .getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url").getAsString();
        } catch (NullPointerException | IllegalStateException | ClassCastException e) {
            throw new IOException(e);
        }
    }

}
