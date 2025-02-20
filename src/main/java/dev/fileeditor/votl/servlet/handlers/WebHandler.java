package dev.fileeditor.votl.servlet.handlers;

import dev.fileeditor.votl.servlet.WebServlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.javalin.http.*;

public class WebHandler {

	public static Handler setContentType() {
		return (ctx) -> ctx.contentType(ContentType.JSON);
	}

	public static ExceptionHandler<Exception> exceptionHandler() {
		return (ex, ctx) -> {
			WebServlet.log.error("{} {}", ctx.req().getMethod(), ctx.req().getPathInfo(), ex);

			ctx.json(response(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
		};
	}

	public static ExceptionHandler<HttpResponseException> errorResponseHandler() {
		return (ex, ctx) -> {
			ctx.json(response(HttpStatus.forStatus(ex.getStatus()), ex.getMessage()));
		};
	}

	private static ObjectNode response(HttpStatus status, String reason) {
		ObjectNode node = new ObjectMapper().createObjectNode();
		node.put("status", status.getCode());
		node.put("reason", reason);
		return node;
	}
}
