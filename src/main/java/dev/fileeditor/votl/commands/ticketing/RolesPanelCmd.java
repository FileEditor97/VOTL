package dev.fileeditor.votl.commands.ticketing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.fileeditor.votl.base.command.SlashCommand;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.RoleType;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.message.MessageUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class RolesPanelCmd extends CommandBase {
	
	public RolesPanelCmd() {
		this.name = "rolespanel";
		this.path = "bot.ticketing.rolespanel";
		this.children = new SlashCommand[]{new Create(), new Update(), new RowText(), new OtherRole(), new SupportRole()};
		this.botPermissions = new Permission[]{Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS};
		this.module = CmdModule.TICKETING;
		this.category = CmdCategory.TICKETING;
		this.accessLevel = CmdAccessLevel.ADMIN;
	}

	@Override
	protected void execute(SlashCommandEvent event) {}

	private class Create extends SlashCommand {
		public Create() {
			this.name = "create";
			this.path = "bot.ticketing.rolespanel.create";
			this.options = List.of(
				new OptionData(OptionType.CHANNEL, "channel", lu.getText(path+".channel.help"), true)
					.setChannelTypes(ChannelType.TEXT)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			Guild guild = event.getGuild();
			long guildId = guild.getIdLong();

			TextChannel channel = (TextChannel) event.optGuildChannel("channel");
			if (channel == null) {
				editError(event, path+".no_channel", "Received: No channel");
				return;
			}

			int assignRolesSize = bot.getDBUtil().roles.countRoles(guildId, RoleType.ASSIGN);
			List<Map<String, Object>> toggleRoles = bot.getDBUtil().roles.getToggleable(guildId);
			if (assignRolesSize == 0 && toggleRoles.isEmpty()) {
				editError(event, path+".empty_roles");
				return;
			}
			List<ActionRow> actionRows = new ArrayList<>();

			if (assignRolesSize > 0) {
				actionRows.add(ActionRow.of(Button.success("role:start_request", lu.getLocalized(event.getGuildLocale(), "bot.ticketing.embeds.button_request"))));
			}
			actionRows.add(ActionRow.of(Button.danger("role:remove", lu.getLocalized(event.getGuildLocale(), "bot.ticketing.embeds.button_remove"))));
			if (!toggleRoles.isEmpty()) {
				List<Button> buttons = new ArrayList<>();
				toggleRoles.forEach(data -> {
					if (buttons.size() >= 5) return;
					String roleId = data.get("roleId").toString();
					Role role = guild.getRoleById(roleId);
					if (role == null) return;
					String description = data.get("description").toString();
					buttons.add(Button.primary("role:toggle:"+roleId, MessageUtil.limitString(description, 80)));
				});
				actionRows.add(ActionRow.of(buttons));
			}

			MessageEmbed embed = new EmbedBuilder()
				.setColor(bot.getDBUtil().getGuildSettings(guild).getColor())
				.setTitle(lu.getLocalized(event.getGuildLocale(), "bot.ticketing.embeds.role_title"))
				.setDescription(lu.getLocalized(event.getGuildLocale(), "bot.ticketing.embeds.role_value"))
				.setFooter(guild.getName(), guild.getIconUrl())
				.build();

			channel.sendMessageEmbeds(embed).addComponents(actionRows).queue();

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").replace("{channel}", channel.getAsMention()))
				.build()
			);
		}
	}

	private class Update extends SlashCommand {
		public Update() {
			this.name = "update";
			this.path = "bot.ticketing.rolespanel.update";
			this.options = List.of(
				new OptionData(OptionType.CHANNEL, "channel", lu.getText(path+".channel.help"), true)
					.setChannelTypes(ChannelType.TEXT)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			Guild guild = event.getGuild();
			long guildId = guild.getIdLong();

			TextChannel channel = (TextChannel) event.optGuildChannel("channel");
			if (channel == null) {
				editError(event, path+".no_channel", "Received: No channel");
				return;
			}

			String latestId = channel.getLatestMessageId();
			channel.retrieveMessageById(latestId).queue(msg -> {
				if (!msg.getAuthor().equals(event.getJDA().getSelfUser())) {
					editError(event, path+".not_found", "Not bot's message");
					return;
				}

				int assignRolesSize = bot.getDBUtil().roles.countRoles(guildId, RoleType.ASSIGN);
				List<Map<String, Object>> toggleRoles = bot.getDBUtil().roles.getToggleable(guildId);
				if (assignRolesSize == 0 && toggleRoles.isEmpty()) {
					editError(event, path+".empty_roles");
					return;
				}
				List<ActionRow> actionRows = new ArrayList<>();

				if (assignRolesSize > 0) {
					actionRows.add(ActionRow.of(Button.success("role:start_request", lu.getLocalized(event.getGuildLocale(), "bot.ticketing.embeds.button_request"))));
				}
				actionRows.add(ActionRow.of(Button.danger("role:remove", lu.getLocalized(event.getGuildLocale(), "bot.ticketing.embeds.button_remove"))));
				if (!toggleRoles.isEmpty()) {
					List<Button> buttons = new ArrayList<>();
					toggleRoles.forEach(data -> {
						if (buttons.size() >= 5) return;
						String roleId = data.get("roleId").toString();
						Role role = guild.getRoleById(roleId);
						if (role == null) return;
						String description = data.get("description").toString();
						buttons.add(Button.primary("role:toggle:"+roleId, MessageUtil.limitString(description, 80)));
					});
					actionRows.add(ActionRow.of(buttons));
				}
				
				msg.editMessageComponents(actionRows).queue();

				editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
					.setDescription(lu.getText(event, path+".done").replace("{channel}", channel.getAsMention()))
					.build()
				);
			}, failure -> editError(event, path+".not_found", failure.getMessage()));
		}
	}

	private class RowText extends SlashCommand {
		public RowText() {
			this.name = "row";
			this.path = "bot.ticketing.rolespanel.row";
			this.options = List.of(
				new OptionData(OptionType.INTEGER, "row", lu.getText(path+".row.help"), true)
					.setRequiredRange(1, 3),
				new OptionData(OptionType.STRING, "text", lu.getText(path+".text.help"), true)
					.setMaxLength(150)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			Integer row = event.optInteger("row");
			String text = event.optString("text");

			if (bot.getDBUtil().ticketSettings.setRowText(event.getGuild().getIdLong(), row, text)) {
				editErrorDatabase(event, "set ticket row text");
				return;
			}

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").replace("{row}", row.toString()).replace("{text}", text))
				.build());
		}
	}

	private class OtherRole extends SlashCommand {
		public OtherRole() {
			this.name = "other";
			this.path = "bot.ticketing.rolespanel.other";
			this.options = List.of(
				new OptionData(OptionType.BOOLEAN, "enabled", lu.getText(path+".enabled.help"), true)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			boolean enabled = event.optBoolean("enabled");

			if (bot.getDBUtil().ticketSettings.setOtherRole(event.getGuild().getIdLong(), enabled)) {
				editErrorDatabase(event, "set ticket other");
				return;
			}

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").formatted(String.valueOf(enabled)))
				.build());
		}
	}

	private class SupportRole extends SlashCommand {
		public SupportRole() {
			this.name = "support";
			this.path = "bot.ticketing.rolespanel.support";
			this.options = List.of(
				new OptionData(OptionType.STRING, "roles", lu.getText(path+".roles.help"), true)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();

			if (event.optString("roles").equalsIgnoreCase("null")) {
				// Clear roles
				if (!bot.getDBUtil().ticketSettings.setSupportRoles(event.getGuild().getIdLong(), null)) {
					editErrorDatabase(event, "clear ticket support roles");
					return;
				}

				editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
					.setDescription(lu.getText(event, path+".done_clear"))
					.build());
			} else {
				// Set roles
				List<Role> roles = event.optMentions("roles").getRoles();
				if (roles.isEmpty() || roles.size()>3) {
					editError(event, path+".bad_input");
					return;
				}
				if (!bot.getDBUtil().ticketSettings.setSupportRoles(event.getGuild().getIdLong(), roles.stream().map(Role::getIdLong).toList())) {
					editErrorDatabase(event, "set ticket support roles");
					return;
				}

				editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
					.setDescription(lu.getText(event, path+".done").formatted(roles.stream().map(Role::getAsMention).collect(Collectors.joining(", "))))
					.build());
			}
		}
	}

}
