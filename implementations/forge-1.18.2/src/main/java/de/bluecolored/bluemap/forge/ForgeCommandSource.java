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
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.core.world.World;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.Optional;

public class ForgeCommandSource implements CommandSource {

    private ForgeMod mod;
    private Plugin plugin;
    private CommandSourceStack delegate;

    public ForgeCommandSource(ForgeMod mod, Plugin plugin, CommandSourceStack delegate) {
        this.mod = mod;
        this.plugin = plugin;
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Text text) {
        Component component = TextComponent.Serializer.fromJsonLenient(text.toJSONString());
        if (component == null) component = new TextComponent(text.toPlainString());
        delegate.sendSuccess(component, false);
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPermission(1);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        Vec3 pos = delegate.getPosition();
        return Optional.of(new Vector3d(pos.x, pos.y, pos.z));
    }

    @Override
    public Optional<World> getWorld() {
        try {
            ServerLevel world = delegate.getLevel();
            return Optional.ofNullable(plugin.getWorld(mod.getUUIDForWorld(world)));
        } catch (IOException ignore) {}

        return Optional.empty();
    }

}
