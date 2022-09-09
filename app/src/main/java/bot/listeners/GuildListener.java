package bot.listeners;

import bot.App;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildListener extends ListenerAdapter {

	private final App bot;

	public GuildListener(App bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		String guildId = event.getGuild().getId();
		// check if not exists in DB and adds it
		if (bot.getDBUtil().guildAdd(guildId)) {
			bot.getLogger().info("Added guild '"+event.getGuild().getName()+"'("+guildId+") to db");
		}
		bot.getLogger().info("Joined guild '"+event.getGuild().getName()+"'("+guildId+")");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		String guildId = event.getGuild().getId();
		if (bot.getDBUtil().isGuild(guildId) || bot.getDBUtil().isGuildVoice(guildId)) {
			//deletes from db guild and guildSettings
			bot.getDBUtil().guildRemove(guildId);
			bot.getLogger().info("Removed guild '"+event.getGuild().getName()+"'("+guildId+") from db");
		}
		bot.getLogger().info("Left guild '"+event.getGuild().getName()+"'("+guildId+")");
	}
}
