package dev.fileeditor.votl.commands.moderation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import dev.fileeditor.votl.base.command.CooldownScope;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CaseType;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.database.managers.CaseManager.CaseData;
import dev.fileeditor.votl.utils.exception.FormatterException;
import dev.fileeditor.votl.utils.message.TimeUtil;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class MuteCmd extends CommandBase {
	
	public MuteCmd() {
		this.name = "mute";
		this.path = "bot.moderation.mute";
		this.options = List.of(
			new OptionData(OptionType.USER, "user", lu.getText(path+".user.help"), true),
			new OptionData(OptionType.STRING, "time", lu.getText(path+".time.help"), true),
			new OptionData(OptionType.STRING, "reason", lu.getText(path+".reason.help")).setMaxLength(400)
		);
		this.botPermissions = new Permission[]{Permission.MODERATE_MEMBERS};
		this.category = CmdCategory.MODERATION;
		this.module = CmdModule.MODERATION;
		this.accessLevel = CmdAccessLevel.MOD;
		this.cooldown = 5;
		this.cooldownScope = CooldownScope.GUILD;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.deferReply().queue();

		Member tm = event.optMember("user");
		if (tm == null) {
			editError(event, path+".not_found");
			return;
		}
		if (event.getUser().equals(tm.getUser()) || event.getJDA().getSelfUser().equals(tm.getUser())) {
			editError(event, path+".not_self");
			return;
		}

		final Duration duration;
		try {
			duration = TimeUtil.stringToDuration(event.optString("time"), false);
		} catch (FormatterException ex) {
			editError(event, ex.getPath());
			return;
		}
		if (duration.isZero()) {
			editError(event, path+".abort", "Duration must larger than 1 minute");
			return;
		}

		Guild guild = Objects.requireNonNull(event.getGuild());
		String reason = event.optString("reason", lu.getLocalized(event.getGuildLocale(), path+".no_reason"));
		CaseData caseData = bot.getDBUtil().cases.getMemberActive(tm.getIdLong(), guild.getIdLong(), CaseType.MUTE);

		if (tm.isTimedOut() && caseData != null) {
			// Case already exists, change duration
			editHookEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_WARNING)
				.setDescription(lu.getText(event, path+".already_muted").formatted(caseData.getCaseId()))
				.addField(lu.getText(event, "logger.moderation.mute.short_title"), lu.getText(event, "logger.moderation.mute.short_info")
					.replace("{username}", tm.getAsMention())
					.replace("{until}", TimeUtil.formatTime(tm.getTimeOutEnd(), false))
					, false)
				.build()
			);
		} else {
			// No case -> override current timeout
			// No case and not timed out -> timeout
			Member mod = event.getMember();
			if (!guild.getSelfMember().canInteract(tm)) {
				editError(event, path+".abort", "Bot can't interact with target member.");
				return;
			}
			if (bot.getCheckUtil().hasHigherAccess(tm, mod)) {
				editError(event, path+".higher_access");
				return;
			}
			if (!mod.canInteract(tm)) {
				editError(event, path+".abort", "You can't interact with target member.");
				return;
			}
			
			tm.timeoutFor(duration).reason(reason).queue(done -> {
				tm.getUser().openPrivateChannel().queue(pm -> {
					MessageEmbed embed = bot.getModerationUtil().getDmEmbed(CaseType.MUTE, guild, reason, duration, mod.getUser(), false);
					if (embed == null) return;
					pm.sendMessageEmbeds(embed).queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER));
				});

				// Set previous mute case inactive, as member is not timeout
				if (caseData != null) bot.getDBUtil().cases.setInactive(caseData.getCaseId());
				// add info to db
				bot.getDBUtil().cases.add(CaseType.MUTE, tm.getIdLong(), tm.getUser().getName(), mod.getIdLong(), mod.getUser().getName(),
					guild.getIdLong(), reason, Instant.now(), duration);
				CaseData muteDate = bot.getDBUtil().cases.getMemberLast(tm.getIdLong(), guild.getIdLong());
				// log mute
				bot.getLogger().mod.onNewCase(guild, tm.getUser(), muteDate);
				
				// send embed
				editHookEmbed(event, bot.getModerationUtil().actionEmbed(guild.getLocale(), muteDate.getCaseId(),
					path+".success", tm.getUser(), mod.getUser(), reason, duration)
				);
			},
			failed -> editError(event, "errors.error", failed.getMessage()));
		}
	}

}
