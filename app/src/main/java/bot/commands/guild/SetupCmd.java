package bot.commands.guild;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import bot.App;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

@CommandInfo
(
	name = "setup",
	description = "Setup menu for this server.",
	usage = "/setup <select>",
	requirements = "Have 'Manage Server' permission"
)
public class SetupCmd extends SlashCommand {

	protected static Permission[] userPerms;

	public SetupCmd(App bot) {
		this.name = "setup";
		this.help = bot.getMsg("0", "bot.guild.setup.description");;
		this.category = new Category("guild");
		SetupCmd.userPerms = new Permission[]{Permission.MANAGE_SERVER};
		this.children = new SlashCommand[]{new Voice(bot)};
	}

	@Override
	protected void execute(SlashCommandEvent event) {

	}

	private static class Voice extends SlashCommand {

		private final App bot;

		protected Permission[] botPerms;

		public Voice(App bot) {
			this.name = "voice";
			this.help = bot.getMsg("0", "bot.guild.setup.values.voice");
			this.botPerms = new Permission[]{Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MOVE_OTHERS}; // Permission.MESSAGE_EMBED_LINKS
			this.bot = bot;
		}

		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue(
				hook -> {
					MessageEditData reply = getReply(event);

					hook.editOriginal(reply).queue();
				}
			);
		}

		private MessageEditData getReply(SlashCommandEvent event) {
			
			MessageCreateData permission = bot.getCheckUtil().lacksPermissions(event.getTextChannel(), event.getMember(), true, botPerms);
			if (permission != null)
				return MessageEditData.fromCreateData(permission);

			permission = bot.getCheckUtil().lacksPermissions(event.getTextChannel(), event.getMember(), userPerms);
			if (permission != null)
				return MessageEditData.fromCreateData(permission);

			String guildID = event.getGuild().getId();

			if (bot.getDBUtil().guildAdd(guildID)) {
				bot.getLogger().info("Added guild through setup '"+event.getGuild().getName()+"'("+guildID+") to db");
			}

			List<MessageEditBuilder> msgBuilder = new ArrayList<MessageEditBuilder>();
			msgBuilder.add(new MessageEditBuilder()); // I hate this, but I have no other way to do this, without errors

			try {
				event.getGuild().createCategory(bot.getMsg(guildID, "bot.voice.setup.category"))
					.addRolePermissionOverride(event.getGuild().getRoleByBot(event.getJDA().getSelfUser().getId()).getIdLong(),
						EnumSet.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MOVE_OTHERS),
						null)
					.queue(
						category -> {
							try {
								category.createVoiceChannel(bot.getMsg(guildID, "bot.voice.setup.channel"))
									.syncPermissionOverrides()
									.addMemberPermissionOverride(event.getJDA().getSelfUser().getIdLong(),
										EnumSet.of(Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MOVE_OTHERS),
										null)
									.addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VOICE_SPEAK))
									.queue(
										channel -> {
											bot.getDBUtil().guildVoiceSetup(guildID, category.getId(), channel.getId());
											msgBuilder.set(0, msgBuilder.get(0).setEmbeds(
												bot.getEmbedUtil().getEmbed(event.getMember())
													.setDescription(bot.getMsg(guildID, "bot.voice.setup.done").replace("{channel}", channel.getAsMention()))
													.build()
											));
										}
									);
							} catch (InsufficientPermissionException ex) {
								msgBuilder.set(0, MessageEditBuilder.fromCreateData(bot.getEmbedUtil().getPermError(event.getTextChannel(), event.getMember(), ex.getPermission(), true)));
							}
						}
					);
			} catch (InsufficientPermissionException ex) {
				msgBuilder.set(0, MessageEditBuilder.fromCreateData(bot.getEmbedUtil().getPermError(event.getTextChannel(), event.getMember(), ex.getPermission(), true)));
				ex.printStackTrace();
			}

			return msgBuilder.get(0).build();
			
		}

	}

}
