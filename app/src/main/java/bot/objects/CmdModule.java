package bot.objects;

public enum CmdModule {
	VOICE("modules.voice"),
	WEBHOOK("modules.webhook"),
	LANGUAGE("modules.language");
	
	private final String path;
	
	CmdModule(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

}
