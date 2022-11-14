package votl.commands.voice;

import java.util.Objects;

import votl.App;
import votl.commands.CommandBase;
import votl.objects.CmdModule;
import votl.objects.command.SlashCommandEvent;
import votl.objects.constants.CmdCategory;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;

import com.jagrosh.jdautilities.doc.standard.CommandInfo;

@CommandInfo(
	name = "ghost",
	description = "Hides voice channel from @everyone",
	usage = "/ghost",
	requirements = "Must have created voice channel"
)
public class GhostCmd extends CommandBase {

	public GhostCmd(App bot) {
		super(bot);
		this.name = "ghost";
		this.path = "bot.voice.ghost";
		this.botPermissions = new Permission[]{Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VIEW_CHANNEL};
		this.category = CmdCategory.VOICE;
		this.module = CmdModule.VOICE;
		this.mustSetup = true;
	}

	@Override
	protected void execute(SlashCommandEvent event) {

		event.deferReply(true).queue(
			hook -> {
				sendReply(event, hook);
			}
		);
	}

	@SuppressWarnings("null")
	private void sendReply(SlashCommandEvent event, InteractionHook hook) {

		Member member = Objects.requireNonNull(event.getMember());

		if (!bot.getDBUtil().isVoiceChannel(member.getId())) {
			hook.editOriginal(bot.getEmbedUtil().getError(event, "errors.no_channel")).queue();
			return;
		}

		Guild guild = Objects.requireNonNull(event.getGuild());
		DiscordLocale userLocale = event.getUserLocale();

		VoiceChannel vc = guild.getVoiceChannelById(bot.getDBUtil().channelGetChannel(member.getId()));
		try {
			vc.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
		} catch (InsufficientPermissionException ex) {
			hook.editOriginal(bot.getEmbedUtil().getPermError(event, member, ex.getPermission(), true)).queue();
			return;
		}

		hook.editOriginalEmbeds(
			bot.getEmbedUtil().getEmbed(event)
				.setDescription(lu.getLocalized(userLocale, "bot.voice.ghost.done"))
				.build()
		).queue();
	}
}
