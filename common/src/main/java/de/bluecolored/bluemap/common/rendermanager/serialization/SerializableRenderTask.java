package de.bluecolored.bluemap.common.rendermanager.serialization;

import de.bluecolored.bluemap.common.rendermanager.RenderTask;

public interface SerializableRenderTask<T extends SerializableRenderTask<T, D>, D extends SerializableRenderTask.Serialized<T>> extends RenderTask {

    D serialize();

    interface Serialized<T> {

        T deserialize();

    }

}


