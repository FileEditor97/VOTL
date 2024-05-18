package dev.fileeditor.votl.commands.moderation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import dev.fileeditor.votl.App;
import dev.fileeditor.votl.base.command.CooldownScope;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CaseType;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.database.managers.CaseManager.CaseData;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class UnmuteCmd extends CommandBase {
	
	public UnmuteCmd(App bot) {
		super(bot);
		this.name = "unmute";
		this.path = "bot.moderation.unmute";
		this.options = List.of(
			new OptionData(OptionType.USER, "user", lu.getText(path+".user.help"), true),
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

		Guild guild = Objects.requireNonNull(event.getGuild());
		String reason = event.optString("reason", lu.getLocalized(event.getGuildLocale(), path+".no_reason"));
		
		CaseData muteData = bot.getDBUtil().cases.getMemberActive(tm.getIdLong(), guild.getIdLong(), CaseType.MUTE);
		if (muteData != null) bot.getDBUtil().cases.setInactive(muteData.getCaseId());

		if (tm.isTimedOut()) {
			tm.removeTimeout().reason(reason).queue(done -> {
				Member mod = event.getMember();
				// add info to db
				bot.getDBUtil().cases.add(CaseType.UNMUTE, tm.getIdLong(), tm.getUser().getName(), mod.getIdLong(), mod.getUser().getName(),
					guild.getIdLong(), reason, Instant.now(), null);
				CaseData unmuteData = bot.getDBUtil().cases.getMemberLast(tm.getIdLong(), guild.getIdLong());
				// log unban
				bot.getLogger().mod.onNewCase(guild, tm.getUser(), unmuteData, muteData != null ? muteData.getReason() : null);
				// reply
				editHookEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
					.setDescription(lu.getText(event, path+".success")
						.replace("{user_tag}", tm.getUser().getName())
						.replace("{reason}", reason))
					.build()
				);
			},
			failed -> {
				editError(event, path+".abort", failed.getMessage());
			});
		} else {
			editError(event, path+".not_muted");
		}
	}

}