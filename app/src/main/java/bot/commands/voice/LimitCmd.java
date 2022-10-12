package bot.commands.voice;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import bot.App;
import bot.objects.constants.CmdCategory;
import bot.utils.exception.CheckException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@CommandInfo(
	name = "Limit",
	description = "Sets limit for your channel.",
	usage = "/limit <limit:Integer from 0 to 99>",
	requirements = "Must have created voice channel"
)
public class LimitCmd extends SlashCommand {
	
	private static App bot;
	private static final String MODULE = "voice";

	protected static Permission[] botPerms;

	public LimitCmd(App bot) {
		this.name = "limit";
		this.help = bot.getMsg("bot.voice.limit.help");
		this.category = CmdCategory.VOICE;
		LimitCmd.botPerms = new Permission[]{Permission.MANAGE_CHANNEL};
		LimitCmd.bot = bot;
		this.children = new SlashCommand[]{new Set(), new Reset()};
	}

	@Override
	protected void execute(SlashCommandEvent event) {

	}

	private static class Set extends SlashCommand {

		public Set() {
			this.name = "set";
			this.help = bot.getMsg("bot.voice.limit.set.help");
			this.options = Collections.singletonList(
				new OptionData(OptionType.INTEGER, "limit", bot.getMsg("bot.voice.limit.set.option_description"))
					.setRequiredRange(0, 99)
					.setRequired(true)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {

			event.deferReply(true).queue(
				hook -> {
					try {
						Integer filLimit = event.getOption("limit", 0, OptionMapping::getAsInt);
						sendReply(event, hook, filLimit);
					} catch (Exception e) {
						hook.editOriginal(bot.getEmbedUtil().getError(event, "errors.request_error", e.toString())).queue();
					}
				}
			);

		}
	}

	private static class Reset extends SlashCommand {

		public Reset() {
			this.name = "reset";
			this.help = bot.getMsg("bot.voice.limit.reset.help");
		}

		@Override
		protected void execute(SlashCommandEvent event) {

			event.deferReply(true).queue(
				hook -> {
					Integer filLimit = Optional.ofNullable(bot.getDBUtil().guildVoiceGetLimit(Objects.requireNonNull(event.getGuild()).getId())).orElse(0);

					sendReply(event, hook, filLimit);
				}
			);

		}
	}

	@SuppressWarnings("null")
	private static void sendReply(SlashCommandEvent event, InteractionHook hook, Integer filLimit) {

		Member member = Objects.requireNonNull(event.getMember());
		Guild guild = Objects.requireNonNull(event.getGuild());
		String guildId = guild.getId();

		try {
			bot.getCheckUtil().moduleEnabled(event, guildId, MODULE)
				.hasPermissions(event.getTextChannel(), member, true, botPerms)
				.guildExists(event, guildId);
		} catch (CheckException ex) {
			hook.editOriginal(ex.getEditData());
			return;
		}

		String memberId = member.getId();

		if (bot.getDBUtil().isVoiceChannel(memberId)) {
			VoiceChannel vc = guild.getVoiceChannelById(bot.getDBUtil().channelGetChannel(memberId));

			vc.getManager().setUserLimit(filLimit).queue();
			
			if (!bot.getDBUtil().isUser(memberId)) {
				bot.getDBUtil().userAdd(memberId);
			}
			bot.getDBUtil().userSetLimit(memberId, filLimit);

			hook.editOriginalEmbeds(
				bot.getEmbedUtil().getEmbed(member)
					.setDescription(bot.getMsg(guildId, "bot.voice.limit.done").replace("{value}", filLimit.toString()))
					.build()
			).queue();
		} else {
			hook.editOriginal(bot.getEmbedUtil().getError(event, "errors.no_channel")).queue();
		}
	}
}
