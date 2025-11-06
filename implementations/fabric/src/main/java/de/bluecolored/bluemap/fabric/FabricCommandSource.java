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
package de.bluecolored.bluemap.fabric;

import com.flowpowered.math.vector.Vector3d;
import com.google.gson.JsonElement;
import de.bluecolored.bluemap.common.commands.TextFormat;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class FabricCommandSource implements CommandSource {

    private final FabricMod mod;
    private final ServerCommandSource delegate;

    public FabricCommandSource(FabricMod mod, ServerCommandSource delegate) {
        this.mod = mod;
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Component text) {
        if (TextFormat.lineCount(text) > 1)
            text = Component.newline().append(text).appendNewline();

        JsonElement textJson = GsonComponentSerializer.gson().serializeToTree(text.compact());
        Text minecraftText = net.minecraft.text.Text.Serializer.fromJson(textJson);
        delegate.sendMessage(minecraftText);
    }

    @Override
    public boolean hasPermission(String permission) {
        try {
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            return Permissions.check(delegate, permission, 1);
        } catch (ClassNotFoundException ex) {
            return delegate.hasPermissionLevel(1);
        }
    }

    @Override
    public Optional<Vector3d> getPosition() {
        if (!delegate.isExecutedByPlayer() && delegate.getName().equals("Server")) return Optional.empty();

        Vec3d pos = delegate.getPosition();
        return Optional.of(new Vector3d(pos.x, pos.y, pos.z));
    }

    @Override
    public Optional<ServerWorld> getWorld() {
        if (!delegate.isExecutedByPlayer() && delegate.getName().equals("Server")) return Optional.empty();

        return Optional.of(delegate.getWorld())
                .map(mod::getServerWorld);
    }

}
