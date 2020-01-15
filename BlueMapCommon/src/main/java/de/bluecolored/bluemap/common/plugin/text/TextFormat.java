package de.bluecolored.bluemap.common.plugin.text;

public enum TextFormat {

	OBFUSCATED ("obfuscated", 'k'), 
	BOLD ("bold", 'l'), 
	STRIKETHROUGH ("strikethrough", 'm'), 
	UNDERLINED ("underlined", 'n'), 
	ITALIC ("italic", 'o');

	private final String id;
	private final char formattingCode;
	
	private TextFormat(String id, char formattingCode) {
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
