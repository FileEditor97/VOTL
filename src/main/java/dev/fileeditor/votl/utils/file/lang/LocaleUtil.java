package dev.fileeditor.votl.utils.file.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.fileeditor.votl.App;
import dev.fileeditor.votl.objects.Emote;
import dev.fileeditor.votl.objects.annotation.Nonnull;
import dev.fileeditor.votl.objects.annotation.Nullable;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

public class LocaleUtil {

	private final App bot;
	private final LangUtil langUtil;
	public final DiscordLocale defaultLocale;

	public LocaleUtil(App bot, DiscordLocale defaultLocale) {
		this.bot = bot;
		this.langUtil = new LangUtil(bot.getFileManager());
		this.defaultLocale = defaultLocale;
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path) {
		return Emote.getWithEmotes(langUtil.getString(locale, path));
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user) {
		return getLocalized(locale, path, user, true);
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, boolean format) {
		if (format)
			user = bot.getMessageUtil().getFormattedMembers(user);

		return Objects.requireNonNull(getLocalized(locale, path).replace("{user}", user));
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, String target) {
		target = (target == null ? "null" : target);
		
		return getLocalized(locale, path, user, Collections.singletonList(target), false);
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, List<String> targets) {
		return getLocalized(locale, path, user, targets, false);
	}

	@Nonnull
	public String getLocalized(DiscordLocale locale, String path, String user, List<String> targets, boolean format) {
		String targetReplacement = targets.isEmpty() ? "null" : bot.getMessageUtil().getFormattedMembers(targets.toArray(new String[0]));

		return Objects.requireNonNull(getLocalized(locale, path, user, format)
			.replace("{target}", targetReplacement)
			.replace("{targets}", targetReplacement)
		);
	}

	@Nullable
	public String getLocalizedNullable(DiscordLocale locale, String path) {
		return langUtil.getNullableString(locale, path);
	}

	@Nonnull
	public Map<DiscordLocale, String> getFullLocaleMap(String path, String defaultText) {
		Map<DiscordLocale, String> localeMap = new HashMap<>();
		for (DiscordLocale locale : bot.getFileManager().getLanguages()) {
			// Ignores UK/US change
			if (locale.equals(DiscordLocale.ENGLISH_UK) || locale.equals(DiscordLocale.ENGLISH_US)) continue;
			localeMap.put(locale, getLocalized(locale, path));
		}
		localeMap.put(DiscordLocale.ENGLISH_UK, defaultText);
		localeMap.put(DiscordLocale.ENGLISH_US, defaultText);
		return localeMap;
	}

	@Nonnull
	public Map<DiscordLocale, String> getLocaleMap(String path) {
		Map<DiscordLocale, String> localeMap = new HashMap<>();
		for (DiscordLocale locale : bot.getFileManager().getLanguages()) {
			// Ignores UK/US change
			if (locale.equals(DiscordLocale.ENGLISH_UK) || locale.equals(DiscordLocale.ENGLISH_US)) continue;
			localeMap.put(locale, getLocalized(locale, path));
		}
		return localeMap;
	}

	@Nonnull
	public String getText(@Nonnull String path) {
		return getLocalized(defaultLocale, path);
	}

	@Nonnull
	public String getText(IReplyCallback replyCallback, @Nonnull String path) {
		return getLocalized(replyCallback.getUserLocale(), path);
	}

	@Nonnull
	public String getUserText(IReplyCallback replyCallback, @Nonnull String path) {
		return getUserText(replyCallback, path, Collections.emptyList(), false);
	}

	@Nonnull
	public String getUserText(IReplyCallback replyCallback, @Nonnull String path, boolean format) {
		return getUserText(replyCallback, path, Collections.emptyList(), format);
	}

	@Nonnull
	public String getUserText(IReplyCallback replyCallback, @Nonnull String path, String target) {
		return getUserText(replyCallback, path, Collections.singletonList(target), false);
	}
	
	@Nonnull
	public String getUserText(IReplyCallback replyCallback, @Nonnull String path, List<String> targets) {
		return getUserText(replyCallback, path, targets, false);
	}

	@Nonnull
	private String getUserText(IReplyCallback replyCallback, @Nonnull String path, List<String> targets, boolean format) {
		return getLocalized(replyCallback.getUserLocale(), path, replyCallback.getUser().getEffectiveName(), targets, format);
	}

	@Nonnull
	public String getGuildText(IReplyCallback replyCallback, @Nonnull String path) {
		return getLocalized(replyCallback.getGuildLocale(), path);
	}

}
