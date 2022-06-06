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
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.core.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import java.io.IOException;
import java.util.Optional;

public class ForgeCommandSource implements CommandSource {

    private final ForgeMod mod;
    private final Plugin plugin;
    private final net.minecraft.command.CommandSource delegate;

    public ForgeCommandSource(ForgeMod mod, Plugin plugin, net.minecraft.command.CommandSource delegate) {
        this.mod = mod;
        this.plugin = plugin;
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Text text) {
        var component = ITextComponent.Serializer.fromJson(text.toJSONString());
        if (component != null)
            delegate.sendFeedback(component, false);
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPermissionLevel(1);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        Vec3d pos = delegate.getPos();
        if (pos != null) {
            return Optional.of(new Vector3d(pos.x, pos.y, pos.z));
        }

        return Optional.empty();
    }

    @Override
    public Optional<World> getWorld() {
        try {
            var serverWorld = mod.getWorld(delegate.getWorld());
            String worldId = plugin.getBlueMap().getWorldId(serverWorld.getSaveFolder());
            return Optional.ofNullable(plugin.getWorlds().get(worldId));
        } catch (IOException ignore) {}

        return Optional.empty();
    }

}
