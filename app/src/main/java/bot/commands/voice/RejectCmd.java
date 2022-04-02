package bot.commands.voice;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import bot.App;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

@CommandInfo(
	name = "reject",
	description = "Withdraw the user permission to join your channel.",
	usage = "{prefix}reject <user/-s by ID or mention>",
	requirements = "Must have created voice channel"
)
public class RejectCmd extends Command {
	
	private final App bot;

	protected Permission[] botPerms;

	public RejectCmd(App bot) {
		this.name = "reject";
		this.help = "bot.voice.reject.description";
		this.category = new Category("voice");
		this.botPerms = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MOVE_OTHERS};
		this.bot = bot;
	}

	@Override
	protected void execute(CommandEvent event) {
		if (bot.getCheckUtil().lacksPermissions(event.getTextChannel(), event.getMember(), true, botPerms))
			return;

		if (!bot.getDBUtil().isGuildVoice(event.getGuild().getId())) {
			bot.getEmbedUtil().sendError(event.getEvent(), "errors.voice_not_setup");
			return;
		}

		if (bot.getDBUtil().isVoiceChannel(event.getMember().getId())) {
			VoiceChannel vc = event.getGuild().getVoiceChannelById(bot.getDBUtil().channelGetChannel(event.getMember().getId()));

			List<Member> members = event.getMessage().getMentionedMembers(event.getGuild());
			if (members.isEmpty()) {
				bot.getEmbedUtil().sendError(event.getEvent(), "bot.voice.reject.invalid_args");
				return;
			}
			if (members.contains(event.getMember())) {
				bot.getEmbedUtil().sendError(event.getEvent(), "bot.voice.reject.not_self");
				return;
			}

			for (Member member : members) {
				try {
					vc.getManager().putMemberPermissionOverride(member.getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
				} catch (InsufficientPermissionException ex) {
					bot.getEmbedUtil().sendPermError(event.getTextChannel(), event.getMember(), Permission.MANAGE_PERMISSIONS, true);
					return;
				}
				if (vc.getMembers().contains(member)) {
					event.getGuild().kickVoiceMember(member).queue();
				}
			}
			MessageEmbed embed = bot.getEmbedUtil().getEmbed(event.getMember())
				.setDescription(bot.getMsg(event.getGuild().getId(), "bot.voice.reject.done", "",
					members.stream().map(object -> Objects.toString(object.getEffectiveName(), null)).collect(Collectors.toList())
				))
				.build();
			event.reply(embed);
		} else {
			event.reply(bot.getMsg(event.getGuild().getId(), "bot.voice.reject.no_channel"));
		}
	}
}
