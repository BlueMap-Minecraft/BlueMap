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
package de.bluecolored.bluemap.forge;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.text.Text;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ForgePlayer implements Player {

    private static final Map<GameType, Gamemode> GAMEMODE_MAP = new EnumMap<>(GameType.class);
    static {
        GAMEMODE_MAP.put(GameType.ADVENTURE, Gamemode.ADVENTURE);
        GAMEMODE_MAP.put(GameType.SURVIVAL, Gamemode.SURVIVAL);
        GAMEMODE_MAP.put(GameType.CREATIVE, Gamemode.CREATIVE);
        GAMEMODE_MAP.put(GameType.SPECTATOR, Gamemode.SPECTATOR);
        GAMEMODE_MAP.put(GameType.NOT_SET, Gamemode.SURVIVAL);
    }

    private final UUID uuid;
    private Text name;
    private String world;
    private Vector3d position;
    private boolean online;
    private boolean sneaking;
    private boolean invisible;
    private Gamemode gamemode;

    private final ForgeMod mod;
    private final BlueMapService blueMap;

    public ForgePlayer(UUID playerUuid, ForgeMod mod, BlueMapService blueMap) {
        this.uuid = playerUuid;
        this.mod = mod;
        this.blueMap = blueMap;

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
    public Gamemode getGamemode() {
        return this.gamemode;
    }

    /**
     * Only call on server thread!
     */
    public void update() {
        MinecraftServer server = mod.getServer();
        if (server == null) {
            this.online = false;
            return;
        }

        ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(uuid);
        if (player == null) {
            this.online = false;
            return;
        }

        this.gamemode = GAMEMODE_MAP.get(player.interactionManager.getGameType());
        if (this.gamemode == null) this.gamemode = Gamemode.SURVIVAL;

        EffectInstance invis = player.getActivePotionEffect(Effects.INVISIBILITY);
        this.invisible = invis != null && invis.getDuration() > 0;

        this.name = Text.of(player.getName().getString());
        this.online = true;

        Vec3d pos = player.getPositionVec();
        this.position = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
        this.sneaking = player.isCrouching();

        try {
            var world = mod.getWorld(player.getServerWorld());
            this.world = blueMap.getWorldId(world.getSaveFolder());
        } catch (IOException e) {
            this.world = "unknown";
        }
    }

}
