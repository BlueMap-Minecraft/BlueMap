package de.bluecolored.bluemap.common.live;

import de.bluecolored.bluemap.common.serverinterface.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface LivePlayerInfoTransformer extends Function<Player, @Nullable LivePlayerInfo> {}
