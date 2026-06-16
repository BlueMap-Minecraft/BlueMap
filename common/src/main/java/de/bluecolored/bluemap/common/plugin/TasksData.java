package de.bluecolored.bluemap.common.plugin;

import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TasksData {

    private List<RenderTask> renderTasks = new ArrayList<>();

}
