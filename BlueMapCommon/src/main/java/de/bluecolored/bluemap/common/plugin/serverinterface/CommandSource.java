package de.bluecolored.bluemap.common.plugin.serverinterface;

import de.bluecolored.bluemap.common.plugin.text.Text;

public interface CommandSource {

	void sendMessage(Text text);
	
	default void sendMessages(Iterable<Text> textLines) {
		for (Text text : textLines) {
			sendMessage(text);
		}
	}
	
}
