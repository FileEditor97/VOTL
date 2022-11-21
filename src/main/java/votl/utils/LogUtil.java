package votl.utils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import javax.annotation.Nonnull;

import votl.App;
import votl.objects.constants.Constants;
import votl.utils.message.LocaleUtil;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Guild.Ban;
import net.dv8tion.jda.api.interactions.DiscordLocale;

public class LogUtil {

	private final App bot;
	private final LocaleUtil lu;

	public LogUtil(App bot) {
		this.bot = bot;
		this.lu = bot.getLocaleUtil();
	}
	
	@Nonnull
	public MessageEmbed getBanEmbed(DiscordLocale locale, Map<String, Object> banMap) {
		return getBanEmbed(locale, banMap, false);
	}

	@Nonnull
	public MessageEmbed getBanEmbed(DiscordLocale locale, Map<String, Object> banMap, Boolean formatUser) {
		return getBanEmbed(locale, Integer.parseInt(banMap.get("banId").toString()) , banMap.get("userNickname").toString(),
			banMap.get("userId").toString(), banMap.get("modId").toString(), Timestamp.valueOf(banMap.get("timeStart").toString()),
			Duration.parse(banMap.get("duration").toString()), banMap.get("reason").toString(), formatUser);
	}

	@SuppressWarnings("null")
	@Nonnull
	public MessageEmbed getBanEmbed(DiscordLocale locale, Integer banId, String userTag, String userId, String modId, Timestamp start, Duration duration, String reason, Boolean formatUser) {
		Instant timeStart = start.toInstant();
		Instant timeEnd = timeStart.plus(duration);
		return bot.getEmbedUtil().getEmbed().setColor(Constants.COLOR_FAILURE)
			.setAuthor(lu.getLocalized(locale, "bot.moderation.embeds.ban.title").replace("{case_id}", banId.toString()).replace("{user_tag}", userTag))
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.ban.user"), (formatUser ? String.format("<@%s>", userId) : userTag), true)
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.ban.mod"), String.format("<@%s>", modId), true)
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.ban.duration"), duration.isZero() ? lu.getLocalized(locale, "bot.moderation.embeds.permanently") : 
				lu.getLocalized(locale, "bot.moderation.embeds.temporary")
					.replace("{time}", bot.getMessageUtil().formatTime(timeEnd, false)), true)
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.ban.reason"), reason, true)
			.setFooter("ID: "+userId)
			.setTimestamp(timeStart)
			.build();
	}

	@Nonnull
	public MessageEmbed getUnbanEmbed(DiscordLocale locale, Ban banData, Member mod, String reason) {
		return getUnbanEmbed(locale, banData.getUser().getAsTag(), banData.getUser().getId(), mod.getAsMention(), banData.getReason(), reason);
	}

	@SuppressWarnings("null")
	@Nonnull
	public MessageEmbed getUnbanEmbed(DiscordLocale locale, String userTag, String userId, String modMention, String banReason, String reason) {
		return bot.getEmbedUtil().getEmbed().setColor(Constants.COLOR_WARNING)
			.setAuthor(lu.getLocalized(locale, "bot.moderation.embeds.unban.title").replace("{user_tag}", userTag))
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.unban.user"), String.format("<@%s>", userId), true)
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.unban.mod"), modMention, true)
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.unban.ban_reason"), (banReason!=null ? banReason : ""), true)
			.addField(lu.getLocalized(locale, "bot.moderation.embeds.unban.reason"), reason, true)
			.setFooter("ID: "+userId)
			.setTimestamp(Instant.now())
			.build();
	}

}