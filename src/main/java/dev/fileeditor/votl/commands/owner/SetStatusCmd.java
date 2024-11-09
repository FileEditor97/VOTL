package dev.fileeditor.votl.commands.owner;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SetStatusCmd extends CommandBase {
	public SetStatusCmd() {
		this.name = "setstatus";
		this.path = "bot.owner.setstatus";
		this.options = List.of(
			new OptionData(OptionType.STRING, "type", lu.getText(path+".type.help"), true)
				.addChoices(
					new Command.Choice("- Clear -", "clear"),
					new Command.Choice("- Custom -", "custom"),
					new Command.Choice("Playing", "playing"),
					new Command.Choice("Streaming", "streaming"),
					new Command.Choice("Listening", "listening"),
					new Command.Choice("Watching", "watching")
				),
			new OptionData(OptionType.STRING, "text", lu.getText(path+".text.help"), true)
				.setMaxLength(128),
			new OptionData(OptionType.STRING, "url", lu.getText(path+".url.help"))
				.setMaxLength(100)
		);
		this.category = CmdCategory.OWNER;
		this.ownerCommand = true;
		this.guildOnly = false;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.deferReply(true).queue();

		ActivityType type = parseType(event.optString("type"));
		if (type == null) {
			event.getJDA().getPresence().setActivity(null);
			editHookEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".clear"))
				.build());
			return;
		}
		String text = event.optString("text");

		switch (type) {
			case PLAYING, LISTENING, WATCHING, CUSTOM_STATUS -> {
				event.getJDA().getPresence().setActivity(Activity.of(type, text));
				editHookEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
					.setDescription(lu.getText(event, path+".set").formatted(activityString(type), text))
					.build());
			}
			case STREAMING -> {
				String url = event.optString("url");
				if (!Activity.isValidStreamingUrl(url)) {
					editError(event, path+".invalid_url", url);
					return;
				}
				event.getJDA().getPresence().setActivity(Activity.of(type, text, url));
				editHookEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
					.setDescription(lu.getText(event, path+".set").formatted(activityString(type), text+"\n> URL: "+url ))
					.build());
			}
		}
	}

	private ActivityType parseType(@Nullable String type) {
		return switch (type) {
			case "playing" -> ActivityType.PLAYING;
			case "streaming" -> ActivityType.STREAMING;
			case "listening" -> ActivityType.LISTENING;
			case "watching" -> ActivityType.WATCHING;
			case "custom" -> ActivityType.CUSTOM_STATUS;
			default -> null;
		};
	}

	private String activityString(@Nullable ActivityType type) {
		return switch (type) {
			case PLAYING -> "Playing";
			case STREAMING -> "Streaming";
			case LISTENING -> "Listening";
			case WATCHING -> "Watching";
			case CUSTOM_STATUS -> "Custom Status";
			default -> null;
		};
	}
}
