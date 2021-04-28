package com.sx4.bot.formatter.parser;

import com.sx4.bot.formatter.function.FormatterEvent;
import com.sx4.bot.formatter.function.FormatterFunction;

import java.util.List;

public class SubListFunction extends FormatterFunction<List> {

	public SubListFunction() {
		super(List.class, "subList");
	}

	public List<?> parse(FormatterEvent event, Integer start, Integer end) {
		List<?> list = (List<?>) event.getObject();
		if (list.isEmpty()) {
			return list;
		}

		int size = list.size();
		if (start < 0) {
			start = size + start;
		}

		if (end < 0) {
			end = size + end;
		}

		return list.subList(Math.max(0, start), Math.min(size, end));
	}

}