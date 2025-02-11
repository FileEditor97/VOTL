package dev.fileeditor.votl.servlet;

import dev.fileeditor.oauth2.OAuth2Client;
import dev.fileeditor.oauth2.session.Session;
import dev.fileeditor.votl.servlet.utils.AuthSessionController;
import dev.fileeditor.votl.servlet.utils.AuthStateController;
import io.javalin.http.util.CookieStore;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import java.io.FileNotFoundException;

import dev.fileeditor.votl.servlet.handlers.WebFilter;
import dev.fileeditor.votl.servlet.handlers.WebHandler;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

@SuppressWarnings("unused")
public class WebServlet {
	
	public static final Logger log = (Logger) LoggerFactory.getLogger(WebServlet.class);

	public static final int DEFAULT_PORT = 8080;

	private static Javalin web;
	private static OAuth2Client client;

	private final int port;
	private final String allowedHost;
	private final long clientId;
	private final String clientSecret;

	private static boolean initialized;

	public WebServlet(int port, String allowedHost, long clientId, String clientSecret) {
		this.port = port;
		this.allowedHost = allowedHost;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		WebServlet.initialized = false;
	}

	public static OAuth2Client getClient() {
		return client;
	}

	private void initialize() {
		log.info("Starting Javalin API on port: {}", port);

		web = Javalin.create(config -> {
				config.http.asyncTimeout = 10_000;
				config.bundledPlugins.enableCors(cors -> {
					cors.addRule(it -> {
						it.allowHost(allowedHost);
						it.allowCredentials = true;
					});
				});
			})
			.beforeMatched(WebFilter.authCheck())
			.before(WebFilter.filterRequest())
			.exception(FileNotFoundException.class, (e, ctx) -> ctx.status(HttpStatus.NOT_FOUND))
			.exception(Exception.class, WebHandler.exceptionHandler())
			.after(ctx -> ctx.cookieStore().clear())
			.start(port);

		client = new OAuth2Client.Builder()
			.setClientId(clientId)
			.setClientSecret(clientSecret)
			.setStateController(new AuthStateController())
			.setSessionController(new AuthSessionController())
			.build();

		initialized = true;
	}

	public static void shutdown() {
		if (initialized) {
			web.stop();
			client.shutdown();
		}
	}

	public static Session getSession(CookieStore cs) {
		if (!initialized) return null;

		// TODO session ID encode/decode
		String id = cs.get("session");
		if (id == null || id.isBlank()) return null;
		return getClient().getSessionController().getSession(id);
	}

	/**
	 * Map the handler for HTTP GET requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerGet(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("GET {} has been registered to {}", path, handler.getClass().getTypeName());
		web.get(path, handler);
	}

	/**
	 * Map the handler for HTTP POST requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerPost(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("POST {} has been registered to {}", path, handler.getClass().getTypeName());
		web.post(path, handler);
	}

	/**
	 * Map the handler for HTTP PUT requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerPut(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("PUT {} has been registered to {}", path, handler.getClass().getTypeName());
		web.put(path, handler);
	}

	/**
	 * Map the handler for HTTP PATCH requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerPatch(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("PATCH {} has been registered to {}", path, handler.getClass().getTypeName());
		web.patch(path, handler);
	}

	/**
	 * Map the handler for HTTP DELETE requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerDelete(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("DELETE {} has been registered to {}", path, handler.getClass().getTypeName());
		web.delete(path, handler);
	}

	/**
	 * Map the handler for HTTP HEAD requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerHead(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("HEAD {} has been registered to {}", path, handler.getClass().getTypeName());
		web.head(path, handler);
	}

	/**
	 * Map the handler for HTTP OPTIONS requests
	 *
	 * @param path  the path
	 * @param handler The handler
	 */
	public synchronized void registerOptions(final String path, final Handler handler) {
		if (!initialized) initialize();

		log.debug("OPTIONS {} has been registered to {}", path, handler.getClass().getTypeName());
		web.options(path, handler);
	}

}
