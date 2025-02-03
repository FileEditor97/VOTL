package dev.fileeditor.votl.commands.level;

import dev.fileeditor.votl.base.command.CooldownScope;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.ExpType;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.utils.database.managers.LevelManager;
import dev.fileeditor.votl.utils.encoding.EncodingUtil;
import dev.fileeditor.votl.utils.imagegen.UserBackground;
import dev.fileeditor.votl.utils.imagegen.UserBackgroundHandler;
import dev.fileeditor.votl.utils.imagegen.renders.UserProfileRender;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class UserProfileCmd extends CommandBase {
	public UserProfileCmd() {
		this.name = "profile";
		this.path = "bot.level.profile";
		this.category = CmdCategory.LEVELS;
		this.module = CmdModule.LEVELS;
		this.options = List.of(
			new OptionData(OptionType.USER, "user", lu.getText(path+".user.help")),
			new OptionData(OptionType.INTEGER, "id", lu.getText(path+".id.help"))
				.setRequiredRange(0, 20)
				.addChoice("Dark", 0)
				.addChoice("Light", 1)
				.addChoice("Mountains", 2)
		);
		this.cooldown = 120;
		this.cooldownScope = CooldownScope.USER;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.deferReply().queue();

		Member target = event.optMember("user", event.getMember());
		if (target == null || target.getUser().isBot()) {
			editError(event, path+".bad_user");
			return;
		}

		int selectedBackgroundId = event.optInteger("id", 0);

		sendBackgroundMessage(event, target, selectedBackgroundId);
	}

	private void sendBackgroundMessage(
		SlashCommandEvent event,
		Member target,
		int backgroundId
	) {
		UserBackground background = UserBackgroundHandler.getInstance().fromId(backgroundId);
		if (background == null) {
			editError(event, path+".failed", "Background not found");
			return;
		}

		long guildId = target.getGuild().getIdLong();
		long userId = target.getIdLong();

		UserProfileRender render = new UserProfileRender(target)
			.setLocale(bot.getLocaleUtil(), event.getUserLocale())
			.setBackground(background);

		// Experience
		LevelManager.PlayerData playerData = bot.getDBUtil().levels.getPlayer(guildId, userId);

		long textExp = playerData.getExperience(ExpType.TEXT);
		long voiceExp = playerData.getExperience(ExpType.VOICE);

		int textLevel = bot.getLevelUtil().getLevelFromExperience(textExp);
		int voiceLevel = bot.getLevelUtil().getLevelFromExperience(voiceExp);

		long textMinXpInLevel = bot.getLevelUtil().getExperienceFromLevel(textLevel);
		long voiceMinXpInLevel = bot.getLevelUtil().getExperienceFromLevel(voiceLevel);

		long textXpDiff = bot.getLevelUtil().getExperienceFromLevel(textLevel + 1) - textMinXpInLevel;
		long voiceXpDiff = bot.getLevelUtil().getExperienceFromLevel(voiceLevel + 1) - voiceMinXpInLevel;

		Integer textRank = bot.getDBUtil().levels.getServerRank(guildId, userId, ExpType.TEXT);
		Integer voiceRank = bot.getDBUtil().levels.getServerRank(guildId, userId, ExpType.VOICE);

		long globalExperience = bot.getDBUtil().levels.getSumGlobalExp(userId);

		render.setLevel(textLevel, voiceLevel)
			.setTotalExperience(textExp, voiceExp)
			.setXpDiff(textXpDiff, voiceXpDiff)
			.setPercentage(
				((double) (textExp - textMinXpInLevel) / textXpDiff) * 100,
				((double) (voiceExp - voiceMinXpInLevel) / voiceXpDiff) * 100
			)
			.setCurrentLevelExperience(textExp - textMinXpInLevel, voiceExp - voiceMinXpInLevel)
			.setServerRank(
				textRank==null?"-":String.valueOf(textRank),
				voiceRank==null?"-":String.valueOf(voiceRank)
			)
			.setGlobalExperience(globalExperience);

		// Send
		final String attachmentName = EncodingUtil.encodeUserBg(guildId, userId);

		EmbedBuilder embed = new EmbedBuilder()
			.setImage("attachment://" + attachmentName)
			.setColor(bot.getDBUtil().getGuildSettings(event.getGuild()).getColor());

		try {
			event.getHook().editOriginalEmbeds(embed.build()).setFiles(FileUpload.fromData(
				new ByteArrayInputStream(render.renderToBytes()),
				attachmentName
			)).queue();
		} catch (IOException e) {
			bot.getAppLogger().error("Failed to generate the rank background: {}", e.getMessage(), e);
			editError(event, path+".failed", "Rendering exception");
		}
	}
}
