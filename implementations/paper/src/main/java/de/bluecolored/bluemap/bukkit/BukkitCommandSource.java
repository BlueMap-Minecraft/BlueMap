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
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class BukkitCommandSource implements CommandSource {

    private final CommandSourceStack delegate;

    public BukkitCommandSource(CommandSourceStack delegate) {
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Component text) {
        delegate.getSender().sendMessage(text.compact());
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.getSender().hasPermission(permission);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        if (delegate.getSender() instanceof ConsoleCommandSender) return Optional.empty();

        Location location = delegate.getLocation();
        return Optional.of(new Vector3d(location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public Optional<ServerWorld> getWorld() {
        if (delegate.getSender() instanceof ConsoleCommandSender) return Optional.empty();

        ServerWorld serverWorld = BukkitPlugin.getInstance().getServerWorld(delegate.getLocation().getWorld());
        return Optional.ofNullable(serverWorld);
    }

}
