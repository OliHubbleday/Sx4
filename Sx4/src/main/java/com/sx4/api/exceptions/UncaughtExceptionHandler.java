package com.sx4.api.exceptions;

import com.sx4.bot.utility.ExceptionUtility;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Optional;

@Provider
public class UncaughtExceptionHandler implements ExceptionMapper<Throwable> {
	
	public Response toResponse(Throwable exception) {
		ExceptionUtility.sendErrorMessage(exception);
		
		if (exception instanceof WebApplicationException) {
			return ((WebApplicationException) exception).getResponse();
		}
		
		Optional<StackTraceElement> element = Arrays.stream(exception.getStackTrace())
			.filter(e -> e.getClassName().contains("com.sx4.api"))
			.findFirst();
		
		return Response.status(500)
			.entity(exception.toString() + (element.map(stackTraceElement -> "\n" + stackTraceElement.toString()).orElse("")))
			.type("text/plain")
			.build();
	}
}