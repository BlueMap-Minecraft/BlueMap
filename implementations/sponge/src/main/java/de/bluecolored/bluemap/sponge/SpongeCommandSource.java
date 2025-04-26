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
import de.bluecolored.bluemap.common.commands.TextFormat;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.Locatable;

import java.util.Optional;

public class SpongeCommandSource implements CommandSource {

    private final Audience audience;
    private final Subject subject;

    public SpongeCommandSource(Audience audience, Subject subject) {
        this.subject = subject;
        this.audience = audience;
    }

    @Override
    public void sendMessage(Component text) {
        if (TextFormat.lineCount(text) > 1)
            text = Component.newline().append(text).appendNewline();

        audience.sendMessage(text.compact());
    }

    @Override
    public boolean hasPermission(String permission) {
        return subject.hasPermission(permission);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        if (audience instanceof Locatable locatable)
            return Optional.of(SpongePlugin.fromSpongeVector(locatable.location().position()));
        return Optional.empty();
    }

    @Override
    public Optional<ServerWorld> getWorld() {
        if (audience instanceof Locatable locatable) {
            org.spongepowered.api.world.server.ServerWorld world = locatable.serverLocation().world();
            if (world == null) return Optional.empty();
            return Optional.of(SpongePlugin.getInstance().getServerWorld(world));
        }
        return Optional.empty();
    }

}
