package de.bluecolored.bluemap.common.plugin.text;

public enum TextColor {

	BLACK ("black", '0'), 
	DARK_BLUE ("dark_blue", '1'), 
	DARK_GREEN ("dark_green", '2'), 
	DARK_AQUA ("dark_aqua", '3'), 
	DARK_RED ("dark_red", '4'),
	DARK_PURPLE ("dark_purple", '5'), 
	GOLD ("gold", '6'), 
	GRAY ("gray", '7'), 
	DARK_GRAY ("dark_gray", '8'), 
	BLUE ("blue", '9'), 
	GREEN ("green", 'a'),
	AQUA ("aqua", 'b'), 
	RED ("red", 'c'), 
	LIGHT_PURPLE ("light_purple", 'd'), 
	YELLOW ("yellow", 'e'), 
	WHITE ("white", 'f');

	private final String id;
	private final char formattingCode;
	
	private TextColor(String id, char formattingCode) {
		this.id = id;
		this.formattingCode = formattingCode;
	}
	
	public String getId() {
		return id;
	}
	
	public char getFormattingCode() {
		return formattingCode;
	}

}
