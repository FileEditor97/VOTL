package dev.fileeditor.votl;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dev.fileeditor.votl.base.command.CommandClient;
import dev.fileeditor.votl.base.command.CommandClientBuilder;
import dev.fileeditor.votl.base.waiter.EventWaiter;
import dev.fileeditor.votl.commands.guild.*;
import dev.fileeditor.votl.commands.moderation.*;
import dev.fileeditor.votl.commands.other.*;
import dev.fileeditor.votl.commands.owner.*;
import dev.fileeditor.votl.commands.role.*;
import dev.fileeditor.votl.commands.strike.*;
import dev.fileeditor.votl.commands.ticketing.*;
import dev.fileeditor.votl.commands.verification.*;
import dev.fileeditor.votl.commands.voice.VoiceCmd;
import dev.fileeditor.votl.commands.webhook.WebhookCmd;
import dev.fileeditor.votl.listeners.*;
import dev.fileeditor.votl.menus.ActiveModlogsMenu;
import dev.fileeditor.votl.menus.ModlogsMenu;
import dev.fileeditor.votl.menus.ReportMenu;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.objects.constants.Links;
import dev.fileeditor.votl.services.CountingThreadFactory;
import dev.fileeditor.votl.services.ScheduledCheck;
import dev.fileeditor.votl.utils.CheckUtil;
import dev.fileeditor.votl.utils.GroupHelper;
import dev.fileeditor.votl.utils.ModerationUtil;
import dev.fileeditor.votl.utils.TicketUtil;
import dev.fileeditor.votl.utils.WebhookAppender;
import dev.fileeditor.votl.utils.database.DBUtil;
import dev.fileeditor.votl.utils.file.FileManager;
import dev.fileeditor.votl.utils.file.lang.LocaleUtil;
import dev.fileeditor.votl.utils.logs.GuildLogger;
import dev.fileeditor.votl.utils.logs.LogEmbedUtil;
import dev.fileeditor.votl.utils.message.EmbedUtil;
import dev.fileeditor.votl.utils.message.MessageUtil;

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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

@SuppressWarnings("BusyWait")
public class App {
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(App.class);

	public final String VERSION = Optional.ofNullable(App.class.getPackage().getImplementationVersion()).map(v -> "v"+v).orElse("DEVELOPMENT");

	public final JDA JDA;
	public final EventWaiter WAITER;
	private final CommandClient commandClient;

	private final FileManager fileManager = new FileManager();

	private final GuildLogger guildLogger;
	private final LogEmbedUtil logEmbedUtil;

	private final DBUtil dbUtil;
	private final MessageUtil messageUtil;
	private final EmbedUtil embedUtil;
	private final CheckUtil checkUtil;
	private final LocaleUtil localeUtil;
	private final TicketUtil ticketUtil;
	private final GroupHelper groupHelper;
	private final ModerationUtil moderationUtil;

