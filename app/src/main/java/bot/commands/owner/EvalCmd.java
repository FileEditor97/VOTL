package bot.commands.owner;

import bot.App;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
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

	private final App bot;

	protected Permission[] botPerms;
	
	public EvalCmd(App bot) {
		this.name = "eval";
		this.help = bot.getMsg("bot.owner.eval.description");
		this.ownerCommand = true;
		this.category = new Category("owner");
		this.options = Collections.singletonList(
			new OptionData(OptionType.STRING, "code", bot.getMsg("bot.owner.eval.code_description")) 
				.setRequired(true)
			// Я блять ненавижу эту штуку
			// Нужно переделовать через modals, но для этого нужно вначале получить комманду от пользователя
			// позже выслать форму для заполения и только потом обработать ее
			// ............пиздец
		);
		this.bot = bot;
	}

	@Override
	protected void execute(SlashCommandEvent event) {

		event.deferReply(true).queue(
			hook -> {
				MessageEmbed embed = getEvalEmbed(event);

				hook.editOriginalEmbeds(embed).queue();
			}
		);

	}

	private MessageEmbed getEvalEmbed(SlashCommandEvent event) {
		/* if (event.getArgs().isEmpty()) {
			bot.getEmbedUtil().sendError(event.getEvent(), "bot.owner.eval.invalid_args");
			event.reply("Available variables:\nbot; jda; guild; channel; message", success -> success.delete().queueAfter(5, TimeUnit.SECONDS));
			return;
		} */

		String args = event.getOption("code", OptionMapping::getAsString);
		args = args.trim();
		if (args.startsWith("```") && args.endsWith("```")) {
			if (args.startsWith("```java")) {
				args = args.substring(4);
			}
			args = args.substring(3, args.length() - 3);
		}

		Map<String, Object> variables = Map.of(
			"bot", bot,
			"jda", event.getJDA(),
			"guild", event.getGuild(),
			"channel", event.getChannel(),
			"client", event.getClient()
		);

		Binding binding = new Binding(variables);
		GroovyShell shell = new GroovyShell(binding);

		long startTime = System.currentTimeMillis();

		try {
			Object resp = shell.evaluate(args);
			String respString = String.valueOf(resp);

			return formatEvalEmbed(event.getTextChannel(), args, respString,
				bot.getMsg(event.getGuild().getId(), "bot.owner.eval.time")
					.replace("{time}", String.valueOf(System.currentTimeMillis() - startTime))
	 			, true);
		} catch (PowerAssertionError | Exception ex) {
			return formatEvalEmbed(event.getTextChannel(), args, ex.getMessage(),
				bot.getMsg(event.getGuild().getId(), "bot.owner.eval.time")
					.replace("{time}", String.valueOf(System.currentTimeMillis() - startTime))
				, false);
		}
	}

	private MessageEmbed formatEvalEmbed(TextChannel tc, String input, String output, String footer, boolean success) {
		String newMsg = input;
		
		EmbedBuilder embed = bot.getEmbedUtil().getEmbed()
			.setColor(success ? Color.GREEN : Color.RED)
			.addField(bot.getMsg(tc.getGuild().getId(), "bot.owner.eval.input"), String.format(
				"```java\n"+
					"%s\n"+
					"```",
				newMsg
				), false)
			.addField(bot.getMsg(tc.getGuild().getId(), "bot.owner.eval.output"), String.format(
				"```java\n"+
					"%s\n"+
					"```",
				output
				), false)
			.setFooter(footer, null);

		return embed.build();
	}
}