package de.bluecolored.bluemap.sponge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.storage.WorldProperties;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.World;

public class Commands {
	
	private SpongePlugin plugin;
	
	public Commands(SpongePlugin plugin) {
		this.plugin = plugin;
	}
	
	public CommandSpec createRootCommand() {
		
		@SuppressWarnings("unused")
		CommandSpec debugCommand = CommandSpec.builder()
				.executor((source, args) -> {
					if (source instanceof Locatable) {
						Location<org.spongepowered.api.world.World> loc = ((Locatable) source).getLocation();
						UUID worldUuid = loc.getExtent().getUniqueId();
						World world = plugin.getWorld(worldUuid);
						Block block = world.getBlock(loc.getBlockPosition());
						Block blockBelow = world.getBlock(loc.getBlockPosition().add(0, -1, 0));
						
						source.sendMessages(Lists.newArrayList(
								Text.of("Block: " + block),
								Text.of("Block below: " + blockBelow)
							));
					}
					
					return CommandResult.success();
				})
				.build();
		
		return CommandSpec.builder()
			.description(Text.of("Displays BlueMaps render status"))
			.permission("bluemap.status")
			.childArgumentParseExceptionFallback(false)
			.child(createReloadCommand(), "reload")
			.child(createPauseRenderCommand(), "pause")
			.child(createResumeRenderCommand(), "resume")
			.child(createRenderCommand(), "render")
			//.child(debugCommand, "debug")
			.executor((source, args) -> {
				source.sendMessages(createStatusMessage());
				return CommandResult.success();
			})
			.build();
	}
	
	public CommandSpec createStandaloneReloadCommand() {
		return CommandSpec.builder()
				.description(Text.of("BlueMaps root command"))
				.childArgumentParseExceptionFallback(false)
				.child(createReloadCommand(), "reload")
				.build();
	}
	
	public CommandSpec createReloadCommand() {
		return CommandSpec.builder()
			.description(Text.of("Reloads all resources and configuration-files"))
			.permission("bluemap.reload")
			.executor((source, args) -> {
					source.sendMessage(Text.of(TextColors.GOLD, "Reloading BlueMap..."));
					
					plugin.getAsyncExecutor().submit(() -> {
						try {
							plugin.reload();
							
							if (plugin.isLoaded()) {
								source.sendMessage(Text.of(TextColors.GREEN, "BlueMap reloaded!"));
							} else {
								source.sendMessage(Text.of(TextColors.RED, "Could not load BlueMap! See the console for details!"));
							}
	
						} catch (Exception ex) {
							Logger.global.logError("Failed to reload BlueMap!", ex);
							
							source.sendMessage(Text.of(TextColors.RED, "There was an error reloading BlueMap! See the console for details!"));
						}
					});

					return CommandResult.success();
			})
			.build();
	}

	public CommandSpec createPauseRenderCommand() {
		return CommandSpec.builder()
			.description(Text.of("Pauses all rendering"))
			.permission("bluemap.pause")
			.executor((source, args) -> {
				if (plugin.getRenderManager().isRunning()) {
					plugin.getRenderManager().stop();
					source.sendMessage(Text.of(TextColors.GREEN, "BlueMap rendering paused!"));
					return CommandResult.success();
				} else {
					source.sendMessage(Text.of(TextColors.RED, "BlueMap rendering are already paused!"));
					return CommandResult.empty();
				}
			})
			.build();
	}
	
	public CommandSpec createResumeRenderCommand() {
		return CommandSpec.builder()
			.description(Text.of("Resumes all paused rendering"))
			.permission("bluemap.resume")
			.executor((source, args) -> {
				if (!plugin.getRenderManager().isRunning()) {
					plugin.getRenderManager().start();
					source.sendMessage(Text.of(TextColors.GREEN, "BlueMap renders resumed!"));
					return CommandResult.success();
				} else {
					source.sendMessage(Text.of(TextColors.RED, "BlueMap renders are already running!"));
					return CommandResult.empty();
				}
			})
			.build();
	}
	
