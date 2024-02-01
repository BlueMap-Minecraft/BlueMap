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
package de.bluecolored.bluemap.bukkit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BukkitCommands implements Listener {

    private final CommandDispatcher<CommandSender> dispatcher;

    public BukkitCommands(final Plugin plugin) {
        this.dispatcher = new CommandDispatcher<>();

        // register commands
        new Commands<>(plugin, dispatcher, bukkitSender -> {

            // RCON doesn't work async, use console instead
            if (bukkitSender instanceof RemoteConsoleCommandSender)
                return new BukkitCommandSource(plugin, Bukkit.getConsoleSender());

            return new BukkitCommandSource(plugin, bukkitSender);
        });
    }

    public Collection<BukkitCommand> getRootCommands(){
        Collection<BukkitCommand> rootCommands = new ArrayList<>();

        for (CommandNode<CommandSender> node : this.dispatcher.getRoot().getChildren()) {
            rootCommands.add(new CommandProxy(node.getName()));
        }

        return rootCommands;
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent evt) {
        try {
            String input = evt.getBuffer();
            if (!input.isEmpty() && input.charAt(0) == '/') {
                input = input.substring(1);
            }

            Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, evt.getSender())).get(100, TimeUnit.MILLISECONDS);
            List<String> completions = new ArrayList<>();
            for (Suggestion suggestion : suggestions.getList()) {
                String text = suggestion.getText();

                if (text.indexOf(' ') == -1) {
                    completions.add(text);
                }
            }

            if (!completions.isEmpty()) {
                completions.sort(String::compareToIgnoreCase);

                try {
                    evt.getCompletions().addAll(completions);
                } catch (UnsupportedOperationException ex){
                    // fix for a bug with paper where the completion-Collection is not mutable for some reason
                    List<String> mutableCompletions = new ArrayList<>(evt.getCompletions());
                    mutableCompletions.addAll(completions);
                    evt.setCompletions(mutableCompletions);
                }
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignore) {}
    }

    private class CommandProxy extends BukkitCommand {

        protected CommandProxy(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            String command = commandLabel;
            if (args.length > 0) {
                command += " " + StringUtils.join(args, ' ');
            }

            try {
                return dispatcher.execute(command, sender) > 0;
            } catch (CommandSyntaxException ex) {
                sender.sendMessage(ChatColor.RED + ex.getRawMessage().getString());

                String context = ex.getContext();
                if (context != null) sender.sendMessage(ChatColor.GRAY + context);

                return false;
            }
        }

    }

}
