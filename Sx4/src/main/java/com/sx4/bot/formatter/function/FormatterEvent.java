package com.sx4.bot.formatter.function;

import com.sx4.bot.formatter.FormatterManager;

import java.util.HashMap;
import java.util.Map;

public class FormatterEvent {

	private final Object object;
	private final Map<String, Object> arguments;
	private final FormatterManager manager;

	public FormatterEvent(Object object, Map<String, Object> arguments, FormatterManager manager) {
		this.object = object;
		this.arguments = arguments;
		this.manager = manager;
	}

	public Object getObject() {
		return this.object;
	}

	public Map<String, Object> getArguments() {
		return new HashMap<>(this.arguments);
	}

	public void addArgument(String name, Object value) {
		this.arguments.put(name, value);
	}

	public FormatterManager getManager() {
		return this.manager;
	}

}
