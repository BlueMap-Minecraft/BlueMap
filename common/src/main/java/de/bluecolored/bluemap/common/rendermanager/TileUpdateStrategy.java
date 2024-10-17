package de.bluecolored.bluemap.common.rendermanager;

import de.bluecolored.bluemap.core.map.renderstate.TileState;

import java.util.function.Predicate;

public interface TileUpdateStrategy extends Predicate<TileState> {

    TileUpdateStrategy FORCE_ALL = tileState -> true;
    TileUpdateStrategy FORCE_EDGE = tileState -> tileState == TileState.RENDERED_EDGE;
    TileUpdateStrategy FORCE_NONE = tileState -> false;

    static TileUpdateStrategy fixed(boolean force) {
        return force ? FORCE_ALL : FORCE_NONE;
    }

}
