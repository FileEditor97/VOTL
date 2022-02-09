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
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

@CommandInfo(
	name = "permit",
	description = "Gives the user permission to join your channel",
	usage = "{prefix}permit <user/-s by ID or mention>",
	requirements = "Must have created voice channel"
)
public class PermitCmd extends Command {
	
	private final App bot;

	protected Permission[] botPerms;

	public PermitCmd(App bot) {
		this.name = "permit";
		this.help = "Gives the user permission to join your channel";
		this.category = new Category("voice");
		this.botPerms = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS};
		this.bot = bot;
	}

	@Override
	protected void execute(CommandEvent event) {
		for (Permission perm : botPerms) {
			if (!event.getSelfMember().hasPermission(event.getTextChannel(), perm)) {
				bot.getEmbedUtil().sendPermError(event.getTextChannel(), event.getMember(), perm, true);
				return;
			}
		}

		if (!bot.getDBUtil().isGuild(event.getGuild().getId())) {
			bot.getEmbedUtil().sendError(event.getTextChannel(), event.getMember(), "errors.voice_not_setup");
			return;
		}

		if (bot.getDBUtil().isVoiceChannel(event.getMember().getId())) {
			VoiceChannel vc = event.getGuild().getVoiceChannelById(bot.getDBUtil().channelGetChannel(event.getMember().getId()));

			List<Member> members = event.getMessage().getMentionedMembers(event.getGuild());
			if (members.isEmpty()) {
				bot.getEmbedUtil().sendError(event.getTextChannel(), event.getMember(), "bot.voice.permit.no_args");
				return;
			}
			if (members.contains(event.getMember())) {
				bot.getEmbedUtil().sendError(event.getTextChannel(), event.getMember(), "bot.voice.permit.not_self");
				return;
			}

			for (Member member : members) {
				try {
					vc.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(Permission.VOICE_CONNECT), null).queue();
				} catch (InsufficientPermissionException ex) {
					bot.getEmbedUtil().sendPermError(event.getTextChannel(), event.getMember(), Permission.MANAGE_PERMISSIONS, true);
					return;
				}
			}
			event.reply(bot.getMsg(event.getGuild().getId(), "bot.voice.permit.done", "",
				members.stream().map(object -> Objects.toString(object.getEffectiveName(), null)).collect(Collectors.toList())
				));
		} else {
			event.reply(bot.getMsg(event.getGuild().getId(), "bot.voice.permit.no_channel"));
		}
	}
}