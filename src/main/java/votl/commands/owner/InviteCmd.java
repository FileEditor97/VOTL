package votl.commands.owner;

import votl.App;
import votl.commands.CommandBase;
import votl.objects.command.SlashCommandEvent;
import votl.objects.constants.CmdCategory;

public class InviteCmd extends CommandBase {

	public InviteCmd(App bot) {
		super(bot);
		this.name = "invite";
		this.path = "bot.owner.invite";
		this.category = CmdCategory.OWNER;
		this.ownerCommand = true;
		this.guildOnly = false;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		createReply(event, lu.getText(event, path+".value")
			.replace("{bot_invite}", bot.getFileManager().getString("config", "bot-invite"))
		);
	}
}
