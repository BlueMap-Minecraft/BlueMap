/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
