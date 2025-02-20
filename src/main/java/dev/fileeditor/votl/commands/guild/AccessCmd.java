package dev.fileeditor.votl.commands.guild;

import java.util.List;
import java.util.Objects;

import dev.fileeditor.votl.base.command.SlashCommand;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

public class AccessCmd extends CommandBase {

	public AccessCmd() {
		this.name = "access";
		this.path = "bot.guild.access";
		this.children = new SlashCommand[]{new View(), new AddRole(), new RemoveRole(), new AddOperator(), new RemoveOperator()};
		this.category = CmdCategory.GUILD;
		this.accessLevel = CmdAccessLevel.ADMIN;
	}

	@Override
	protected void execute(SlashCommandEvent event) {}

	private class View extends SlashCommand {
		public View() {
			this.name = "view";
			this.path = "bot.guild.access.view";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();

			Guild guild = Objects.requireNonNull(event.getGuild());
			long guildId = guild.getIdLong();

			List<Long> exemptIds = bot.getDBUtil().access.getRoles(guildId, CmdAccessLevel.EXEMPT);
			List<Long> helperIds = bot.getDBUtil().access.getRoles(guildId, CmdAccessLevel.HELPER);
			List<Long> modIds = bot.getDBUtil().access.getRoles(guildId, CmdAccessLevel.MOD);
			List<Long> operatorIds = bot.getDBUtil().access.getOperators(guildId);

			EmbedBuilder embedBuilder = bot.getEmbedUtil().getEmbed()
				.setTitle(lu.getText(event, "bot.guild.access.view.embed.title"));

			if (exemptIds.isEmpty() && helperIds.isEmpty() && modIds.isEmpty() && operatorIds.isEmpty()) {
				editEmbed(event,
					embedBuilder.setDescription(
						lu.getText(event, "bot.guild.access.view.embed.none_found")
					).build()
				);
				return;
			}

			StringBuilder sb = new StringBuilder();

			sb.append(lu.getText(event, "bot.guild.access.view.embed.exempt")).append("\n");
			if (exemptIds.isEmpty()) sb.append("> %s\n".formatted(lu.getText(event, "bot.guild.access.view.embed.none")));
			else for (Long roleId : exemptIds) {
				Role role = guild.getRoleById(roleId);
				if (role == null) {
					if (bot.getDBUtil().access.removeRole(guildId, roleId)) {
						editErrorDatabase(event, "remove role");
						return;
					}
					continue;
				}
				sb.append("> %s `%s`\n".formatted(role.getAsMention(), roleId));
			}

			sb.append(lu.getText(event, "bot.guild.access.view.embed.helper")).append("\n");
			if (helperIds.isEmpty()) sb.append("> %s\n".formatted(lu.getText(event, "bot.guild.access.view.embed.none")));
			else for (Long roleId : helperIds) {
				Role role = guild.getRoleById(roleId);
				if (role == null) {
					if (bot.getDBUtil().access.removeRole(guildId, roleId)) {
						editErrorDatabase(event, "remove role");
						return;
					}
					continue;
				}
				sb.append("> %s `%s`\n".formatted(role.getAsMention(), roleId));
			}

			sb.append(lu.getText(event, "bot.guild.access.view.embed.mod")).append("\n");
			if (modIds.isEmpty()) sb.append("> %s".formatted(lu.getText(event, "bot.guild.access.view.embed.none")));
			else for (Long roleId : modIds) {
				Role role = guild.getRoleById(roleId);
				if (role == null) {
					if (bot.getDBUtil().access.removeRole(guildId, roleId)) {
						editErrorDatabase(event, "remove role");
						return;
					}
					continue;
				}
				sb.append("> %s `%s`\n".formatted(role.getAsMention(), roleId));
			}

			sb.append("\n").append(lu.getText(event, "bot.guild.access.view.embed.operator")).append("\n");
			if (operatorIds.isEmpty()) sb.append("> %s".formatted(lu.getText(event, "bot.guild.access.view.embed.none")));
			else for (Long userId : operatorIds) {
				UserSnowflake user = User.fromId(userId);
				sb.append("> %s `%s`\n".formatted(user.getAsMention(), userId));
			}

			embedBuilder.setDescription(sb);
			editEmbed(event, embedBuilder.build());
		}
	}

