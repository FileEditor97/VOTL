package bot.commands.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import bot.App;
import bot.objects.CmdModule;
import bot.objects.command.SlashCommand;
import bot.objects.command.SlashCommandEvent;
import bot.objects.constants.CmdCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;

@CommandInfo(
	name = "perms",
	description = "View/Reset voice channel permissions",
	usage = "/perms <select>",
	requirements = "Must have created voice channel"
)
public class PermsCmd extends SlashCommand {

	public PermsCmd(App bot) {
		this.name = "perms";
		this.helpPath = "bot.voice.perms.help";
		this.children = new SlashCommand[]{new View(), new Reset()};
		this.bot = bot;
		this.category = CmdCategory.VOICE;
		this.module = CmdModule.VOICE;
		this.mustSetup = true;
	}

	@Override
	protected void execute(SlashCommandEvent event) {

	}

	private class View extends SlashCommand {

		public View() {
			this.name = "view";
			this.helpPath = "bot.voice.perms.view.help";
			this.botPermissions = new Permission[]{Permission.MANAGE_PERMISSIONS};
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

			Member author = Objects.requireNonNull(event.getMember());
			String authorId = author.getId();

			if (!bot.getDBUtil().isVoiceChannel(authorId)) {
				hook.editOriginal(bot.getEmbedUtil().getError(event, "errors.no_channel")).queue();
				return;
			}

			Guild guild = Objects.requireNonNull(event.getGuild());
			DiscordLocale userLocale = event.getUserLocale();
			
			VoiceChannel vc = guild.getVoiceChannelById(bot.getDBUtil().channelGetChannel(authorId));

			EmbedBuilder embedBuilder = bot.getEmbedUtil().getEmbed(event)
				.setTitle(lu.getLocalized(userLocale, "bot.voice.perms.view.embed.title").replace("{channel}", vc.getName()))
				.setDescription(lu.getLocalized(userLocale, "bot.voice.perms.view.embed.field")+"\n\n");

			//@Everyone
			PermissionOverride publicOverride = vc.getPermissionOverride(guild.getPublicRole());

			String view = contains(publicOverride, Permission.VIEW_CHANNEL);
			String join = contains(publicOverride, Permission.VOICE_CONNECT);
			
			embedBuilder = embedBuilder.appendDescription("> " + formatHolder(lu.getLocalized(userLocale, "bot.voice.perms.view.embed.everyone"), view, join))
				.appendDescription("\n\n" + lu.getLocalized(userLocale, "bot.voice.perms.view.embed.roles") + "\n");

			//Roles
			List<PermissionOverride> overrides = new ArrayList<>(vc.getRolePermissionOverrides()); // cause given override list is immutable
			try {
				overrides.remove(vc.getPermissionOverride(Objects.requireNonNull(guild.getBotRole()))); // removes bot's role
				overrides.remove(vc.getPermissionOverride(guild.getPublicRole())); // removes @everyone role
			} catch (NullPointerException ex) {
				bot.getLogger().warn("PermsCmd null pointer at role override remove");
			}
			
			if (overrides.isEmpty()) {
				embedBuilder.appendDescription(lu.getLocalized(userLocale, "bot.voice.perms.view.embed.none") + "\n");
			} else {
				for (PermissionOverride ov : overrides) {
					view = contains(ov, Permission.VIEW_CHANNEL);
					join = contains(ov, Permission.VOICE_CONNECT);

					embedBuilder.appendDescription("> " + formatHolder(ov.getRole().getName(), view, join) + "\n");
				}
			}
			embedBuilder.appendDescription("\n" + lu.getLocalized(userLocale, "bot.voice.perms.view.embed.members") + "\n");

			//Members
			overrides = new ArrayList<>(vc.getMemberPermissionOverrides());
			try {
				overrides.remove(vc.getPermissionOverride(author)); // removes user
				overrides.remove(vc.getPermissionOverride(guild.getSelfMember())); // removes bot
			} catch (NullPointerException ex) {
				bot.getLogger().warn("PermsCmd null pointer at member override remove");
			}

			EmbedBuilder embedBuilder2 = embedBuilder;
			List<PermissionOverride> ovs = overrides;

			guild.retrieveMembersByIds(false, overrides.stream().map(ov -> ov.getId()).toArray(String[]::new)).onSuccess(
				members -> {
					if (members.isEmpty()) {
						embedBuilder2.appendDescription(lu.getLocalized(userLocale, "bot.voice.perms.view.embed.none") + "\n");
					} else {
						for (PermissionOverride ov : ovs) {
							String view2 = contains(ov, Permission.VIEW_CHANNEL);
							String join2 = contains(ov, Permission.VOICE_CONNECT);

							Member find = members.stream().filter(m -> m.getId().equals(ov.getId())).findFirst().get(); 
							embedBuilder2.appendDescription("> " + formatHolder(find.getEffectiveName(), view2, join2) + "\n");
						}
					}

					hook.editOriginalEmbeds(embedBuilder2.build()).queue();
				}
			);

		}

		private String contains(PermissionOverride override, Permission perm) {
			if (override != null) {
				if (override.getAllowed().contains(perm))
					return "✅";
				else if (override.getDenied().contains(perm))
					return "❌";
			}
			return "▪️";
		}

		@Nonnull
		private String formatHolder(String holder, String view, String join) {
			return "`" + holder + "` | " + view + " | " + join;
		}
	}

	private class Reset extends SlashCommand {

		public Reset() {
			this.name = "reset";
			this.helpPath = "bot.voice.perms.reset.help";
			this.botPermissions = new Permission[]{Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT};
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
				vc.getManager().sync().queue();
			} catch (InsufficientPermissionException ex) {
				hook.editOriginal(bot.getEmbedUtil().getPermError(event, member, ex.getPermission(), true)).queue();
				return;
			}

			hook.editOriginalEmbeds(
				bot.getEmbedUtil().getEmbed(event)
					.setDescription(lu.getLocalized(userLocale, "bot.voice.perms.reset.done"))
					.build()
			).queue();
		}
	}
}
