package de.bluecolored.bluemap.core.config;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Placeholder {

	private String name;
	private Supplier<String> valueSupplier;
	
	public Placeholder(String name, String value) {
		this(name, () -> value);
	}
	
	public Placeholder(String name, Supplier<String> valueSupplier) {
		this.name = name;
		this.valueSupplier = valueSupplier;
	}
	
	public String apply(String config) {
		return config.replaceAll(Pattern.quote("%" + name + "%"), valueSupplier.get());
	}
	
}
