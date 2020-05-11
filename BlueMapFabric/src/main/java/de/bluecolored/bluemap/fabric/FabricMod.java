package de.bluecolored.bluemap.fabric;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.world.ServerWorld;

public class FabricMod implements ModInitializer, ServerInterface {
	
	private Plugin plugin;
	
	@Override
	public void onInitialize() {
		Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));
		
		this.plugin = new Plugin("forge", this);
		
		//register commands
		CommandRegistry.INSTANCE.register(true, dispatcher -> {
			new Commands<>(plugin, dispatcher, fabricSource -> new FabricCommandSource(this, plugin, fabricSource));
		});
	}
	
	public UUID getUUIDForWorld(ServerWorld world) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public void registerListener(ServerEventListener listener) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public void unregisterAllListeners() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public UUID getUUIDForWorld(File worldFolder) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public File getConfigFolder() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}
	
}
