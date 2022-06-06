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
package de.bluecolored.bluemap.bukkit;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class BukkitPlayer implements Player {

    private static final Map<GameMode, Gamemode> GAMEMODE_MAP = new EnumMap<>(GameMode.class);
    static {
        GAMEMODE_MAP.put(GameMode.ADVENTURE, Gamemode.ADVENTURE);
        GAMEMODE_MAP.put(GameMode.SURVIVAL, Gamemode.SURVIVAL);
        GAMEMODE_MAP.put(GameMode.CREATIVE, Gamemode.CREATIVE);
        GAMEMODE_MAP.put(GameMode.SPECTATOR, Gamemode.SPECTATOR);
    }

    private final UUID uuid;
    private Text name;
    private String world;
    private Vector3d position;
    private boolean online;
    private boolean sneaking;
    private boolean invisible;
    private boolean vanished;
    private Gamemode gamemode;

    public BukkitPlayer(UUID playerUUID) {
        this.uuid = playerUUID;
        update();
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public Text getName() {
        return this.name;
    }

    @Override
    public String getWorld() {
        return this.world;
    }

    @Override
    public Vector3d getPosition() {
        return this.position;
    }

    @Override
    public boolean isOnline() {
        return this.online;
    }

    @Override
    public boolean isSneaking() {
        return this.sneaking;
    }

    @Override
    public boolean isInvisible() {
        return this.invisible;
    }

    @Override
    public boolean isVanished() {
        return vanished;
    }

    @Override
    public Gamemode getGamemode() {
        return this.gamemode;
    }

    /**
     * API access, only call on server thread!
     */
    public void update() {
        org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            this.online = false;
            return;
        }

        this.gamemode = GAMEMODE_MAP.get(player.getGameMode());
        if (this.gamemode == null) this.gamemode = Gamemode.SURVIVAL;

        this.invisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);

        //also check for "vanished" players
        boolean vanished = false;
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) vanished = true;
        }
        this.vanished = vanished;

        this.name = Text.of(player.getName());
        this.online = player.isOnline();

        Location location = player.getLocation();
        this.position = new Vector3d(location.getX(), location.getY(), location.getZ());
        this.sneaking = player.isSneaking();

        try {
            var world = BukkitPlugin.getInstance().getWorld(player.getWorld());
            this.world = BukkitPlugin.getInstance().getPlugin().getBlueMap().getWorldId(world.getSaveFolder());
        } catch (IOException e) {
            this.world = "unknown";
        }
    }

}