	public CommandSpec createRenderCommand() {
		return CommandSpec.builder()
			.description(Text.of("Renders the whole world"))
			.permission("bluemap.rendertask.create.world")
			.childArgumentParseExceptionFallback(false)
			.child(createPrioritizeTaskCommand(), "prioritize")
			.child(createRemoveTaskCommand(), "remove")
			.arguments(GenericArguments.optional(GenericArguments.world(Text.of("world"))))
			.executor((source, args) -> {
				WorldProperties spongeWorld = args.<WorldProperties>getOne("world").orElse(null);
				
				if (spongeWorld == null && source instanceof Locatable) {
					Location<org.spongepowered.api.world.World> loc = ((Locatable) source).getLocation();
					spongeWorld = loc.getExtent().getProperties();
				}
				
				if (spongeWorld == null){
					source.sendMessage(Text.of(TextColors.RED, "You have to define a world to render!"));
					return CommandResult.empty();
				}

				World world = plugin.getWorld(spongeWorld.getUniqueId());
				if (world == null) {
					source.sendMessage(Text.of(TextColors.RED, "This world is not loaded with BlueMap! Maybe it is not configured?"));
				}
				
				world.invalidateChunkCache();
				
				Sponge.getScheduler().createTaskBuilder()
					.async()
					.execute(() -> createWorldRenderTask(source, world))
					.submit(plugin);
				
				return CommandResult.success();
			})
			.build();
	}
	
	public CommandSpec createPrioritizeTaskCommand() {
		return CommandSpec.builder()
			.description(Text.of("Prioritizes the render-task with the given uuid"))
			.permission("bluemap.rendertask.prioritize")
			.arguments(GenericArguments.uuid(Text.of("task-uuid")))
			.executor((source, args) -> {
				Optional<UUID> uuid = args.<UUID>getOne("task-uuid");
				if (!uuid.isPresent()) {
					source.sendMessage(Text.of("You need to specify a task-uuid"));
					return CommandResult.empty();
				}
				
				for (RenderTask task : plugin.getRenderManager().getRenderTasks()) {
					if (task.getUuid().equals(uuid.get())) {
						plugin.getRenderManager().prioritizeRenderTask(task);
						break;
					}
				}
				
				source.sendMessages(createStatusMessage());
				return CommandResult.success();
			})
			.build();
	}
	
	public CommandSpec createRemoveTaskCommand() {
		return CommandSpec.builder()
			.description(Text.of("Removes the render-task with the given uuid"))
			.permission("bluemap.rendertask.remove")
			.arguments(GenericArguments.uuid(Text.of("task-uuid")))
			.executor((source, args) -> {
				Optional<UUID> uuid = args.<UUID>getOne("task-uuid");
				if (!uuid.isPresent()) {
					source.sendMessage(Text.of("You need to specify a task-uuid"));
					return CommandResult.empty();
				}
				
				for (RenderTask task : plugin.getRenderManager().getRenderTasks()) {
					if (task.getUuid().equals(uuid.get())) {
						plugin.getRenderManager().removeRenderTask(task);
						break;
					}
				}
				
				source.sendMessages(createStatusMessage());
				return CommandResult.success();
			})
			.build();
	}
	
