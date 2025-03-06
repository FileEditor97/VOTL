package dev.fileeditor.votl.commands.games;

import java.sql.SQLException;
import java.util.List;

import dev.fileeditor.votl.base.command.SlashCommand;
import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.CmdAccessLevel;
import dev.fileeditor.votl.objects.CmdModule;
import dev.fileeditor.votl.objects.constants.CmdCategory;
import dev.fileeditor.votl.objects.constants.Constants;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class GameCmd extends CommandBase {

	public GameCmd() {
		this.name = "game";
		this.path = "bot.games.game";
		this.children = new SlashCommand[]{new Add(), new Remove(), new ViewChannels(), new Clear()};
		this.category = CmdCategory.GAMES;
		this.module = CmdModule.GAMES;
		this.accessLevel = CmdAccessLevel.ADMIN;
	}

	@Override
	protected void execute(SlashCommandEvent event) {}

	private class Add extends SlashCommand {
		public Add() {
			this.name = "add";
			this.path = "bot.games.game.add";
			this.options = List.of(
				new OptionData(OptionType.CHANNEL, "channel", lu.getText(path+".channel.help"), true)
					.setChannelTypes(ChannelType.TEXT),
				new OptionData(OptionType.INTEGER, "max_strikes", lu.getText(path+".max_strikes.help"))
					.setRequiredRange(1,6)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			GuildChannel channel = event.optGuildChannel("channel");
			if (bot.getDBUtil().games.getMaxStrikes(channel.getIdLong()) != null) {
				editError(event, path+".already", "Channel: %s".formatted(channel.getAsMention()));
				return;
			}
			int maxStrikes = event.optInteger("max_strikes", 3);

			try {
				bot.getDBUtil().games.addChannel(event.getGuild().getIdLong(), channel.getIdLong(), maxStrikes);
			} catch (SQLException ex) {
				editErrorDatabase(event, ex, "add channel");
				return;
			}

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").formatted(channel.getAsMention(), maxStrikes))
				.build());
		}
	}

	private class Remove extends SlashCommand {
		public Remove() {
			this.name = "remove";
			this.path = "bot.games.game.remove";
			this.options = List.of(
				new OptionData(OptionType.CHANNEL, "channel", lu.getText(path+".channel.help"), true)
					.setChannelTypes(ChannelType.TEXT)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			GuildChannel channel = event.optGuildChannel("channel");
			if (bot.getDBUtil().games.getMaxStrikes(channel.getIdLong()) == null) {
				editError(event, path+".not_found", "Channel: %s".formatted(channel.getAsMention()));
				return;
			}

			try {
				bot.getDBUtil().games.removeChannel(channel.getIdLong());
			} catch (SQLException ex) {
				editErrorDatabase(event, ex, "remove channel");
				return;
			}

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").formatted(channel.getAsMention()))
				.build());
		}
	}

	private class ViewChannels extends SlashCommand {
		public ViewChannels() {
			this.name = "view-channels";
			this.path = "bot.games.game.view-channels";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply(true).queue();
			List<Long> channels = bot.getDBUtil().games.getChannels(event.getGuild().getIdLong());
			if (channels.isEmpty()) {
				editEmbed(event, bot.getEmbedUtil().getEmbed()
					.setDescription(lu.getText(event, path+".none"))
					.build()
				);
				return;
			}

			StringBuilder builder = new StringBuilder();
			for (Long channelId : channels) {
				builder.append("<#").append(channelId).append(">\n");
			}

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setTitle(lu.getText(event, path+".embed_title"))
				.setDescription(builder.toString())
				.build()
			);
		}
	}

	private class Clear extends SlashCommand {
		public Clear() {
			this.name = "clear";
			this.path = "bot.games.game.clear";
			this.options = List.of(
				new OptionData(OptionType.USER, "user", lu.getText(path+".user.help"), true),
				new OptionData(OptionType.CHANNEL, "channel", lu.getText(path+".channel.help"), true)
					.setChannelTypes(ChannelType.TEXT)
			);
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.deferReply().queue();
			GuildChannel channel = event.optGuildChannel("channel");
			if (bot.getDBUtil().games.getMaxStrikes(channel.getIdLong()) == null) {
				editError(event, path+".not_found", "Channel: %s".formatted(channel.getAsMention()));
				return;
			}
			User user = event.optUser("user");
			if (user == null) {
				editError(event, path+".no_user");
				return;
			}

			try {
				bot.getDBUtil().games.clearStrikes(channel.getIdLong(), user.getIdLong());
			} catch (SQLException ex) {
				editErrorDatabase(event, ex, "clear strikes");
				return;
			}

			editEmbed(event, bot.getEmbedUtil().getEmbed(Constants.COLOR_SUCCESS)
				.setDescription(lu.getText(event, path+".done").formatted(user.getAsMention(), channel.getAsMention()))
				.build());
		}
	}

}