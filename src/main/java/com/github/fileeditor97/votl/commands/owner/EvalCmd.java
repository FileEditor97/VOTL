package com.github.fileeditor97.votl.commands.owner;

import java.util.Collections;
import java.util.Map;

import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import com.github.fileeditor97.votl.App;
import com.github.fileeditor97.votl.objects.CmdAccessLevel;
import com.github.fileeditor97.votl.objects.command.SlashCommand;
import com.github.fileeditor97.votl.objects.command.SlashCommandEvent;
import com.github.fileeditor97.votl.objects.constants.CmdCategory;
import com.github.fileeditor97.votl.objects.constants.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@CommandInfo
(
	name = "Eval",
	usage = "/eval <code:>",
	description = "Evaluates givven code.",
	requirements = {"Be the bot's owner", "Have fck knowledge of what your're doing with it"}
)
public class EvalCmd extends SlashCommand {
	
	public EvalCmd(App bot) {
		this.name = "eval";
		this.helpPath = "bot.owner.eval.help";
		this.options = Collections.singletonList(
			new OptionData(OptionType.STRING, "code", bot.getLocaleUtil().getText("bot.owner.eval.code_description")) 
				.setRequired(true)
			// Я блять ненавижу эту штуку
			// Нужно переделовать через modals, но для этого нужно вначале получить комманду от пользователя
			// позже выслать форму для заполения и только потом обработать ее
			// ............пиздец
		);
		this.bot = bot;
		this.category = CmdCategory.OWNER;
		this.accessLevel = CmdAccessLevel.DEV;
		this.ownerCommand = true;
		this.guildOnly = false;
	}

	@Override
	protected void execute(SlashCommandEvent event) {

		event.deferReply(true).queue(
			hook -> {
				sendEvalEmbed(event, hook);
			}
		);

	}

	private void sendEvalEmbed(SlashCommandEvent event, InteractionHook hook) {

		DiscordLocale userLocale = event.getUserLocale();

		String args = event.getOption("code", "", OptionMapping::getAsString).trim();
		if (args.startsWith("```") && args.endsWith("```")) {
			if (args.startsWith("```java")) {
				args = args.substring(4);
			}
			args = args.substring(3, args.length() - 3);
		}

		Map<String, Object> variables = Map.of(
			"bot", bot,
			"event", event,
			"jda", event.getJDA(),
			"guild", (event.isFromGuild() ? event.getGuild() : null),
			"channel", event.getChannel(),
			"client", event.getClient()
		);

		Binding binding = new Binding(variables);
		GroovyShell shell = new GroovyShell(binding);

		long startTime = System.currentTimeMillis();

		try {
			Object resp = shell.evaluate(args);
			String respString = String.valueOf(resp);

			hook.editOriginalEmbeds(formatEvalEmbed(userLocale, args, respString,
				lu.getLocalized(userLocale, "bot.owner.eval.time")
					.replace("{time}", String.valueOf(System.currentTimeMillis() - startTime))
	 			, true)).queue();
		} catch (PowerAssertionError | Exception ex) {
			hook.editOriginalEmbeds(formatEvalEmbed(userLocale, args, ex.getMessage(),
				lu.getLocalized(userLocale, "bot.owner.eval.time")
					.replace("{time}", String.valueOf(System.currentTimeMillis() - startTime))
				, false)).queue();
		}
	}

	@SuppressWarnings("null")
	private MessageEmbed formatEvalEmbed(DiscordLocale locale, String input, String output, String footer, boolean success) {		
		EmbedBuilder embed = bot.getEmbedUtil().getEmbed()
			.setColor(success ? Constants.COLOR_SUCCESS : Constants.COLOR_FAILURE)
			.addField(lu.getLocalized(locale, "bot.owner.eval.input"), String.format(
				"```java\n"+
					"%s\n"+
					"```",
				input
				), false)
			.addField(lu.getLocalized(locale, "bot.owner.eval.output"), String.format(
				"```java\n"+
					"%s\n"+
					"```",
				output
				), false)
			.setFooter(footer, null);

		return embed.build();
	}
}