package de.bluecolored.bluemap.core.map.renderstate;

import java.util.Objects;

import static de.bluecolored.bluemap.core.map.renderstate.TileState.*;

@FunctionalInterface
public interface TileActionResolver {

    ActionAndNextState findActionAndNextState(
            boolean chunksChanged,
            BoundsSituation bounds
    );

    enum BoundsSituation {
        INSIDE,
        EDGE,
        OUTSIDE
    }

    enum Action {
        NONE,
        RENDER,
        DELETE
    }

    record ActionAndNextState (Action action, TileState state) {

        public ActionAndNextState(Action action, TileState state) {
            this.action = Objects.requireNonNull(action);
            this.state = Objects.requireNonNull(state);
        }

        public static final ActionAndNextState RENDER_RENDERED = new ActionAndNextState(Action.RENDER, RENDERED);
        public static final ActionAndNextState NONE_RENDERED = new ActionAndNextState(Action.NONE, RENDERED);
        public static final ActionAndNextState RENDER_RENDERED_EDGE = new ActionAndNextState(Action.RENDER, RENDERED_EDGE);
        public static final ActionAndNextState NONE_RENDERED_EDGE = new ActionAndNextState(Action.NONE, RENDERED_EDGE);
        public static final ActionAndNextState DELETE_OUT_OF_BOUNDS = new ActionAndNextState(Action.DELETE, OUT_OF_BOUNDS);
        public static final ActionAndNextState NONE_OUT_OF_BOUNDS = new ActionAndNextState(Action.NONE, OUT_OF_BOUNDS);
    }

}
