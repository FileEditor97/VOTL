package dev.fileeditor.votl.commands.other;

import dev.fileeditor.votl.base.command.SlashCommandEvent;
import dev.fileeditor.votl.commands.CommandBase;
import dev.fileeditor.votl.objects.constants.CmdCategory;

public class PingCmd extends CommandBase {
	
	public PingCmd() {
		this.name = "ping";
		this.path = "bot.other.ping";
		this.category = CmdCategory.OTHER;
		this.guildOnly = false;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		createReply(event, lu.getText(event, path+".loading"));

		event.getJDA().getRestPing().queue(time -> {
			editHook(event,
				lu.getText(event, "bot.other.ping.info_full")
					.replace("{websocket}", event.getJDA().getGatewayPing()+"")
					.replace("{rest}", time+"")
			);
		});	
	}
}
