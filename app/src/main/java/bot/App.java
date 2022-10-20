package bot;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.annotation.Nonnull;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import org.slf4j.LoggerFactory;

import bot.utils.*;
import bot.utils.file.FileManager;
import bot.utils.file.lang.LangUtil;
import bot.utils.message.*;
import bot.commands.*;
import bot.commands.guild.*;
import bot.commands.owner.*;
import bot.commands.voice.*;
import bot.commands.webhook.*;
import bot.listeners.GuildListener;
import bot.listeners.VoiceListener;
import bot.objects.Emotes;
import bot.objects.constants.*;
import ch.qos.logback.classic.Logger;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class App {
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(App.class);

	private static App instance;

	public final String version = (App.class.getPackage().getImplementationVersion() == null) ? "DEVELOPMENT" : App.class.getPackage().getImplementationVersion();

	public final JDA jda;
	public final EventWaiter waiter;

	private final FileManager fileManager = new FileManager();

	private final Random random = new Random();

	private final GuildListener guildListener;
	private final VoiceListener voiceListener;
	
	private DBUtil dbUtil;
	private MessageUtil messageUtil;
	private EmbedUtil embedUtil;
	private LangUtil langUtil;
	private CheckUtil checkUtil;

	public final String defaultLanguage = "en-GB";

	public App() {

		JDA setJda = null;

		try {
			fileManager.addFile("config", Constants.SEPAR + "config.json", Constants.DATA_PATH + Constants.SEPAR + "config.json")
				.addFile("database", Constants.SEPAR + "server.db", Constants.DATA_PATH + Constants.SEPAR + "server.db")
				.addLang("en-GB")
				.addLang("ru");
		} catch (Exception ex) {
			logger.error("Error while interacting with File Manager", ex);
			System.exit(0);
		}
		
		// Define for default
		waiter 			= new EventWaiter();
		guildListener 	= new GuildListener(this);
		voiceListener	= new VoiceListener(this);

		dbUtil		= new DBUtil(getFileManager().getFiles().get("database"));
		messageUtil = new MessageUtil(this);
		embedUtil 	= new EmbedUtil(this);
		langUtil 	= new LangUtil(this);
		checkUtil	= new CheckUtil(this);

		// Define a command client
		CommandClient commandClient = new CommandClientBuilder()
			.setOwnerId(fileManager.getString("config", "owner-id"))
			.setServerInvite(Links.DISCORD)
			.setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.FAILURE)
			.useHelpBuilder(false)
			.setStatus(OnlineStatus.ONLINE)
			.setActivity(Activity.watching("/help"))
			.addSlashCommands(
				// voice
				new SetNameCmd(this),
				new SetLimitCmd(this),
				new ClaimCmd(this),
				new NameCmd(this),
				new LimitCmd(this),
				new PermitCmd(this),
				new RejectCmd(this),
				new LockCmd(this),
				new UnlockCmd(this),
				new GhostCmd(this),
				new UnghostCmd(this),
				new PermsCmd(this),
				// guild
				new LanguageCmd(this),
				new SetupCmd(this),
				new ModuleCmd(this, waiter),
				new AccessCmd(this),
				// owner
				new ShutdownCmd(this),
				new EvalCmd(this),
				// webhook
				new WebhookCmd(this),
				// other
				new PingCmd(this),
				new AboutCmd(this),
				new HelpCmd(this),
				new StatusCmd(this)
			)
			.build();

		// Build
		MemberCachePolicy policy = MemberCachePolicy.VOICE		// check if in voice
			.or(Objects.requireNonNull(MemberCachePolicy.OWNER));						// check for owner

		Integer retries = 4; // how many times will it try to build
		Integer cooldown = 8; // in seconds; cooldown amount, will doubles after each retry
		while (true) {
			try {
				setJda = JDABuilder.createLight(fileManager.getString("config", "bot-token"))
					.setEnabledIntents(
						GatewayIntent.GUILD_MEMBERS,				// required for updating member profiles and ChunkingFilter
						GatewayIntent.GUILD_VOICE_STATES			// required for CF VOICE_STATE and policy VOICE
					)
					.setMemberCachePolicy(policy)
					.setChunkingFilter(ChunkingFilter.ALL)		// chunk all guilds
					.enableCache(
						CacheFlag.VOICE_STATE,			// required for policy VOICE
						CacheFlag.MEMBER_OVERRIDES,		// channel permission overrides
						CacheFlag.ROLE_TAGS				// role search
					) 
					.setAutoReconnect(true)
					.addEventListeners(commandClient, waiter, guildListener, voiceListener)
					.build();
				break;
			} catch (InvalidTokenException ex) {
				logger.error("Login failed due to Token", ex);
				System.exit(0);
			} catch (ErrorResponseException ex) { // Tries to reconnect to discord x times with some delay, else exits
				if (retries > 0) {
					retries--;
					logger.info("Retrying connecting in "+cooldown+" seconds..."+retries+" more attempts");
					try {
						Thread.sleep(cooldown*1000);
					} catch (InterruptedException e) {
						logger.error("Thread sleep interupted", e);
					}
					cooldown*=2;
				} else {
					logger.error("No network connection or couldn't connect to DNS", ex);
					System.exit(0);
				}
			}
		}
		

		this.jda = setJda;
	}

	public Logger getLogger() {
		return logger;
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public Random getRandom() {
		return random;
	}

	public DBUtil getDBUtil() {
		return dbUtil;
	}

	public MessageUtil getMessageUtil() {
		return messageUtil;
	}

	public EmbedUtil getEmbedUtil() {
		return embedUtil;
	}

	public CheckUtil getCheckUtil() {
		return checkUtil;
	}

	public List<String> getAllModules() {
		return List.of("voice", "webhook", "language");
	}

	@Nonnull
	public String getLanguage(String id) {
		String res = dbUtil.guildGetLanguage(id);
		return (res == null ? "en-GB" : res);
	}

	@ForRemoval
	@Nonnull
	public String getPrefix(String id) {
		return "/"; // default prefix
	}

	public void setLanguage(String id, String value) {
		dbUtil.guildSetLanguage(id, value);
	}

	@ForRemoval
	@Nonnull
	public String getMsg(String path) {
		return getMsg("0", path);
	}

	@ForRemoval
	@Nonnull
	public String getMsg(String id, String path) {
		return Objects.requireNonNull(
			setPlaceholders(langUtil.getString(getLanguage(id), path))
				.replace("{prefix}", getPrefix(id))
		);
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path) {
		return setPlaceholders(langUtil.getString(locale.getLocale(), path));
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, String target) {
		target = target == null ? "null" : target;
		
		return getLocalized(locale, path, user, Collections.singletonList(target));
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, List<String> targets) {
		String targetReplacement = targets.isEmpty() ? "null" : getMessageUtil().getFormattedMembers(locale, targets.toArray(new String[0]));

		return Objects.requireNonNull(getLocalized(locale, path, user)
			.replace("{target}", targetReplacement)
			.replace("{targets}", targetReplacement)
		);
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user) {
		return getLocalized(locale, path, user, true);
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, boolean format) {
		if (format)
			user = getMessageUtil().getFormattedMembers(locale, user);

		return Objects.requireNonNull(getLocalized(locale, path).replace("{user}", user));
	}

	public Map<DiscordLocale, String> getFullLocaleMap(String path) {
		Map<DiscordLocale, String> localeMap = new HashMap<>();
		for (DiscordLocale locale : fileManager.getLanguages()) {
			// Also counts en-US as en-GB (otherwise rises problem)
			if (locale.getLocale().equals("en-GB"))
				localeMap.put(DiscordLocale.ENGLISH_US, getLocalized(DiscordLocale.ENGLISH_US, path));
			localeMap.put(locale, getLocalized(locale, path));
		}
		return localeMap;
	}

	@Nonnull
	private String setPlaceholders(@Nonnull String msg) {
		return Objects.requireNonNull(Emotes.getWithEmotes(msg)
			.replace("{name}", "Voice of the Lord")
			.replace("{guild_invite}", Links.DISCORD)
			.replace("{owner_id}", fileManager.getString("config", "owner-id"))
			.replace("{developer_name}", Constants.DEVELOPER_NAME)
			.replace("{developer_id}", Constants.DEVELOPER_ID)
			.replace("{bot_invite}", fileManager.getString("config", "bot-invite"))
			.replace("{bot_version}", version)
		);
	}

	public static void main(String[] args) {
		instance = new App();
		instance.logger.info("Success start");
	}
}
