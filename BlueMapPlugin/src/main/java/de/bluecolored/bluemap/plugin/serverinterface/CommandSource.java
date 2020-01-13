package de.bluecolored.bluemap.plugin.serverinterface;

import org.apache.commons.lang3.StringUtils;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

public interface CommandSource {

	default void sendMessage(String plainMessage) {
		CompoundTag textTag = new CompoundTag();
		textTag.putString("text", plainMessage);
		sendSerializedMessage(textTag);
	}

	default void sendMessage(String plainMessage, char colorCodePrefix) {
		sendSerializedMessage(parseColorCodes(plainMessage, colorCodePrefix));
	}
	
	default void sendMessage(Iterable<Tag<?>> textTags) {
		ListTag<Tag<?>> textListTag = new ListTag<>(Tag.class);
		for (Tag<?> textTag : textTags) {
			textListTag.add(textTag);
		}
		sendSerializedMessage(textListTag);
	}
	
	default void sendMessage(Tag<?>... textTags) {
		ListTag<Tag<?>> textListTag = new ListTag<>(Tag.class);
		for (Tag<?> textTag : textTags) {
			textListTag.add(textTag);
		}
		sendSerializedMessage(textListTag);
	}
	
	void sendSerializedMessage(Tag<?> textListTag);

	static Tag<?> parseColorCodes(String plainMessage, char colorCodePrefix) {
		ListTag<Tag<?>> tagList = new ListTag<>(Tag.class);
		tagList.addString("");
		
		if (plainMessage.isEmpty()) return tagList;
		
		TextColor color = TextColor.UNDEFINED;
		boolean bold = false;
		boolean italic = false;
		boolean obfuscated = false;
		boolean strikethrough = false;
		boolean underlined = false;

		plainMessage = "r" + plainMessage;
		
		String[] parts = StringUtils.split(plainMessage, colorCodePrefix);
		
		for (String part : parts) {
			if (part.isEmpty()) throw new IllegalArgumentException("There is a color code with no color-char!");
			String message = part.substring(1);
			char code = part.charAt(0);
			
			switch (code) {
			case 'r':
				color = TextColor.UNDEFINED;
				bold = false;
				italic = false;
				obfuscated = false;
				strikethrough = false;
				underlined = false;
			case 'k':
				obfuscated = true;
				break;
			case 'l':
				bold = true;
				break;
			case 'm':
				strikethrough = true;
				break;
			case 'n':
				underlined = true;
				break;
			case 'o':
				italic = true;
				break;
			default:
				color = TextColor.ofColorCode(code);
				break;
			}

			if (message.isEmpty()) continue;
			
			CompoundTag textTag = new CompoundTag();
			textTag.putString("text", message);
			if (color != TextColor.UNDEFINED) textTag.putString("color", color.getId());
			if (bold) textTag.putBoolean("bold", true);
			if (italic) textTag.putBoolean("italic", true);
			if (underlined) textTag.putBoolean("underlined", true);
			if (strikethrough) textTag.putBoolean("strikethrough", true);
			if (obfuscated) textTag.putBoolean("obfuscated", true);
			
			
		}
		
		return tagList;
	}
	
}
