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
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.core.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Optional;

public class BukkitCommandSource implements CommandSource {

    private final Plugin plugin;
    private final CommandSender delegate;

    public BukkitCommandSource(Plugin plugin, CommandSender delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Text text) {
        Bukkit.getScheduler().runTask(BukkitPlugin.getInstance(), () -> {
            if (delegate instanceof Player) {
                Player player = (Player) delegate;

                //kinda hacky but works
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + text.toJSONString());
                return;
            }

            delegate.sendMessage(text.toPlainString());
        });
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPermission(permission);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        Location location = getLocation();

        if (location != null) {
            return Optional.of(new Vector3d(location.getX(), location.getY(), location.getZ()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<World> getWorld() {
        Location location = getLocation();

        if (location != null) {
            try {
                var serverWorld = BukkitPlugin.getInstance().getWorld(location.getWorld());
                String worldId = plugin.getBlueMap().getWorldId(serverWorld.getSaveFolder());
                return Optional.ofNullable(plugin.getWorlds().get(worldId));
            } catch (IOException ignore) {}
        }

        return Optional.empty();
    }

    private Location getLocation() {
        Location location = null;
        if (delegate instanceof Entity) {
            location = ((Entity) delegate).getLocation();
        }
        if (delegate instanceof BlockCommandSender) {
            location = ((BlockCommandSender) delegate).getBlock().getLocation();
        }

        return location;
    }

}
