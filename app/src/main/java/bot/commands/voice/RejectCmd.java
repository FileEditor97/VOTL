package bot.commands.voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import bot.App;
import bot.objects.CmdAccessLevel;
import bot.objects.constants.CmdCategory;
import bot.utils.exception.CheckException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@CommandInfo(
	name = "reject",
	description = "Withdraw the user permission to join your channel.",
	usage = "/reject <mention:user/-s role/-s>",
	requirements = "Must have created voice channel"
)
public class RejectCmd extends SlashCommand {
	
	private final App bot;
	
	private static final boolean mustSetup = true;
	private static final String MODULE = "voice";
	private static final CmdAccessLevel ACCESS_LEVEL = CmdAccessLevel.ALL;

	protected static Permission[] userPerms = new Permission[0];
	protected static Permission[] botPerms = new Permission[0];

	public RejectCmd(App bot) {
		this.name = "reject";
		this.help = bot.getMsg("bot.voice.reject.help");
		this.category = CmdCategory.VOICE;
		RejectCmd.botPerms = new Permission[]{Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MOVE_OTHERS};
		this.options = Collections.singletonList(
			new OptionData(OptionType.STRING, "mention", bot.getMsg("bot.voice.reject.option_description"))
				.setRequired(true)
		);
		this.bot = bot;
	}

	@Override
	protected void execute(SlashCommandEvent event) {

		event.deferReply(true).queue(
			hook -> {
				try {
					// check access
					bot.getCheckUtil().hasAccess(event, ACCESS_LEVEL)
					// check module enabled
						.moduleEnabled(event, MODULE)
					// check user perms
						.hasPermissions(event.getTextChannel(), event.getMember(), userPerms)
					// check bots perms
						.hasPermissions(event.getTextChannel(), event.getMember(), true, botPerms);
					// check setup
					if (mustSetup) {
						bot.getCheckUtil().guildExists(event);
					}
				} catch (CheckException ex) {
					hook.editOriginal(ex.getEditData()).queue();
					return;
				}

				Mentions filMentions = event.getOption("mention", OptionMapping::getMentions);
				sendReply(event, hook, filMentions);
			}
		);

	}

	@SuppressWarnings("null")
	private void sendReply(SlashCommandEvent event, InteractionHook hook, Mentions filMentions) {

		Member author = Objects.requireNonNull(event.getMember());
		String authorId = author.getId();

		if (!bot.getDBUtil().isVoiceChannel(authorId)) {
			hook.editOriginal(bot.getEmbedUtil().getError(event, "errors.no_channel")).queue();
			return;
		}

		Guild guild = Objects.requireNonNull(event.getGuild());
		String guildId = guild.getId();

		VoiceChannel vc = guild.getVoiceChannelById(bot.getDBUtil().channelGetChannel(authorId));

		List<Member> members = filMentions.getMembers();
		List<Role> roles = filMentions.getRoles();
		if (members.isEmpty() & roles.isEmpty()) {
			hook.editOriginal(bot.getEmbedUtil().getError(event, "bot.voice.reject.invalid_args")).queue();
			return;
		}
		if (members.contains(author) || members.contains(guild.getSelfMember())) {
			hook.editOriginal(bot.getEmbedUtil().getError(event, "bot.voice.reject.not_self")).queue();
			return;
		}

		List<String> mentionStrings = new ArrayList<>();

		for (Member member : members) {
			try {
				vc.getManager().putPermissionOverride(member, null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
				mentionStrings.add(member.getEffectiveName());
			} catch (InsufficientPermissionException ex) {
				hook.editOriginal(bot.getEmbedUtil().getPermError(event.getTextChannel(), author, Permission.MANAGE_PERMISSIONS, true)).queue();
				return;
			}
			if (vc.getMembers().contains(member)) {
				guild.kickVoiceMember(member).queue();
			}
		}

		for (Role role : roles) {
			if (!role.hasPermission(new Permission[]{Permission.ADMINISTRATOR, Permission.MANAGE_SERVER, Permission.MANAGE_PERMISSIONS, Permission.MANAGE_ROLES}))
				try {
					vc.getManager().putPermissionOverride(role, null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
					mentionStrings.add(role.getName());
				} catch (InsufficientPermissionException ex) {
					hook.editOriginal(bot.getEmbedUtil().getPermError(event.getTextChannel(), author, Permission.MANAGE_PERMISSIONS, true)).queue();
					return;
				}
		}

		hook.editOriginalEmbeds(
			bot.getEmbedUtil().getEmbed(author)
				.setDescription(bot.getMsg(guildId, "bot.voice.reject.done", "", mentionStrings))
				.build()
		).queue();
	}
}
