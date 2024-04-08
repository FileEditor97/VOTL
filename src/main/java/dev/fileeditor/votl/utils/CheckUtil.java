package dev.fileeditor.votl.utils;

import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import dev.fileeditor.votl.App;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.exception.CheckException;

public class CheckUtil {

	private final App bot;
	private final String ownerId;

	public CheckUtil(App bot) {
		this.bot = bot;
		this.ownerId = bot.getFileManager().getString("config", "owner-id");
	}

	public boolean isDeveloper(UserSnowflake user) {
		return user.getId().equals(Constants.DEVELOPER_ID);
	}

	public boolean isBotOwner(UserSnowflake user) {
		return user.getId().equals(ownerId);
	}

	public CmdAccessLevel getAccessLevel(Member member) {
		// Is bot developer
		if (isDeveloper(member) || isBotOwner(member))
			return CmdAccessLevel.DEV;
		
		// Is guild's owner
		if (member.isOwner())
			return CmdAccessLevel.OWNER;
		
		// Check for user level
		CmdAccessLevel userLevel = bot.getDBUtil().access.getUserLevel(member.getGuild().getIdLong(), member.getIdLong());
		if (userLevel != null)
			return userLevel;
		
		// Check if has Administrator privileges
		if (member.hasPermission(Permission.ADMINISTRATOR))
			return CmdAccessLevel.ADMIN;

		// Check for role level
		List<Long> roleIds = bot.getDBUtil().access.getAllRoles(member.getGuild().getIdLong());
		CmdAccessLevel level = member.getRoles()
			.stream()
			.filter(role -> roleIds.contains(role.getIdLong()))
			.map(role -> bot.getDBUtil().access.getRoleLevel(role.getIdLong()))
			.max(CmdAccessLevel::compareTo)
			.orElse(CmdAccessLevel.ALL);
		
		return level;
	}

	public Boolean hasHigherAccess(Member who, Member than) {
		return getAccessLevel(who).isHigherThan(getAccessLevel(than));
	}

	public Boolean hasAccess(Member member, CmdAccessLevel accessLevel) {
		if (accessLevel.equals(CmdAccessLevel.ALL)) return true;
		if (accessLevel.isHigherThan(getAccessLevel(member)))
			return false;
		return true;
	}

	public CheckUtil hasAccess(IReplyCallback replyCallback, Member member, CmdAccessLevel accessLevel) throws CheckException {
		if (accessLevel.equals(CmdAccessLevel.ALL)) return this;
		if (accessLevel.isHigherThan(getAccessLevel(member)))
			throw new CheckException(bot.getEmbedUtil().getError(replyCallback, "errors.interaction.no_access", "Required access level: "+accessLevel.getName()));
		return this;
	}

	public CheckUtil moduleEnabled(IReplyCallback replyCallback, Guild guild, CmdModule module) throws CheckException {
		if (module == null)
			return this;
		if (bot.getDBUtil().getGuildSettings(guild).isDisabled(module)) 
			throw new CheckException(bot.getEmbedUtil().getError(replyCallback, "modules.module_disabled"));
		return this;
	}

	public CheckUtil hasPermissions(IReplyCallback replyCallback, Guild guild, Member member, Permission[] permissions) throws CheckException {
		return hasPermissions(replyCallback, guild, member, false, null, permissions);
	}

	public CheckUtil hasPermissions(IReplyCallback replyCallback, Guild guild, Member member, boolean isSelf, Permission[] permissions) throws CheckException {
		return hasPermissions(replyCallback, guild, member, isSelf, null, permissions);
	}

	public CheckUtil hasPermissions(IReplyCallback replyCallback, Guild guild, Member member, boolean isSelf, GuildChannel channel, Permission[] permissions) throws CheckException {
		if (permissions == null || permissions.length == 0)
			return this;
		if (guild == null || (!isSelf && member == null))
			return this;

		MessageCreateData msg = null;
		if (isSelf) {
			Member self = guild.getSelfMember();
			if (channel == null) {
				for (Permission perm : permissions) {
					if (!self.hasPermission(perm)) {
						msg = bot.getEmbedUtil().createPermError(replyCallback, perm, true);
						break;
					}
				}
			} else {
				for (Permission perm : permissions) {
					if (!self.hasPermission(channel, perm)) {
						msg = bot.getEmbedUtil().createPermError(replyCallback, channel, perm, true);
						break;
					}
				}
			}
		} else {
			if (channel == null) {
				for (Permission perm : permissions) {
					if (!member.hasPermission(perm)) {
						msg = bot.getEmbedUtil().createPermError(replyCallback, perm, false);
						break;
					}
				}
			} else {
				for (Permission perm : permissions) {
					if (!member.hasPermission(channel, perm)) {
						msg = bot.getEmbedUtil().createPermError(replyCallback, channel, perm, false);
						break;
					}
				}
			}
		}
		if (msg != null) {
			throw new CheckException(msg);
		}
		return this;
	}

}
