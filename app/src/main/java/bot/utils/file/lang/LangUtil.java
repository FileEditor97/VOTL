package bot.utils.file.lang;

import java.util.List;

import javax.annotation.Nonnull;

import bot.App;

public final class LangUtil {

	private final App bot;
	public LangUtil(App bot) {
		this.bot = bot;
	}
	
	@Nonnull
	public String getString(String language, String path) {
		switch (language.toLowerCase()) {
			case "ru-ru":
				return bot.getFileManager().getString(language.toLowerCase(), path);
			default: //en
				return bot.getFileManager().getString("en", path);
		}
	}

	@Nonnull
	public List<String> getStringList(String language, String path) {
		switch (language.toLowerCase()) {
			case "ru-ru":
				return bot.getFileManager().getStringList(language.toLowerCase(), path);
			default: //en
				return bot.getFileManager().getStringList("en", path);
		}
	}

	public enum Language {
		EN    ("\uDDEC", "\uDDE7"),
		RU_RU ("\uDDF7", "\uDDFA"),

		UNKNOWN ("\uDDFA", "\uDDF3");

		@Nonnull
		private final String emote;

		Language(String code1, String code2) {
			this.emote = "\uD83C" + code1 + "\uD83C" + code2;
		}

		@Nonnull
		public static String getEmote(String lang) {
			for (Language language : values()) {
				if (lang.equals(language.name().toLowerCase().replace("_", "-")))
					return language.emote;
			}

			return UNKNOWN.emote;
		}

		@Nonnull
		public static String getString(String lang) {
			for (Language language : values()) {
				if (lang.equals(language.name().toLowerCase().replace("_", "-")))
					return language.emote + " " + lang;
			}

			return UNKNOWN.emote + " " + UNKNOWN.name().toLowerCase();
		}
	}
}
