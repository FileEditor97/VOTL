package votl.commands.other;

import votl.App;
import votl.commands.CommandBase;
import votl.objects.command.SlashCommandEvent;
import votl.objects.constants.CmdCategory;

import com.jagrosh.jdautilities.doc.standard.CommandInfo;

@CommandInfo
(
	name = "Ping",
	description = "Checks the bot's latency.",
	usage = "/ping",
	requirements = "none"
)
public class PingCmd extends CommandBase {
	
	public PingCmd(App bot) {
		super(bot);
		this.name = "ping";
		this.path = "bot.other.ping";
		this.category = CmdCategory.OTHER;
		this.guildOnly = false;
	}

	@SuppressWarnings("null")
	@Override
	protected void execute(SlashCommandEvent event) {
		event.deferReply(true).queue(hook -> {
			Long st = System.currentTimeMillis();
			hook.getJDA().getRestPing().queue(time -> {
				hook.editOriginal(
					lu.getLocalized(event.getUserLocale(), "bot.other.ping.info_full")
						.replace("{ping}", String.valueOf(System.currentTimeMillis() - st))
						.replace("{websocket}", event.getJDA().getGatewayPing()+"")
						.replace("{rest}", time+"")
				).queue();
			});	
		});
	}
}
