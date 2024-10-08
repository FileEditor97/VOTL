package dev.fileeditor.votl.commands.games;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CaseType;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.database.managers.CaseManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.TimeFormat;

public class GameStrikeCmd extends CommandBase {

	private final long denyPerms = Permission.getRaw(Permission.MESSAGE_SEND, Permission.MESSAGE_SEND_IN_THREADS, Permission.MESSAGE_ADD_REACTION, Permission.CREATE_PUBLIC_THREADS);

	public GameStrikeCmd() {
		this.name = "gamestrike";
		this.path = "bot.games.gamestrike";
		this.options = List.of(
			new OptionData(OptionType.CHANNEL, "channel", lu.getText(path+".channel.help"), true)
				.setChannelTypes(ChannelType.TEXT),
			new OptionData(OptionType.USER, "user", lu.getText(path+".user.help"), true),
			new OptionData(OptionType.STRING, "reason", lu.getText(path+".reason.help"), true)
				.setMaxLength(200)
		);
		this.category = CmdCategory.GAMES;
		this.module = CmdModule.GAMES;
		this.accessLevel = CmdAccessLevel.MOD;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		GuildChannel channel = event.optGuildChannel("channel");
		if (bot.getDBUtil().games.getMaxStrikes(channel.getIdLong()) == null) {
			createError(event, path+".not_found", "Channel: %s".formatted(channel.getAsMention()));
			return;
		}
		Member tm = event.optMember("user");
		if (tm == null || tm.getUser().isBot() || tm.equals(event.getMember())
			|| tm.equals(event.getGuild().getSelfMember())
			|| bot.getCheckUtil().hasHigherAccess(tm, event.getMember())) {
			createError(event, path+".not_member");
			return;
		}

		long channelId = channel.getIdLong();
		int strikeCooldown = bot.getDBUtil().getGuildSettings(event.getGuild()).getStrikeCooldown();
		if (strikeCooldown > 0) {
			Instant lastUpdate = bot.getDBUtil().games.getLastUpdate(channelId, tm.getIdLong());
			if (lastUpdate != null && lastUpdate.isAfter(Instant.now().minus(strikeCooldown, ChronoUnit.MINUTES))) {
				// Cooldown between strikes
				editHookEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_FAILURE)
					.setDescription(lu.getText(event, path+".cooldown").formatted(TimeFormat.RELATIVE.format(lastUpdate.plus(strikeCooldown, ChronoUnit.MINUTES))))
					.build()
				);
				return;
			}
		}

		String reason = event.optString("reason");
		// Add to DB
		long guildId = event.getGuild().getIdLong();
		bot.getDBUtil().cases.add(CaseType.GAME_STRIKE, tm.getIdLong(), tm.getUser().getName(), event.getUser().getIdLong(), event.getUser().getName(),
			guildId, reason, Instant.now(), null);
		CaseManager.CaseData strikeData = bot.getDBUtil().cases.getMemberLast(tm.getIdLong(), guildId);
		bot.getDBUtil().games.addStrike(guildId, channelId, tm.getIdLong());
		// Inform user
		tm.getUser().openPrivateChannel().queue(pm -> {
			MessageEmbed embed = bot.getModerationUtil().getGameStrikeEmbed(channel, event.getUser(), reason);
			if (embed == null) return;
			pm.sendMessageEmbeds(embed).queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER));
		});
		// Log
		final int strikeCount = bot.getDBUtil().games.countStrikes(channelId, tm.getIdLong());
		final int maxStrikes = bot.getDBUtil().games.getMaxStrikes(channelId);
		bot.getLogger().mod.onNewCase(event.getGuild(), tm.getUser(), strikeData, strikeCount+"/"+maxStrikes);
		// Reply
		createReplyEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
			.setDescription(lu.getText(event, path+".done").formatted(tm.getAsMention(), channel.getAsMention()))
			.setFooter("#"+strikeData.getCaseId())
			.build());
		// Check if reached limit
		if (strikeCount >= maxStrikes) {
			try {
				channel.getPermissionContainer().upsertPermissionOverride(tm).setDenied(denyPerms).reason("Game ban").queue();
			} catch (InsufficientPermissionException ignored) {}
		}
	}
}