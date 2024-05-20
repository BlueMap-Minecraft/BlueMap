package de.bluecolored.bluemap.common.events;

import de.bluecolored.bluemap.api.events.EventDispatcher;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;

public final class EventUtils {

    private EventUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> void dispatch(EventDispatcher<T> dispatcher, T event) {
        try {
            dispatcher.dispatch(event);
        } catch (Exception e) {
            Logger.global.logError("A BlueMap addon threw an exception trying to process event: '" + event.getClass() + "'", e);
        }
    }

    public static <T> void dispatchAsync(EventDispatcher<T> dispatcher, T event) {
        BlueMap.THREAD_POOL.execute(() -> dispatch(dispatcher, event));
    }

}