	public App() {
		try {
			fileManager.addFile("config", "/config.json", Constants.DATA_PATH + "config.json")
				.addFile("database", "/server.db", Constants.DATA_PATH + "server.db")
				.addLang("en-GB")
				.addLang("ru");
		} catch (Exception ex) {
			logger.error("Error while interacting with File Manager", ex);
			System.exit(0);
		}
		
		// Define for default
		dbUtil		= new DBUtil(getFileManager());
		localeUtil	= new LocaleUtil(this, DiscordLocale.ENGLISH_UK);
		messageUtil	= new MessageUtil(localeUtil);
		embedUtil	= new EmbedUtil(localeUtil);
		checkUtil	= new CheckUtil(this);
		ticketUtil	= new TicketUtil(this);
		moderationUtil = new ModerationUtil(dbUtil, localeUtil);

		guildLogger		= new GuildLogger(this);
		logEmbedUtil	= new LogEmbedUtil(localeUtil);

		WAITER			= new EventWaiter();
		groupHelper		= new GroupHelper(this);
		CommandListener commandListener = new CommandListener(localeUtil);
		InteractionListener interactionListener = new InteractionListener(this, WAITER);

		GuildListener guildListener = new GuildListener(this);
		VoiceListener voiceListener = new VoiceListener(this);
		ModerationListener moderationListener = new ModerationListener(this);
		MessageListener messageListener = new MessageListener(this);
		AuditListener auditListener = new AuditListener(dbUtil, guildLogger);
		MemberListener memberListener = new MemberListener(this);

		ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(3, new CountingThreadFactory("VOTL", "Scheduler", false));
		ScheduledCheck scheduledCheck = new ScheduledCheck(this);

		scheduledExecutor.scheduleWithFixedDelay(scheduledCheck::regularChecks, 2, 5, TimeUnit.MINUTES);
		scheduledExecutor.scheduleWithFixedDelay(scheduledCheck::irregularChecks, 3, 15, TimeUnit.MINUTES);

		// Define a command client
		commandClient = new CommandClientBuilder()
			.setOwnerId(fileManager.getString("config", "owner-id"))
			.setServerInvite(Links.DISCORD)
			.setScheduleExecutor(scheduledExecutor)
			.setStatus(OnlineStatus.ONLINE)
			.setActivity(Activity.customStatus("-> /help"))
			.addSlashCommands(
				// guild
				new AccessCmd(this),
				new AutopunishCmd(this),
				new LogsCmd(this),
				new ModuleCmd(this, WAITER),
				new SetupCmd(this),
				// moderation
				new BanCmd(this),
				new BlacklistCmd(this),
				new CaseCmd(this),
				new DurationCmd(this),
				new GroupCmd(this, WAITER),
				new KickCmd(this, WAITER),
				new MuteCmd(this),
				new ModLogsCmd(this),
				new ModStatsCmd(this),
				new ReasonCmd(this),
				new SyncCmd(this, WAITER),
				new UnbanCmd(this),
				new UnmuteCmd(this),
				// other
				new AboutCmd(this),
				new HelpCmd(this),
				new PingCmd(this),
				new StatusCmd(this),
				// owner
				new EvalCmd(this),
				new ForceAccessCmd(this),
				new GenerateListCmd(this),
				new ShutdownCmd(this),
				// role
				new RoleCmd(this),
				new TempRoleCmd(this),
				// strike
				new ClearStrikesCmd(this),
				new DeleteStikeCmd(this, WAITER),
				new StrikeCmd(this),
				new StrikesCmd(this),
				// ticketing
				new AddUserCmd(this),
				new CloseCmd(this),
				new RcloseCmd(this),
				new RemoveUserCmd(this),
				new RolesManageCmd(this),
				new RolesPanelCmd(this),
				new TicketCountCmd(this),
				new TicketPanelCmd(this),
				// verification
				new VerifyPanelCmd(this),
				new VerifyRoleCmd(this),
				// voice
				new VoiceCmd(this),
				// webhook
				new WebhookCmd(this)
			)
			.addContextMenus(
				new ReportMenu(this),
				new ModlogsMenu(this),
				new ActiveModlogsMenu(this)
			)
			.setListener(commandListener)
			.setDevGuildIds(fileManager.getStringList("config", "dev-servers").toArray(new String[0]))
			.build();

		// Build
		AutoCompleteListener acListener = new AutoCompleteListener(commandClient, dbUtil);

		final Set<GatewayIntent> intents = Set.of(
			GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
			GatewayIntent.GUILD_INVITES,
			GatewayIntent.GUILD_MEMBERS,
			GatewayIntent.GUILD_MESSAGES,
			GatewayIntent.GUILD_MODERATION,
			GatewayIntent.GUILD_VOICE_STATES,
			GatewayIntent.GUILD_WEBHOOKS,
			GatewayIntent.MESSAGE_CONTENT
		);
		final Set<CacheFlag> enabledCacheFlags = Set.of(
			CacheFlag.EMOJI,
			CacheFlag.MEMBER_OVERRIDES,
			CacheFlag.STICKER,
			CacheFlag.ROLE_TAGS,
			CacheFlag.VOICE_STATE
		);
		final Set<CacheFlag> disabledCacheFlags = Set.of(
			CacheFlag.ACTIVITY,
			CacheFlag.CLIENT_STATUS,
			CacheFlag.ONLINE_STATUS,
			CacheFlag.SCHEDULED_EVENTS
		);

		JDABuilder mainBuilder = JDABuilder.create(fileManager.getString("config", "bot-token"), intents)
			.setMemberCachePolicy(MemberCachePolicy.ALL)	// cache all members
			.setChunkingFilter(ChunkingFilter.ALL)		// chunk all guilds
			.enableCache(enabledCacheFlags)
			.disableCache(disabledCacheFlags)
			.setBulkDeleteSplittingEnabled(false)
			.addEventListeners(
				commandClient, WAITER, acListener, interactionListener,
				guildListener, voiceListener, moderationListener, messageListener,
				auditListener, memberListener
			);
			
		JDA tempJda;

		int retries = 4; // how many times will it try to build
		int cooldown = 8; // in seconds; cooldown amount, will doubles after each retry
		while (true) {
			try {
				tempJda = mainBuilder.build();
				break;
			} catch (IllegalArgumentException | InvalidTokenException ex) {
				logger.error("Login failed due to Token", ex);
				System.exit(0);
			} catch (ErrorResponseException ex) { // Tries to reconnect to discord x times with some delay, else exits
				if (retries > 0) {
					retries--;
					logger.info("Retrying connecting in {} seconds... {} more attempts", cooldown, retries);
					try {
						Thread.sleep(cooldown*1000L);
					} catch (InterruptedException e) {
						logger.error("Thread sleep interrupted", e);
					}
					cooldown*=2;
				} else {
					logger.error("No network connection or couldn't connect to DNS", ex);
					System.exit(0);
				}
			}
		}

		this.JDA = tempJda;
	}

	public CommandClient getClient() {
		return commandClient;
	}

	public Logger getAppLogger() {
		return logger;
	}

	public FileManager getFileManager() {
		return fileManager;
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

	public LocaleUtil getLocaleUtil() {
		return localeUtil;
	}

	public GuildLogger getLogger() {
		return guildLogger;
	}

	public LogEmbedUtil getLogEmbedUtil() {
		return logEmbedUtil;
	}

	public TicketUtil getTicketUtil() {
		return ticketUtil;
	}

	public GroupHelper getHelper() {
		return groupHelper;
	}

	public ModerationUtil getModerationUtil() {
		return moderationUtil;
	}

	public static void main(String[] args) {
		App instance = new App();
		instance.createWebhookAppender();
		instance.logger.info("Success start");
	}

	private void createWebhookAppender() {
		String url = getFileManager().getNullableString("config", "webhook");
		if (url == null) return;
		
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder ple = new PatternLayoutEncoder();
		ple.setPattern("%d{dd.MM.yyyy HH:mm:ss} [%thread] [%logger{0}] %msg%n");
		ple.setContext(lc);
		ple.start();
		WebhookAppender webhookAppender = new WebhookAppender();
		webhookAppender.setUrl(url);
		webhookAppender.setEncoder(ple);
		webhookAppender.setContext(lc);
		webhookAppender.start();

		Logger logbackLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logbackLogger.addAppender(webhookAppender);
		logbackLogger.setAdditive(false);
	}
}
