package de.bluecolored.bluemap.sponge;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.storage.WorldProperties;

import de.bluecolored.bluemap.common.plugin.Commands;

public class SpongeCommands {
	
	private Commands commands;
	
	public SpongeCommands(Commands commands) {
		this.commands = commands;
	}
	
	public CommandSpec createRootCommand() {
		return CommandSpec.builder()
			.description(Text.of("Displays BlueMaps render status"))
			.permission("bluemap.status")
			.childArgumentParseExceptionFallback(false)
			.child(createReloadCommand(), "reload")
			.child(createPauseRenderCommand(), "pause")
			.child(createResumeRenderCommand(), "resume")
			.child(createRenderCommand(), "render")
			.child(createDebugCommand(), "debug")
			.executor((source, args) -> {
				commands.executeRootCommand(new SpongeCommandSource(source));
				return CommandResult.success();
			})
			.build();
	}
	
	public CommandSpec createReloadCommand() {
		return CommandSpec.builder()
			.description(Text.of("Reloads all resources and configuration-files"))
			.permission("bluemap.reload")
			.executor((source, args) -> {
				commands.executeReloadCommand(new SpongeCommandSource(source));
				return CommandResult.success();
			})
			.build();
	}

	public CommandSpec createPauseRenderCommand() {
		return CommandSpec.builder()
			.description(Text.of("Pauses all rendering"))
			.permission("bluemap.pause")
			.executor((source, args) -> {
				if (commands.executePauseCommand(new SpongeCommandSource(source))) {
					return CommandResult.success();
				} else {
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
				if (commands.executeResumeCommand(new SpongeCommandSource(source))) {
					return CommandResult.success();
				} else {
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

				if (commands.executeRenderWorldCommand(new SpongeCommandSource(source), spongeWorld.getUniqueId())) {
					return CommandResult.success();
				} else {
					return CommandResult.empty();
				}
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
					source.sendMessage(Text.of(TextColors.RED, "You need to specify a task-uuid"));
					return CommandResult.empty();
				}
				
				commands.executePrioritizeRenderTaskCommand(new SpongeCommandSource(source), uuid.get());
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
					source.sendMessage(Text.of(TextColors.RED, "You need to specify a task-uuid"));
					return CommandResult.empty();
				}
				
				commands.executeRemoveRenderTaskCommand(new SpongeCommandSource(source), uuid.get());
				return CommandResult.success();
			})
			.build();
	}
	
	public CommandSpec createDebugCommand() {
		return CommandSpec.builder()
			.permission("bluemap.debug")
			.description(Text.of("Prints some debug info"))
			.extendedDescription(Text.of("Prints some information about how bluemap sees the blocks at and below your position"))
			.executor((source, args) -> {
				if (source instanceof Locatable) {
					Location<org.spongepowered.api.world.World> loc = ((Locatable) source).getLocation();
					UUID worldUuid = loc.getExtent().getUniqueId();
					
					if (commands.executeDebugCommand(new SpongeCommandSource(source), worldUuid, loc.getBlockPosition())) {
						return CommandResult.success();					
					} else {
						return CommandResult.empty();
					}
				}
				
				source.sendMessage(Text.of(TextColors.RED, "You can only execute this command as a player!"));
				return CommandResult.empty();
			})
			.build();
	}
	
}
