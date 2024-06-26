package dev.fileeditor.votl.commands.guild;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.fileeditor.votl.App;
import dev.fileeditor.votl.base.command.SlashCommand;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.PunishAction;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.exception.FormatterException;
import dev.fileeditor.votl.utils.message.TimeUtil;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AutopunishCmd extends CommandBase {

	public AutopunishCmd(App bot) {
		super(bot);
		this.name = "autopunish";
		this.path = "bot.guild.autopunish";
		this.children = new SlashCommand[]{new Add(bot), new Remove(bot), new View(bot)};
		this.category = CmdCategory.GUILD;
		this.module = CmdModule.STRIKES;
		this.accessLevel = CmdAccessLevel.ADMIN;
	}

	@Override
	protected void execute(SlashCommandEvent event) {}

	private class Add extends SlashCommand {

		public Add(App bot) {
			this.bot = bot;
			this.lu = bot.getLocaleUtil();
			this.name = "add";
			this.path = "bot.guild.autopunish.add";
			this.options = List.of(
				new OptionData(OptionType.INTEGER, "strike-count", lu.getText(path+".strike-count.help"), true).setRequiredRange(1, 40),
				new OptionData(OptionType.BOOLEAN, "kick", lu.getText(path+".kick.help")),
				new OptionData(OptionType.STRING, "mute", lu.getText(path+".mute.help")),
				new OptionData(OptionType.STRING, "ban", lu.getText(path+".ban.help")),
				new OptionData(OptionType.ROLE, "remove-role", lu.getText(path+".remove-role.help")),
				new OptionData(OptionType.ROLE, "add-role", lu.getText(path+".add-role.help"))
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			Integer strikeCount = event.optInteger("strike-count");
			
			if ((event.hasOption("kick") ? 1 : 0) + (event.hasOption("mute") ? 1 : 0) + (event.hasOption("ban") ? 1 : 0) >= 2) {
				editError(event, path+".one_option");
				return;
			}
			if ((event.hasOption("ban") || event.hasOption("kick")) && (event.hasOption("remove-role") || event.hasOption("add-role"))) {
				editError(event, path+".ban_kick_role");
				return;
			}
			if (event.hasOption("remove-role") && event.hasOption("add-role") && event.optRole("remove-role").equals(event.optRole("add-role"))) {
				editError(event, path+".same_role");
				return;
			}

			if (bot.getDBUtil().autopunish.getAction(event.getGuild().getIdLong(), strikeCount) != null) {
				editError(event, path+".exists");
				return;
			}
			
			List<PunishAction> actions = new ArrayList<>();
			List<String> data = new ArrayList<>();
			StringBuilder builder = new StringBuilder(lu.getText(event, path+".title").formatted(strikeCount));
			if (event.optBoolean("kick", false)) {
				actions.add(PunishAction.KICK);
				builder.append(lu.getText(event, path+".vkick"));
			}
			if (event.hasOption("mute")) {
				Duration duration;
				try {
					duration = TimeUtil.stringToDuration(event.optString("mute"), false);
				} catch (FormatterException ex) {
					editError(event, ex.getPath());
					return;
				}
				if (duration.isZero()) {
					editError(event, path+".not_zero");
					return;
				}
				actions.add(PunishAction.MUTE);
				data.add("t"+duration.getSeconds());
				builder.append(lu.getText(event, path+".vmute").formatted(TimeUtil.durationToLocalizedString(lu, event.getUserLocale(), duration)));
			}
			if (event.hasOption("ban")) {
				Duration duration;
				try {
					duration = TimeUtil.stringToDuration(event.optString("ban"), false);
				} catch (FormatterException ex) {
					editError(event, ex.getPath());
					return;
				}
				actions.add(PunishAction.BAN);
				data.add("t"+duration.getSeconds());
				builder.append(lu.getText(event, path+".vban").formatted(duration.isZero() ?
					lu.getText(event, "misc.permanently") :
					lu.getText(event, path+".for")+" "+TimeUtil.durationToLocalizedString(lu, event.getUserLocale(), duration)
				));
			}
			if (event.hasOption("remove-role")) {
				Role role = event.optRole("remove-role");
				if (!event.getGuild().getSelfMember().canInteract(role)) {
					editError(event, path+".incorrect_role");
					return;
				}
				actions.add(PunishAction.REMOVE_ROLE);
				data.add("rr"+role.getId());
				builder.append(lu.getText(event, path+".vremove").formatted(role.getName()));
			}
			if (event.hasOption("add-role")) {
				Role role = event.optRole("add-role");
				if (!event.getGuild().getSelfMember().canInteract(role)) {
					editError(event, path+".incorrect_role");
					return;
				}
				actions.add(PunishAction.ADD_ROLE);
				data.add("ar"+role.getId());
				builder.append(lu.getText(event, path+".vadd").formatted(role.getName()));
			}

			if (actions.isEmpty()) {
				editError(event, path+".empty");
				return;
			}

			bot.getDBUtil().autopunish.addAction(event.getGuild().getIdLong(), strikeCount, actions, String.join(";", data));
			editHookEmbed(event, bot.getEmbedUtil().getEmbed().setColor(Constants.COLOR_SUCCESS).setDescription(builder.toString()).build());
		}

	}

	private class Remove extends SlashCommand {

		public Remove(App bot) {
			this.bot = bot;
			this.lu = bot.getLocaleUtil();
			this.name = "remove";
			this.path = "bot.guild.autopunish.remove";
			this.options = List.of(
				new OptionData(OptionType.INTEGER, "strike-count", lu.getText(path+".strike-count.help"), true).setRequiredRange(1, 40)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			Integer strikeCount = event.optInteger("strike-count");

			if (bot.getDBUtil().autopunish.getAction(event.getGuild().getIdLong(), strikeCount) == null) {
				editError(event, path+".not_found");
				return;
			}

			bot.getDBUtil().autopunish.removeAction(event.getGuild().getIdLong(), strikeCount);
			editHookEmbed(event, bot.getEmbedUtil().getEmbed()
				.setColor(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").formatted(strikeCount))
				.build());
		}
		
	}

	// TODO
	/* private class Update extends SlashCommand {

		public Update(App bot) {
			this.bot = bot;
			this.lu = bot.getLocaleUtil();
			this.name = "update";
			this.path = "bot.guild.autopunish.update";
		}

		@Override
		protected void execute(SlashCommandEvent event) {

		}
		
	} */

	private class View extends SlashCommand {

		public View(App bot) {
			this.bot = bot;
			this.lu = bot.getLocaleUtil();
			this.name = "view";
			this.path = "bot.guild.autopunish.view";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();
			
			List<Map<String, Object>> list = bot.getDBUtil().autopunish.getAllActions(event.getGuild().getIdLong());
			if (list.isEmpty()) {
				editError(event, path+".empty");
				return;
			}

			StringBuffer buffer = new StringBuffer();
			list.forEach(map -> {
				Integer strikeCount = (Integer) map.get("strike");
				List<PunishAction> actions = PunishAction.decodeActions((Integer) map.get("actions"));
				if (actions.isEmpty()) return;
				String data = (String) map.getOrDefault("data", "");

				buffer.append("`%2d` ".formatted(strikeCount));
				actions.forEach(action -> {
					switch (action) {
						case KICK:
							buffer.append(lu.getText(event, action.getPath()));
							break;
						case MUTE:
						case BAN:
							Duration duration;
							try {
								duration = Duration.ofSeconds(Long.parseLong(action.getMatchedValue(data)));
							} catch (NumberFormatException ex) {
								break;
							}
							buffer.append("%s (%s)".formatted(lu.getText(event, action.getPath()), TimeUtil.durationToLocalizedString(lu, event.getUserLocale(), duration)));
							break;
						case REMOVE_ROLE:
						case ADD_ROLE:
							long roleId;
							try {
								roleId = Long.parseLong(action.getMatchedValue(data));
							} catch (NumberFormatException ex) {
								break;
							}
							buffer.append("%s (<@&%d>)".formatted(lu.getText(event, action.getPath()), roleId));
							break;
						default:
							break;
					}
					buffer.append(" ");
				});
				buffer.append("\n");
			});

			editHookEmbed(event, bot.getEmbedUtil().getEmbed()
				.setDescription(buffer.toString())
				.build());
		}
		
	}
	
}
