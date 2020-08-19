/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.sponge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;

public class SpongeCommands {

	private CommandDispatcher<CommandSource> dispatcher;
	
	public SpongeCommands(final Plugin plugin) {
		this.dispatcher = new CommandDispatcher<>();
		
		// register commands
		new Commands<>(plugin, dispatcher, bukkitSender -> new SpongeCommandSource(plugin, bukkitSender));
	}
	
	public Collection<SpongeCommandProxy> getRootCommands(){
		Collection<SpongeCommandProxy> rootCommands = new ArrayList<>();
		
		for (CommandNode<CommandSource> node : this.dispatcher.getRoot().getChildren()) {
			rootCommands.add(new SpongeCommandProxy(node.getName()));
		}
		
		return rootCommands;
	}
	
	public class SpongeCommandProxy implements CommandCallable {

		private String label;
		
		protected SpongeCommandProxy(String label) {
			this.label = label;
		}
		
		@Override
		public CommandResult process(CommandSource source, String arguments) throws CommandException {
			String command = label;
			if (!arguments.isEmpty()) {
				command += " " + arguments;
			}
			
			try {
				return CommandResult.successCount(dispatcher.execute(command, source));
			} catch (CommandSyntaxException ex) {
				source.sendMessage(Text.of(TextColors.RED, ex.getRawMessage().getString()));
				
				String context = ex.getContext();
				if (context != null) source.sendMessage(Text.of(TextColors.GRAY, context));
				
				return CommandResult.empty();
			}
		}

		@Override
		public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) throws CommandException {
			String command = label;
			if (!arguments.isEmpty()) {
				command += " " + arguments;
			}

			List<String> completions = new ArrayList<>();

			try {
				Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(command, source)).get(100, TimeUnit.MILLISECONDS);
				for (Suggestion suggestion : suggestions.getList()) {
					String text = suggestion.getText();
	
					if (text.indexOf(' ') == -1) {
						completions.add(text);
					}
				}
			} catch (InterruptedException ignore) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException | TimeoutException ignore) {}

			completions.sort((s1, s2) -> s1.compareToIgnoreCase(s2));
			return completions;
		}

		@Override
		public boolean testPermission(CommandSource source) {
			return true;
		}

		@Override
		public Optional<Text> getShortDescription(CommandSource source) {
			return Optional.empty();
		}

		@Override
		public Optional<Text> getHelp(CommandSource source) {
			return Optional.empty();
		}

		@Override
		public Text getUsage(CommandSource source) {
			CommandNode<CommandSource> node = dispatcher.getRoot().getChild(label);
			if (node == null) return Text.of("/" + label);
			
			List<Text> lines = new ArrayList<>();
			for (String usageString : dispatcher.getSmartUsage(node, source).values()) {
				lines.add(Text.of(TextColors.WHITE, "/" + label + " ", TextColors.GRAY, usageString));
			}
			
			return Text.joinWith(Text.NEW_LINE, lines);
		}

		public String getLabel() {
			return label;
		}
		
	}
	
}
