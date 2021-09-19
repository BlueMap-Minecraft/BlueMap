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
package de.bluecolored.bluemap.sponge8;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.core.world.World;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.Locatable;

import java.util.Optional;

public class SpongeCommandSource implements CommandSource {

    private final Plugin plugin;
    private final Audience audience;
    private final Subject subject;

    public SpongeCommandSource(Plugin plugin, Audience audience, Subject subject) {
        this.plugin = plugin;
        this.subject = subject;
        this.audience = audience;
    }

    @Override
    public void sendMessage(Text text) {
        audience.sendMessage(GsonComponentSerializer.gson().deserialize(text.toJSONString()));
    }

    @Override
    public boolean hasPermission(String permission) {
        return subject.hasPermission(permission);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        if (audience instanceof Locatable) {
            return Optional.of(SpongePlugin.fromSpongePoweredVector(((Locatable) audience).location().position()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<World> getWorld() {
        if (audience instanceof Locatable) {
            return Optional.ofNullable(plugin.getWorld(((Locatable) audience).serverLocation().world().uniqueId()));
        }

        return Optional.empty();
    }

}
