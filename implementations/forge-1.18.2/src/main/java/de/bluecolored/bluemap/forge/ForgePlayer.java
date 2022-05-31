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
import de.bluecolored.bluemap.common.plugin.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.text.Text;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ForgePlayer implements Player {

    private static final UUID UNKNOWN_WORLD_UUID = UUID.randomUUID();

    private static final Map<GameType, Gamemode> GAMEMODE_MAP = new EnumMap<>(GameType.class);
    static {
        GAMEMODE_MAP.put(GameType.ADVENTURE, Gamemode.ADVENTURE);
        GAMEMODE_MAP.put(GameType.SURVIVAL, Gamemode.SURVIVAL);
        GAMEMODE_MAP.put(GameType.CREATIVE, Gamemode.CREATIVE);
        GAMEMODE_MAP.put(GameType.SPECTATOR, Gamemode.SPECTATOR);
    }

    private UUID uuid;
    private Text name;
    private UUID world;
    private Vector3d position;
    private boolean online;
    private boolean sneaking;
    private boolean invisible;
    private Gamemode gamemode;

    private ForgeMod mod;

    public ForgePlayer(ForgeMod mod, UUID playerUuid) {
        this.uuid = playerUuid;
        this.mod = mod;

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
    public UUID getWorld() {
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

        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) {
            this.online = false;
            return;
        }

        this.gamemode = GAMEMODE_MAP.getOrDefault(player.gameMode.getGameModeForPlayer(), Gamemode.SURVIVAL);
        if (this.gamemode == null) this.gamemode = Gamemode.SURVIVAL;

        MobEffectInstance invis = player.getEffect(MobEffects.INVISIBILITY);
        this.invisible = invis != null && invis.getDuration() > 0;

        this.name = Text.of(player.getName().getString());
        this.online = true;

        Vec3 pos = player.getPosition(1f);
        this.position = new Vector3d(pos.x(), pos.y(), pos.z());
        this.sneaking = player.isCrouching();

        try {
            this.world = mod.getUUIDForWorld((ServerLevel) player.getCommandSenderWorld());
        } catch (IOException | ClassCastException e) {
            this.world = UNKNOWN_WORLD_UUID;
        }
    }

}
