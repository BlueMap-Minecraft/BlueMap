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
package de.bluecolored.bluemap.common.events;

import de.bluecolored.bluemap.api.events.EventDispatcher;
import de.bluecolored.bluemap.api.events.Events;
import de.bluecolored.bluemap.common.BlueMapConfiguration;
import de.bluecolored.bluemap.common.plugin.Plugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PluginLifecycleEvent {

    @Getter
    private final Plugin plugin;

    public static abstract class Load extends PluginLifecycleEvent {

        public Load(Plugin plugin) {
            super(plugin);
        }

        public static class Pre extends Load {

            public static final EventDispatcher<Pre> DISPATCHER = Events.getDispatcher(Pre.class);

            public Pre(Plugin plugin) {
                super(plugin);
            }

        }

        public static class Configurations extends Load {

            public static final EventDispatcher<Configurations> DISPATCHER = Events.getDispatcher(Configurations.class);

            @Getter
            private final BlueMapConfiguration configuration;

            public Configurations(Plugin plugin, BlueMapConfiguration configuration) {
                super(plugin);
                this.configuration = configuration;
            }

        }

        public static class Post extends Load {

            public static final EventDispatcher<Post> DISPATCHER = Events.getDispatcher(Post.class);

            public Post(Plugin plugin) {
                super(plugin);
            }

        }

    }

    public static class Save extends PluginLifecycleEvent {

        public static final EventDispatcher<Save> DISPATCHER = Events.getDispatcher(Save.class);

         public Save(Plugin plugin) {
             super(plugin);
         }

    }

    public static abstract class Unload extends PluginLifecycleEvent {

        public Unload(Plugin plugin) {
            super(plugin);
        }

        public static class Pre extends Unload {

            public static final EventDispatcher<Pre> DISPATCHER = Events.getDispatcher(Pre.class);

            public Pre(Plugin plugin) {
                super(plugin);
            }

        }

        public static class Post extends Unload {

            public static final EventDispatcher<Post> DISPATCHER = Events.getDispatcher(Post.class);

            public Post(Plugin plugin) {
                super(plugin);
            }

        }

    }

}
