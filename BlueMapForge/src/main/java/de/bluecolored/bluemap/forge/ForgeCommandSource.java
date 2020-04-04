package de.bluecolored.bluemap.forge;

import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import net.minecraft.util.text.ITextComponent;

public class ForgeCommandSource implements CommandSource {

	private net.minecraft.command.CommandSource delegate;
	
	public ForgeCommandSource(net.minecraft.command.CommandSource delegate) {
		this.delegate = delegate;
	}

	@Override
	public void sendMessage(Text text) {
		delegate.sendFeedback(ITextComponent.Serializer.fromJson(text.toJSONString()), false);
	}
	
}
