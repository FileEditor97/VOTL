package votl.utils;

import java.util.Objects;

import votl.App;
import votl.objects.CmdAccessLevel;
import votl.objects.CmdModule;
import votl.objects.command.CommandClient;
import votl.objects.command.SlashCommandEvent;
import votl.objects.constants.Constants;
import votl.utils.exception.CheckException;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class CheckUtil {

	private final App bot;

	public CheckUtil(App bot) {
		this.bot = bot;
	}

	public boolean isDeveloper(User user) {
		return user.getId().equals(Constants.DEVELOPER_ID);
	}

	public boolean isOwner(CommandClient client, User user) {
    	if (user.getId().equals(client.getOwnerId()))
    	    return true;
        if (client.getCoOwnerIds()==null)
            return false;
        for (String id : client.getCoOwnerIds())
            if (id.equals(user.getId()))
                return true;
        return false;
    }

	public CmdAccessLevel getAccessLevel(CommandClient client, Member member) {
		// Is bot developer
		if (isDeveloper(member.getUser()) || isOwner(client, member.getUser()))
			return CmdAccessLevel.DEV;
		// Is guild owner
		if (member.isOwner())
			return CmdAccessLevel.OWNER;
		
		Guild guild = Objects.requireNonNull(member.getGuild());
		String access = bot.getDBUtil().access.hasAccess(guild.getId(), member.getId());
		// Has either mod or admin access
		if (access != null) {
			// Has admin access
			if (access.equals("admin"))
				return CmdAccessLevel.ADMIN;
			return CmdAccessLevel.MOD;
		}
		// Default
		return CmdAccessLevel.ALL;
	}

	public Boolean hasHigherAccess(CommandClient client, Member who, Member than) {
		return getAccessLevel(client, who).getLevel() > getAccessLevel(client, than).getLevel();
	}

	public Boolean hasAccess(SlashCommandEvent event, CmdAccessLevel accessLevel) {
		if (getAccessLevel(event.getClient(), event.getMember()).getLevel() >= accessLevel.getLevel()) {
			return true;
		}
		return false;
	}

	public <T> CheckUtil hasAccess(T event, CommandClient client, Member member, CmdAccessLevel accessLevel) throws CheckException {
		if (accessLevel.getLevel() > getAccessLevel(client, member).getLevel())
			throw new CheckException(bot.getEmbedUtil().getError(event, "errors.access_level_low", "Access: "+accessLevel.getName()));
		return this;
	}

	public <T> CheckUtil guildExists(T event, Guild guild) throws CheckException {
		if (!bot.getDBUtil().guild.exists(guild.getId()))
			throw new CheckException(bot.getEmbedUtil().getError(event, "errors.guild_not_setup"));
		return this;
	}

	public <T> CheckUtil moduleEnabled(T event, Guild guild, CmdModule module) throws CheckException {
		if (module == null)
			return this;
		if (bot.getDBUtil().module.isDisabled(guild.getId(), module)) 
			throw new CheckException(bot.getEmbedUtil().getError(event, "modules.module_disabled"));
		return this;
	}

	public <T> CheckUtil hasPermissions(T genericEvent, Guild guild, Member member, Permission[] permissions) throws CheckException {
		return hasPermissions(genericEvent, guild, member, false, null, permissions);
	}

	public <T> CheckUtil hasPermissions(T genericEvent, Guild guild, Member member, boolean isSelf, Permission[] permissions) throws CheckException {
		return hasPermissions(genericEvent, guild, member, isSelf, null, permissions);
	}

	public <T> CheckUtil hasPermissions(T event, Guild guild, Member member, boolean isSelf, GuildChannel channel, Permission[] permissions) throws CheckException {
		if (permissions == null || permissions.length == 0)
			return this;
		if (guild == null || member == null)
			return this;

		MessageCreateData msg = null;
		if (isSelf) {
			Member self = guild.getSelfMember();
			if (channel == null) {
				for (Permission perm : permissions) {
					if (!self.hasPermission(perm)) {
						msg = bot.getEmbedUtil().createPermError(event, member, perm, true);
						break;
					}
				}
			} else {
				for (Permission perm : permissions) {
					if (!self.hasPermission(channel, perm)) {
						msg = bot.getEmbedUtil().createPermError(event, member, channel, perm, true);
						break;
					}
				}
			}
		} else {
			if (channel == null) {
				for (Permission perm : permissions) {
					if (!member.hasPermission(perm)) {
						msg = bot.getEmbedUtil().createPermError(event, member, perm, false);
						break;
					}
				}
			} else {
				for (Permission perm : permissions) {
					if (!member.hasPermission(channel, perm)) {
						msg = bot.getEmbedUtil().createPermError(event, member, channel, perm, false);
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
