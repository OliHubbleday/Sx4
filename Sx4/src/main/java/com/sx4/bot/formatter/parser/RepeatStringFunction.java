package com.sx4.bot.formatter.parser;

import com.sx4.bot.formatter.function.FormatterEvent;
import com.sx4.bot.formatter.function.FormatterFunction;

public class RepeatStringFunction extends FormatterFunction<String> {

	public RepeatStringFunction() {
		super(String.class, "repeat", "Repeats a string by the count given");
	}

	public String parse(FormatterEvent<String> event, Integer count) {
		if (count > 1000) {
			return event.getObject();
		}

		return event.getObject().repeat(count);
	}

}
