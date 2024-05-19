package dev.fileeditor.votl.listeners;

import dev.fileeditor.votl.objects.logs.LogType;
import dev.fileeditor.votl.utils.database.DBUtil;
import dev.fileeditor.votl.utils.logs.GuildLogger;

import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AuditListener extends ListenerAdapter {
	
	private final DBUtil db;
	private final GuildLogger logger;
 
	public AuditListener(DBUtil dbUtil, GuildLogger loggingUtil) {
		this.db = dbUtil;
		this.logger = loggingUtil;
	}

	@Override
	public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
		AuditLogEntry entry = event.getEntry();
		switch (entry.getType()) {
			case CHANNEL_CREATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.CHANNEL)) return;
				
				logger.channel.onChannelCreate(entry);
			}
			case CHANNEL_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.CHANNEL)) return;
				
				logger.channel.onChannelUpdate(entry);
			}
			case CHANNEL_DELETE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.CHANNEL)) return;
				
				logger.channel.onChannelDelete(entry);
				// remove from db exceptions
				db.logExceptions.removeException(event.getGuild().getIdLong(), entry.getTargetIdLong());
			}
			case CHANNEL_OVERRIDE_CREATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.CHANNEL)) return;

				logger.channel.onOverrideCreate(entry);
			}
			case CHANNEL_OVERRIDE_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.CHANNEL)) return;

				logger.channel.onOverrideUpdate(entry);
			}
			case CHANNEL_OVERRIDE_DELETE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.CHANNEL)) return;

				logger.channel.onOverrideDelete(entry);
			}
			case ROLE_CREATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.ROLE)) return;
				
				logger.role.onRoleCreate(entry);
			}
			case ROLE_DELETE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.ROLE)) return;
				
				logger.role.onRoleDelete(entry);
			}
			case ROLE_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.ROLE)) return;
				
				logger.role.onRoleUpdate(entry);
			}
			case GUILD_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onGuildUpdate(entry);
			}
			case EMOJI_CREATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onEmojiCreate(entry);
			}
			case EMOJI_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onEmojiUpdate(entry);
			}
			case EMOJI_DELETE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onEmojiDelete(entry);
			}
			case STICKER_CREATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onStickerCreate(entry);
			}
			case STICKER_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onStickerUpdate(entry);
			}
			case STICKER_DELETE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.GUILD)) return;
				
				logger.server.onStickerDelete(entry);
			}
			case MEMBER_ROLE_UPDATE -> {
				// check if enabled log
				if (!db.getLogSettings(event.getGuild()).enabled(LogType.MEMBER)) return;
				// Ignore role changes by bot, as bot already logs with role connected changes (except verify and strike)
				if (entry.getUserIdLong() == event.getJDA().getSelfUser().getIdLong()) return;

				logger.member.onRoleChange(entry);
			}
		}
	}

}
