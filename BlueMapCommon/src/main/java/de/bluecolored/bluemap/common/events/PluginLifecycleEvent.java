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
