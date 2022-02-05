/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package bot;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import org.slf4j.LoggerFactory;

import bot.utils.DBUtil;
import bot.utils.HelpWrapper;
import bot.utils.file.FileManager;
import bot.utils.file.lang.LangUtil;
import bot.utils.message.*;
import bot.commands.*;
import bot.commands.owner.*;
import bot.commands.voice.*;
import bot.constants.*;
import bot.listeners.GuildListener;
import bot.listeners.VoiceListener;
import ch.qos.logback.classic.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
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

	public String defaultPrefix;

	public App() {

		JDA setJda = null;

		fileManager.addFile("config", "/config.json", Constants.DATA_PATH + "/config.json")
			.addFile("database", "/server.db", Constants.DATA_PATH + "/server.db")
			.addLang("en");

		defaultPrefix = fileManager.getString("config", "default-prefix");
		
		// Define for default
		waiter 			= new EventWaiter();
		guildListener 	= new GuildListener(this);
		voiceListener	= new VoiceListener(this);

		dbUtil		= new DBUtil(getFileManager().getFiles().get("database"));
		messageUtil = new MessageUtil(this);
		embedUtil 	= new EmbedUtil(this);
		langUtil 	= new LangUtil(this);

		HelpWrapper helpWrapper = new HelpWrapper(this);

		// Define a command client
		CommandClient commandClient = new CommandClientBuilder()
			.setPrefix(defaultPrefix)
			.setPrefixFunction(event -> {
				if (!event.isFromGuild()) {
					return defaultPrefix;
				}
				return getPrefix(event.getGuild().getId());
			})
			.setOwnerId(Constants.OWNER_ID)
			.setServerInvite(Links.DISCORD)
			.setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
			.setHelpConsumer(helpWrapper::helpConsumer)
			.addCommands(
				// voice
				new SetupVoiceCmd(this),
				// owner
				new EvalCmd(this),
				new ShutdownCmd(this),
				// other
				new PingCmd(this),
				new AboutCmd(this)
			)
			.build();

		// Build
		MemberCachePolicy policy = MemberCachePolicy.VOICE;
		policy = policy.and(MemberCachePolicy.ONLINE);
		policy = policy.or(MemberCachePolicy.OWNER);

		try {
			setJda = JDABuilder.createDefault(fileManager.getString("config", "bot-token"))
				.setEnabledIntents(
					GatewayIntent.GUILD_PRESENCES,
					GatewayIntent.GUILD_MEMBERS,
					GatewayIntent.GUILD_MESSAGES,
					GatewayIntent.GUILD_VOICE_STATES,
					GatewayIntent.GUILD_EMOJIS,
					GatewayIntent.DIRECT_MESSAGES
				)
				.setMemberCachePolicy(policy)
				.setChunkingFilter(ChunkingFilter.ALL)
				.enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES, CacheFlag.ONLINE_STATUS)
				.setAutoReconnect(true)
				.addEventListeners(commandClient, waiter, guildListener, voiceListener)
				.setStatus(OnlineStatus.IDLE)
				.setActivity(Activity.watching("Loading..."))
				.build();
		} catch (LoginException e) {
			logger.error("Build failed", e);
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

	public String getLanguage(String id) {
		String res = dbUtil.guildGetLanguage(id);
		return (res == null ? "en" : res);
	}

	public String getPrefix(String id) {
		String res = dbUtil.guildGetPrefix(id);
		return (res == null ? defaultPrefix : res);
	}

	public void setLanguage(String id, String value) {
		dbUtil.guildSetLanguage(id, value);
	}

	public void setPrefix(String id, String value) {
		dbUtil.guildSetPrefix(id, value);
	}

	public String getMsg(String id, String path, String user, String target) {
		target = target == null ? "null" : target;
		
		return getMsg(id, path, user, Collections.singletonList(target));
	}

	public String getMsg(String id, String path, String user, List<String> targets) {
		String targetReplacement = targets.isEmpty() ? "null" : getMessageUtil().getFormattedMembers(id, targets.toArray(new String[0]));

		return getMsg(id, path, user)
			.replace("{target}", targetReplacement)
			.replace("{targets}", targetReplacement);
	}

	public String getMsg(String id, String path, String user) {
		return getMsg(id, path, user, true);
	}

	public String getMsg(String id, String path, String user, boolean format) {
		if (format)
			user = getMessageUtil().getFormattedMembers(id, user);

		return getMsg(id, path).replace("{user}", user);
	}

	public String getMsg(String id, String path) {
		return setPlaceholders(langUtil.getString(getLanguage(id), path))
			.replace("{prefix}", getPrefix(id));
	}

	private String setPlaceholders(String msg) {
		return Emotes.getWithEmotes(msg)
			.replace("{name}", "Voice of the Lord")
			.replace("{guild_invite}", Links.DISCORD)
			.replace("{github_url}", Links.GITHUB)
			.replace("{unionteams}", Links.UNIONTEAMS)
			.replace("{rotr_invite}", Links.ROTR_INVITE)
			.replace("{ww2_invite}", Links.WW2_INVITE)
			.replace("{owner}", fileManager.getString("config", "owner"))
			.replace("{owner_id}", Constants.OWNER_ID)
			.replace("{bot_invite}", fileManager.getString("config", "bot-invite"))
			.replace("{bot_version}", version);
	}


	public static void main(String[] args) {
		instance = new App();
		instance.logger.info("Success start");
		instance.jda.getPresence().setStatus(OnlineStatus.ONLINE);
		instance.jda.getPresence().setActivity(Activity.watching(instance.defaultPrefix + "help"));
	}
}
