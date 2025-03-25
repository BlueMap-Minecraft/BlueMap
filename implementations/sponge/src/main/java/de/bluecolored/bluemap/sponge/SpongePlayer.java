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
package de.bluecolored.bluemap.sponge;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.LightTypes;

import java.util.*;

public class SpongePlayer extends Player {

    private static final Map<GameMode, Gamemode> GAMEMODE_MAP = new HashMap<>(5);
    static {
        GAMEMODE_MAP.put(GameModes.ADVENTURE.get(), Gamemode.ADVENTURE);
        GAMEMODE_MAP.put(GameModes.SURVIVAL.get(), Gamemode.SURVIVAL);
        GAMEMODE_MAP.put(GameModes.CREATIVE.get(), Gamemode.CREATIVE);
        GAMEMODE_MAP.put(GameModes.SPECTATOR.get(), Gamemode.SPECTATOR);
    }

    private final UUID uuid;
    private Text name;
    private ServerWorld world;
    private Vector3d position;
    private Vector3d rotation;
    private int skyLight;
    private int blockLight;
    private boolean sneaking;
    private boolean invisible;
    private boolean vanished;
    private Gamemode gamemode;

    public SpongePlayer(UUID playerUUID) {
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
    public ServerWorld getWorld() {
        return this.world;
    }

    @Override
    public Vector3d getPosition() {
        return this.position;
    }

    @Override
    public Vector3d getRotation() {
        return rotation;
    }

    @Override
    public int getSkyLight() {
        return skyLight;
    }

    @Override
    public int getBlockLight() {
        return blockLight;
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
        ServerPlayer player = Sponge.server().player(uuid).orElse(null);
        if (player == null) return;

        this.gamemode = GAMEMODE_MAP.get(player.gameMode().get());
        if (this.gamemode == null) this.gamemode = Gamemode.SURVIVAL;

        boolean invis = false;
        Optional<List<PotionEffect>> effects = player.get(Keys.POTION_EFFECTS);
        if (effects.isPresent()) {
            for (PotionEffect effect : effects.get()) {
                if (effect.type().equals(PotionEffectTypes.INVISIBILITY.get()) && effect.duration().ticks() > 0) invis = true;
            }
        }
        this.invisible = invis;

        this.vanished = player.get(Keys.VANISH_STATE).orElse(VanishState.unvanished()).invisible();

        this.name = Text.of(player.name());
        this.position = SpongePlugin.fromSpongeVector(player.position());
        this.rotation = SpongePlugin.fromSpongeVector(player.rotation());
        this.sneaking = player.get(Keys.IS_SNEAKING).orElse(false);

        this.skyLight = player.world().light(LightTypes.SKY, player.blockPosition());
        this.blockLight = player.world().light(LightTypes.BLOCK, player.blockPosition());

        this.world = SpongePlugin.getInstance().getServerWorld(player.world());
    }

}