	private class AddRole extends SlashCommand {
		public AddRole() {
			this.name = "role";
			this.path = "bot.guild.access.add.role";
			this.options = List.of(
				new OptionData(OptionType.ROLE, "role", lu.getText(path+".role.help"), true),
				new OptionData(OptionType.INTEGER, "access_level", lu.getText(path+".access_level.help"), true)
					.addChoice("Ban Exemption", CmdAccessLevel.EXEMPT.getLevel())
					.addChoice("Helper", CmdAccessLevel.HELPER.getLevel())
					.addChoice("Moderator", CmdAccessLevel.MOD.getLevel())
			);
			this.subcommandGroup = new SubcommandGroupData("add", lu.getText("bot.guild.access.add.help"));
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();

			Role role = event.optRole("role");
			if (role == null) {
				editError(event, "bot.guild.access.add.no_role");
				return;
			}

			long roleId = role.getIdLong();
			Guild guild = event.getGuild();

			if (role.isPublicRole() || role.isManaged() || !guild.getSelfMember().canInteract(role) || role.hasPermission(Permission.ADMINISTRATOR)) {
				editError(event, "bot.guild.access.add.incorrect_role");
				return;
			}
			if (bot.getDBUtil().access.isRole(roleId)) {
				editError(event, "bot.guild.access.add.role.already");
				return;
			}

			CmdAccessLevel level = CmdAccessLevel.byLevel(event.optInteger("access_level"));
			if (bot.getDBUtil().access.addRole(guild.getIdLong(), roleId, level)) {
				editErrorDatabase(event, "add role");
				return;
			}

			// Log
			bot.getLogger().botLogs.onAccessAdded(guild, event.getUser(), null, role, level);
			// Send reply
			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, "bot.guild.access.add.role.done")
					.replace("{role}", role.getAsMention())
					.replace("{level}", level.getName())
				)
				.build()
			);
		}
	}

	private class RemoveRole extends SlashCommand {
		public RemoveRole() {
			this.name = "role";
			this.path = "bot.guild.access.remove.role";
			this.options = List.of(
				new OptionData(OptionType.ROLE, "role", lu.getText(path+".role.help"), true)
			);
			this.subcommandGroup = new SubcommandGroupData("remove", lu.getText("bot.guild.access.remove.help"));
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();

			Role role = event.optRole("role");
			if (role == null) {
				editError(event, "bot.guild.access.remove.no_role");
				return;
			}

			long roleId = role.getIdLong();

			CmdAccessLevel level = bot.getDBUtil().access.getRoleLevel(roleId);
			if (level.equals(CmdAccessLevel.ALL)) {
				editError(event, "bot.guild.access.remove.role.no_access");
			}

			if (bot.getDBUtil().access.removeRole(event.getGuild().getIdLong(), roleId)) {
				editErrorDatabase(event, "remove role");
				return;
			}

			// Log
			bot.getLogger().botLogs.onAccessRemoved(event.getGuild(), event.getUser(), null, role, level);
			// Send reply
			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, "bot.guild.access.remove.role.done")
					.replace("{role}", role.getAsMention())
					.replace("{level}", level.getName())
				)
				.build()
			);
		}
	}

	private class AddOperator extends SlashCommand {
		public AddOperator() {
			this.name = "operator";
			this.path = "bot.guild.access.add.operator";
			this.options = List.of(
				new OptionData(OptionType.USER, "user", lu.getText(path+".user.help"), true)
			);
			this.subcommandGroup = new SubcommandGroupData("add", lu.getText("bot.guild.access.add.help"));
			this.accessLevel = CmdAccessLevel.OWNER;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();

			Member member = event.optMember("user");
			if (member == null) {
				editError(event, "bot.guild.access.add.no_member");
				return;
			}
			if (member.isOwner() || member.getUser().isBot()) {
				editError(event, "bot.guild.access.add.incorrect_user");
				return;
			}

			long userId = member.getIdLong();
			long guildId = event.getGuild().getIdLong();
			if (bot.getDBUtil().access.isOperator(guildId, userId)) {
				editError(event, "bot.guild.access.add.user_already");
				return;
			}

			if (bot.getDBUtil().access.addOperator(guildId, userId)) {
				editErrorDatabase(event, "add operator");
				return;
			}
			
			// Log
			bot.getLogger().botLogs.onAccessAdded(event.getGuild(), event.getUser(), member.getUser(), null, CmdAccessLevel.OPERATOR);
			// Send reply
			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, "bot.guild.access.add.operator.done").replace("{user}", member.getAsMention()))
				.build()
			);
		}
	}

	private class RemoveOperator extends SlashCommand {
		public RemoveOperator() {
			this.name = "operator";
			this.path = "bot.guild.access.remove.operator";
			this.options = List.of(
				new OptionData(OptionType.USER, "user", lu.getText(path+".user.help"), true)
			);
			this.subcommandGroup = new SubcommandGroupData("remove", lu.getText("bot.guild.access.remove.help"));
			this.accessLevel = CmdAccessLevel.OWNER;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();

			User user = event.optUser("user");
			if (user == null) {
				editError(event, "bot.guild.access.remove.no_user");
				return;
			}

			long userId = user.getIdLong();
			long guildId = event.getGuild().getIdLong();
			if (!bot.getDBUtil().access.isOperator(guildId, userId)) {
				editError(event, "bot.guild.access.remove.operator.not_operator");
				return;
			}

			if (bot.getDBUtil().access.removeUser(guildId, userId)) {
				editErrorDatabase(event, "remove user");
				return;
			}

			// Log
			bot.getLogger().botLogs.onAccessRemoved(event.getGuild(), event.getUser(), user, null, CmdAccessLevel.OPERATOR);
			// Send reply
			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, "bot.guild.access.remove.operator.done").replace("{user}", user.getAsMention()))
				.build()
			);
		}
	}

}
