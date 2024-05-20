package de.bluecolored.bluemap.common.events;

import de.bluecolored.bluemap.api.events.EventDispatcher;
import de.bluecolored.bluemap.api.events.Events;
import de.bluecolored.bluemap.common.web.RoutingRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WebserverStartEvent {

    public static final EventDispatcher<WebserverStartEvent> DISPATCHER = Events.getDispatcher(WebserverStartEvent.class);

    private final HttpServer webserver;
    private final RoutingRequestHandler requestHandler;

}
