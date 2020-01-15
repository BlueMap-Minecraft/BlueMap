package de.bluecolored.bluemap.plugin.serverinterface;

public enum TextColor {

	UNDEFINED ("undefined", 'r'),
	WHITE ("white", 'f'), 
	BLACK ("black", '0'),
	YELLOW ("yellow", 'e'), 
	GOLD ("gold", '6'), 
	AQUA ("aqua", 'b'), 
	DARK_AQUA ("dark_aqua", '3'), 
	BLUE ("blue", '9'), 
	DARK_BLUE ("dark_blue", '1'), 
	LIGHT_PURPLE ("light_purple", 'd'), 
	DARK_PURPLE ("dark_purple", '5'), 
	RED ("red", 'c'), 
	DARK_RED ("dark_red", '4'), 
	GREEN ("green", 'a'), 
	DARK_GREEN ("dark_green", '2'), 
	GRAY ("gray", '7'), 
	DARK_GRAY ("dark_gray", '8');
	
	private final String id;
	private final char colorCode;
	
	private TextColor(String id, char colorCode) {
		this.id = id;
		this.colorCode = colorCode;
	}

	public String getId() {
		return id;
	}

	public char getColorCode() {
		return colorCode;
	}

	public static TextColor ofColorCode(char code) {
		for (TextColor color : values()) {
			if (color.colorCode == code) return color;
		}
		
		throw new IllegalArgumentException("'" + code + "' isn't a valid color-code!");
	}
	
}
