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
package de.bluecolored.bluemap.common.plugin.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Text {

	private String content = "";
	private TextColor color;
	private Set<TextFormat> formats = new HashSet<>();
	private Text hoverText;
	private String clickCommand;
	private List<Text> children = new ArrayList<>();
	
	public Text setHoverText(Text hoverText) {
		this.hoverText = hoverText;
		
		return this;
	}
	
	public Text setClickCommand(String clickCommand) {
		this.clickCommand = clickCommand;
		
		return this;
	}
	
	public Text addChild(Text child) {
		children.add(child);
		
		return this;
	}
	
	public String toJSONString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		
		sb.append(quote("text")).append(":").append(quote(content)).append(',');

		if (color != null) {
			sb.append(quote("color")).append(":").append(quote(color.getId())).append(',');
		}

		for (TextFormat format : formats) {
			sb.append(quote(format.getId())).append(":").append(true).append(',');
		}
		
		if (hoverText != null) {
			sb.append(quote("hoverEvent")).append(":{");
			sb.append(quote("action")).append(":").append(quote("show_text")).append(',');
			sb.append(quote("value")).append(":").append(quote(hoverText.toFormattingCodedString('\u00a7')));
			sb.append("},");
		}

		if (clickCommand != null) {
			sb.append(quote("clickEvent")).append(":{");
			sb.append(quote("action")).append(":").append(quote("run_command")).append(',');
			sb.append(quote("value")).append(":").append(quote(clickCommand));
			sb.append("},");
		}
		
		if (!children.isEmpty()) {
			sb.append(quote("extra")).append(":[");
			for (Text child : children) {
				sb.append(child.toJSONString()).append(',');
			}
			sb.deleteCharAt(sb.length() - 1); //delete last ,
			sb.append("],");
		}
		
		if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);  //delete last ,
		
		sb.append("}");
		return sb.toString();
	}
	
	public String toFormattingCodedString(char escapeChar) {
		StringBuilder sb = new StringBuilder();
		
		if (!content.isEmpty()) {
			if (color != null) {
				sb.append(escapeChar).append(color.getFormattingCode());
			}
			
			for (TextFormat format : formats) {
				sb.append(escapeChar).append(format.getFormattingCode());
			}
			
			sb.append(content);
		}
		
		for (Text child : children) {
			if (sb.length() > 0) sb.append(escapeChar).append('r');
			sb.append(child.withParentFormat(this).toFormattingCodedString(escapeChar));
		}
		
		return sb.toString();
	}
	
	public String toPlainString() {
		StringBuilder sb = new StringBuilder();
		
		if (content != null) sb.append(content);
		for (Text child : children) {
			sb.append(child.toPlainString());
		}
		
		return sb.toString();
	}
	
	private Text withParentFormat(Text parent) {
		Text text = new Text();
		
		text.content = this.content;
		text.clickCommand = this.clickCommand;
		text.children = this.children;
		
		text.color = this.color != null ? this.color : parent.color;

		text.formats.addAll(this.formats);
		text.formats.addAll(parent.formats);
		
		return text;
	}
	
	private String quote(String value) {
		return '"' + escape(value) + '"';
	}
	
	private String escape(String value) {
		value = value.replace("\\", "\\\\");
		value = value.replace("\"", "\\\"");
		value = value.replace("\u00a7", "\\u00a7");
		value = value.replace("\n", "\\n");
		return value;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + toJSONString();
	}
	
	public static Text of(String message) {
		Text text = new Text();
		text.content = message;
		return text;
	}
	
	public static Text of(TextColor color, String message) {
		Text text = new Text();
		text.content = message;
		text.color = color;
		return text;
	}
	
	public static Text of(Object... objects) {
		Text text = new Text();
		
		Text currentChild = new Text();
		for (Object object : objects) {
			
			if (object instanceof Text) {
				if (!currentChild.content.isEmpty()) {
					text.addChild(currentChild);
					currentChild = new Text().withParentFormat(currentChild);
				}
				
				text.addChild((Text) object);
				continue;
			}
			
			if (object instanceof TextColor) {
				if (!currentChild.content.isEmpty()) {
					text.addChild(currentChild);
					currentChild = new Text();
				}
				
				currentChild.color = (TextColor) object;
				continue;
			}
			
			if (object instanceof TextFormat) {
				if (!currentChild.content.isEmpty()) {
					text.addChild(currentChild);
					currentChild = new Text().withParentFormat(currentChild);
				}
				
				currentChild.formats.add((TextFormat) object);
				continue;
			}
			
			currentChild.content += object.toString();
			
		}
		
		if (!currentChild.content.isEmpty()) {
			text.addChild(currentChild);
		}
		
		if (text.children.isEmpty()) return text;
		if (text.children.size() == 1) return text.children.get(0);
		
		return text;
	}
	
}
