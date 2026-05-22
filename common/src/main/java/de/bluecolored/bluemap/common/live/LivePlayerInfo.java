package de.bluecolored.bluemap.common.live;

import com.flowpowered.math.vector.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
public class LivePlayerInfo {

    @NonNull private UUID uuid;
    @NonNull private String name;
    @NonNull private Vector3d position;
    @NonNull private Vector3d rotation;

}
