package votl.listeners;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import votl.App;
import votl.objects.command.SlashCommandEvent;
import votl.utils.LogUtil;
import votl.utils.database.DBUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Guild.Ban;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class LogListener {
	
	private final App bot;
	private final DBUtil db;
	private final LogUtil logUtil;

	public LogListener(App bot) {
		this.bot = bot;
		this.db = bot.getDBUtil();
		this.logUtil = bot.getLogUtil();
	}

	public void onBan(SlashCommandEvent event, User target, Member moderator, Integer banId) {
		String guildId = Objects.requireNonNull(event.getGuild()).getId();

		String channelId = db.guild.getModLogChannel(guildId);
		if (channelId == null) {
			return;
		}
		TextChannel channel = event.getJDA().getTextChannelById(channelId);
		if (channel == null) {
			return;
		}

		Map<String, Object> ban = db.ban.getInfo(banId);
		if (ban.isEmpty()) {
			bot.getLogger().warn("That is not supposed to happen... Ban ID: %s", banId);
			return;
		}

		try {
			MessageEmbed embed = logUtil.getBanEmbed(event.getGuildLocale(), ban, true);
			channel.sendMessageEmbeds(embed).queue();
		} catch (InsufficientPermissionException ex) {
			return;
		}
	}

	public void onUnban(SlashCommandEvent event, Member moderator, Ban banData, String reason) {
		String guildId = Objects.requireNonNull(event.getGuild()).getId();

		String channelId = db.guild.getModLogChannel(guildId);
		if (channelId == null) {
			return;
		}
		TextChannel channel = event.getJDA().getTextChannelById(channelId);
		if (channel == null) {
			return;
		}

		try {
			MessageEmbed embed = logUtil.getUnbanEmbed(event.getUserLocale(), banData, moderator, reason);
			channel.sendMessageEmbeds(embed).queue();
		} catch (InsufficientPermissionException ex) {
			return;
		}
	}

	// Group settings
	public void onGroupCreation(SlashCommandEvent event, Integer groupId, String name) {
		String masterId = event.getGuild().getId();

		String channelId = db.guild.getGroupLogChannel(masterId);
		if (channelId == null) {
			return;
		}
		TextChannel channel = event.getJDA().getTextChannelById(channelId);
		if (channel == null) {
			return;
		}

		try {
			MessageEmbed embed = logUtil.getGroupCreationEmbed(event.getGuildLocale(), event.getMember().getAsMention(), masterId, event.getGuild().getIconUrl(), groupId, name);
			channel.sendMessageEmbeds(embed).queue();
		} catch (InsufficientPermissionException ex) {
			return;
		}
	}

	public void onGroupDeletion(SlashCommandEvent event, Integer groupId, String name) {
		String masterId = event.getGuild().getId();
		String masterIcon = event.getGuild().getIconUrl();

		// For each group guild (except master) remove if from group DB and send log to log channel
		List<String> guildIds = db.group.getGroupGuildIds(groupId);
		for (String guildId : guildIds) {
			db.group.remove(groupId, guildId);
			String channelId = db.guild.getGroupLogChannel(guildId);
			if (channelId == null) {
				continue;
			}
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel == null) {
				continue;
			}

			try {
				MessageEmbed embed = logUtil.getGroupDeletedEmbed(channel.getGuild().getLocale(), masterId, masterIcon, groupId, name);
				channel.sendMessageEmbeds(embed).queue();
			} catch (InsufficientPermissionException ex) {
				continue;
			}
		}

		// Master log
		String channelId = db.guild.getGroupLogChannel(masterId);
		if (channelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel != null) {
				try {
					MessageEmbed embed = logUtil.getGroupDeletedMasterEmbed(event.getGuildLocale(), event.getMember().getAsMention(), masterId, masterIcon, groupId, name);
					channel.sendMessageEmbeds(embed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}
	}

	public void onGroupJoin(SlashCommandEvent event, Integer groupId, String name) {
		String guildId = event.getGuild().getId();
		String guildName = event.getGuild().getName();
		String masterId = db.group.getMaster(groupId);
		String masterIcon = event.getJDA().getGuildById(masterId).getIconUrl();

		String channelId = db.guild.getGroupLogChannel(guildId);
		
		if (channelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel != null) {
				try {
					MessageEmbed embed = logUtil.getGroupJoinEmbed(event.getGuildLocale(), event.getMember().getAsMention(), masterId, masterIcon, groupId, name);
					channel.sendMessageEmbeds(embed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}

		// Send log to group's master log channel
		String masterChannelId = db.guild.getGroupLogChannel(masterId);
		if (masterChannelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(masterChannelId);
			if (channel != null) {
				try {
					MessageEmbed masterEmbed = logUtil.getGroupJoinMasterEmbed(channel.getGuild().getLocale(), masterId, masterIcon, guildName, guildId, groupId, name);
					channel.sendMessageEmbeds(masterEmbed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}
	}

	public void onGroupLeave(SlashCommandEvent event, Integer groupId, String name) {
		String guildId = event.getGuild().getId();
		String guildName = event.getGuild().getName();
		String masterId = db.group.getMaster(groupId);
		String masterIcon = event.getJDA().getGuildById(masterId).getIconUrl();

		String channelId = db.guild.getGroupLogChannel(guildId);
		if (channelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel != null) {
				try {
					MessageEmbed embed = logUtil.getGroupLeaveEmbed(event.getGuildLocale(), event.getMember().getAsMention(), masterId, masterIcon, groupId, name);
					channel.sendMessageEmbeds(embed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}

		// Send log to group's master log channel
		String masterChannelId = db.guild.getGroupLogChannel(masterId);
		if (masterChannelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(masterChannelId);
			if (channel != null) {
				try {
					MessageEmbed masterEmbed = logUtil.getGroupLeaveMasterEmbed(channel.getGuild().getLocale(), masterId, masterIcon, guildName, guildId, groupId, name);
					channel.sendMessageEmbeds(masterEmbed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}
	}

	public void onGroupRemove(SlashCommandEvent event, Guild target, Integer groupId, String name) {
		String targetId = target.getId();
		String targetName = target.getName();
		String masterId = event.getGuild().getId();
		String masterIcon = event.getGuild().getIconUrl();

		String channelId = db.guild.getGroupLogChannel(targetId);
		if (channelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel != null) {
				try {
					MessageEmbed embed = logUtil.getGroupLeaveEmbed(channel.getGuild().getLocale(), "Forced, by group Master", masterId, masterIcon, groupId, name);
					channel.sendMessageEmbeds(embed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}

		// Send log to group's master log channel
		String masterChannelId = db.guild.getGroupLogChannel(masterId);
		if (masterChannelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(masterChannelId);
			if (channel != null) {
				try {
					MessageEmbed embed = logUtil.getGroupRemoveEmbed(event.getGuildLocale(), event.getMember().getAsMention(), masterId, masterIcon, targetName, targetId, groupId, name);
					channel.sendMessageEmbeds(embed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}
	}

	public void onGroupRename(SlashCommandEvent event, String oldName, Integer groupId, String newName) {
		String masterId = event.getGuild().getId();
		String masterIcon = event.getGuild().getIconUrl();

		// Send log to each group guild
		List<String> guildIds = db.group.getGroupGuildIds(groupId);
		for (String guildId : guildIds) {
			String channelId = db.guild.getGroupLogChannel(guildId);
			if (channelId == null) {
				continue;
			}
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel == null) {
				continue;
			}

			try {
				MessageEmbed embed = logUtil.getGroupRenamedEmbed(channel.getGuild().getLocale(), masterId, masterIcon, groupId, oldName, newName);
				channel.sendMessageEmbeds(embed).queue();
			} catch (InsufficientPermissionException ex) {
				continue;
			}
		}

		// Master log
		String channelId = db.guild.getGroupLogChannel(masterId);
		if (channelId != null) {
			TextChannel channel = event.getJDA().getTextChannelById(channelId);
			if (channel != null) {
				try {
					MessageEmbed embed = logUtil.getGroupRenamedMasterEmbed(event.getGuildLocale(), event.getMember().getAsMention(), masterId, masterIcon, groupId, oldName, newName);
					channel.sendMessageEmbeds(embed).queue();
				} catch (InsufficientPermissionException ex) {}
			}
		}
	}

	// To be done
	public void onAutoUnban(SlashCommandEvent event, User target, Integer banId) {}

	public void onSyncBan(SlashCommandEvent event, User target) {}

	public void onSyncUnban(SlashCommandEvent event, User target) {}

}