	private List<Text> createStatusMessage(){
		List<Text> lines = new ArrayList<>();
		
		RenderManager renderer = plugin.getRenderManager();
		
		lines.add(Text.EMPTY);
		lines.add(Text.of(TextColors.BLUE, "Tile-Updates:"));
		
		if (renderer.isRunning()) {
			lines.add(Text.of(TextColors.WHITE, " Render-Threads are ", Text.of(TextActions.runCommand("/bluemap pause"), TextActions.showText(Text.of("click to pause rendering")), TextColors.GREEN, "running"), TextColors.GRAY, "!"));
		} else {
			lines.add(Text.of(TextColors.WHITE, " Render-Threads are ", Text.of(TextActions.runCommand("/bluemap resume"), TextActions.showText(Text.of("click to resume rendering")), TextColors.RED, "paused"), TextColors.GRAY, "!"));
		}
		
		lines.add(Text.of(TextColors.WHITE, " Scheduled tile-updates: ", Text.of(TextActions.showText(Text.of("tiles waiting for a free render-thread")), TextColors.GOLD, renderer.getQueueSize()), Text.of(TextActions.showText(Text.of("tiles waiting for world-save")), TextColors.GRAY, " + " + plugin.getUpdateHandler().getUpdateBufferCount())));
		
		RenderTask[] tasks = renderer.getRenderTasks();
		if (tasks.length > 0) {
			RenderTask task = tasks[0];
			
			long time = task.getActiveTime();
			String durationString = DurationFormatUtils.formatDurationWords(time, true, true);
			double pct = (double)task.getRenderedTileCount() / (double)(task.getRenderedTileCount() + task.getRemainingTileCount());
			
			long ert = (long)((time / pct) * (1d - pct));
			String ertDurationString = DurationFormatUtils.formatDurationWords(ert, true, true);

			double tps = task.getRenderedTileCount() / (time / 1000.0);
			
			lines.add(Text.of(TextColors.BLUE, "Current task:"));
			lines.add(Text.of(" ", createCancelTaskText(task), TextColors.WHITE, " Task ", TextColors.GOLD, task.getName(), TextColors.WHITE, " for map ", TextActions.showText(Text.of(TextColors.WHITE, "World: ", TextColors.GOLD, task.getMapType().getWorld().getName())), TextColors.GOLD, task.getMapType().getName()));
			lines.add(Text.of(TextColors.WHITE, " rendered ", TextColors.GOLD, task.getRenderedTileCount(), TextColors.WHITE, " tiles ", TextColors.GRAY, "(" + (Math.round(pct * 1000)/10.0) + "% | " + GenericMath.round(tps, 1) + "t/s)", TextColors.WHITE, " in ", TextColors.GOLD, durationString));
			lines.add(Text.of(TextColors.WHITE, " with ", TextColors.GOLD, task.getRemainingTileCount(), TextColors.WHITE, " tiles to go. ETA: ", TextColors.GOLD, ertDurationString));
		}

		if (tasks.length > 1) {
			lines.add(Text.of(TextColors.BLUE, "Waiting tasks:"));
			for (int i = 1; i < tasks.length; i++) {
				RenderTask task = tasks[i];
				lines.add(Text.of(" ", createCancelTaskText(task), createPrioritizeTaskText(task), TextColors.WHITE, " Task ", TextColors.GOLD, task.getName(), TextColors.WHITE, " for map ", Text.of(TextActions.showText(Text.of(TextColors.WHITE, "World: ", TextColors.GOLD, task.getMapType().getWorld().getName())), TextColors.GOLD, task.getMapType().getName()), TextColors.GRAY, " (" + task.getRemainingTileCount() + " tiles)"));
			}
		}
		
		return lines;
	}
	
	private Text createCancelTaskText(RenderTask task) {
		return Text.of(TextActions.runCommand("/bluemap render remove " + task.getUuid()), TextActions.showText(Text.of("click to remove this render-task")), TextColors.RED, "[X]");
	}
	
	private Text createPrioritizeTaskText(RenderTask task) {
		return Text.of(TextActions.runCommand("/bluemap render prioritize " + task.getUuid()), TextActions.showText(Text.of("click to prioritize this render-task")), TextColors.GREEN, "[^]");
	}
	
	private void createWorldRenderTask(CommandSource source, World world) {
		source.sendMessage(Text.of(TextColors.GOLD, "Collecting chunks to render..."));
		Collection<Vector2i> chunks = world.getChunkList();
		source.sendMessage(Text.of(TextColors.GREEN, chunks.size() + " chunks found!"));
		
		for (MapType map : SpongePlugin.getInstance().getMapTypes()) {
			if (!map.getWorld().getUUID().equals(world.getUUID())) continue;

			source.sendMessage(Text.of(TextColors.GOLD, "Collecting tiles for map '" + map.getId() + "'"));
			
			HiresModelManager hmm = map.getTileRenderer().getHiresModelManager();
			Set<Vector2i> tiles = new HashSet<>();
			for (Vector2i chunk : chunks) {
				Vector3i minBlockPos = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
				tiles.add(hmm.posToTile(minBlockPos));
				tiles.add(hmm.posToTile(minBlockPos.add(0, 0, 15)));
				tiles.add(hmm.posToTile(minBlockPos.add(15, 0, 0)));
				tiles.add(hmm.posToTile(minBlockPos.add(15, 0, 15)));
			}

			RenderTask task = new RenderTask("world-render", map);
			task.addTiles(tiles);
			task.optimizeQueue();
			plugin.getRenderManager().addRenderTask(task);
			
			source.sendMessage(Text.of(TextColors.GREEN, tiles.size() + " tiles found! Task created."));
		}

		source.sendMessage(Text.of(TextColors.GREEN, "All render tasks created! Use /bluemap to view the progress!"));
	}
	
}
