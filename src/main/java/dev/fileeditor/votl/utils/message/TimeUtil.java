package dev.fileeditor.votl.utils.message;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.fileeditor.votl.utils.exception.FormatterException;
import dev.fileeditor.votl.utils.file.lang.LocaleUtil;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.Nullable;

public class TimeUtil {

	private static final Pattern timePatternFull = Pattern.compile("^(([0-9]+)([smhdw]))+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern timePattern = Pattern.compile("([0-9]+)([smhdw])", Pattern.CASE_INSENSITIVE);

	private enum TimeUnit{
		SECONDS('s', 1),
		MINUTES('m', 60),
		HOURS  ('h', 3600),
		DAYS   ('d', 86400),
		WEEKS  ('w', 604800);

		private final char character;
		private final int multip;

		private static final HashMap<Character, TimeUnit> BY_CHAR = new HashMap<>();

		static {
			for (TimeUnit format : TimeUnit.values()) {
				BY_CHAR.put(format.getChar(), format);
			}
		}

		TimeUnit(char character, int multip) {
			this.character = character;
			this.multip = multip;
		}

		public char getChar() {
			return character;
		}

		public int getMultip() {
			return multip;
		}

		@Nullable
		public static Integer getMultipByChar(char c) {
			return Optional.ofNullable(BY_CHAR.get(c)).map(TimeUnit::getMultip).orElse(null);
		}
	}

	/*
	 * Duration and Period class have parse() method,
	 * but they are quite inconvenient, as we want to
	 * use both duration(h m s) and period(w d).
	 */
	public static Duration stringToDuration(String text, boolean allowSeconds) throws FormatterException {
		if (text == null || text.isEmpty() || text.equals("0")) {
			return Duration.ZERO;
		}

		if (!timePatternFull.matcher(text).matches()) {
			throw new FormatterException("errors.formatter.no_time_provided");
		}
		
		Matcher timeMatcher = timePattern.matcher(text);
		long time = 0L;
		while (timeMatcher.find()) {
			Character c = timeMatcher.group(2).charAt(0);
			if (c.equals('s') && !allowSeconds) {
				throw new FormatterException("errors.formatter.except_seconds");
			}
			Integer multip = TimeUnit.getMultipByChar(c);
			if (multip == null) {
				throw new FormatterException("errors.formatter.no_multip");
			}

			try {
				time = Math.addExact(time, Math.multiplyExact(Long.parseLong(timeMatcher.group(1)), multip));
			} catch (NumberFormatException ex) {
				throw new FormatterException("errors.formatter.parse_long");
			} catch (ArithmeticException ex) {
				throw new FormatterException("errors.formatter.long_overflow");
			}
		}
		
		return Duration.ofSeconds(time);
	}

	@SuppressWarnings("unused")
	public static String durationToString(Duration duration) {
		if (duration.isZero()) {
			return "0 seconds";
		}

		StringBuilder builder = new StringBuilder();

		final long weeks = Math.floorDiv(duration.toDaysPart(), 7);
		final long days = Math.floorMod(duration.toDaysPart(), 7);
		final int hours = duration.toHoursPart();
		final int minutes = duration.toMinutesPart();
		final int seconds = duration.toSecondsPart();

		if (weeks > 0) {
			if (weeks==1)
				builder.append("1 week");
			else
				builder.append(weeks).append(" weeks");
		}
		if (days > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (days==1)
				builder.append("1 day");
			else
				builder.append(days).append(" days");
		}
		if (hours > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (hours==1)
				builder.append("1 hour");
			else
				builder.append(hours).append(" hours");
		}
		if (minutes > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (minutes==1)
				builder.append("1 minute");
			else
				builder.append(minutes).append(" minutes");
		}
		if (seconds > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (seconds==1)
				builder.append("1 second");
			else
				builder.append(seconds).append(" seconds");
		}

		return builder.toString();
	}

	public static String durationToLocalizedString(LocaleUtil lu, DiscordLocale locale, Duration duration) {
		if (duration.isZero()) {
			return "0 %s".formatted(lu.getLocalized(locale, "misc.time.seconds"));
		}

		StringBuilder builder = new StringBuilder();

		final long weeks = Math.floorDiv(duration.toDaysPart(), 7);
		final long days = Math.floorMod(duration.toDaysPart(), 7);
		final int hours = duration.toHoursPart();
		final int minutes = duration.toMinutesPart();
		final int seconds = duration.toSecondsPart();

		if (weeks > 0) {
			if (weeks==1)
				builder.append("1 %s".formatted(lu.getLocalized(locale, "misc.time.week")));
			else
				builder.append("%s %s".formatted(weeks, lu.getLocalized(locale, "misc.time.weeks")));
		}
		if (days > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (days==1)
				builder.append("1 %s".formatted(lu.getLocalized(locale, "misc.time.day")));
			else
				builder.append("%s %s".formatted(days, lu.getLocalized(locale, "misc.time.days")));
		}
		if (hours > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (hours==1)
				builder.append("1 %s".formatted(lu.getLocalized(locale, "misc.time.hour")));
			else
				builder.append("%s %s".formatted(hours, lu.getLocalized(locale, "misc.time.hours")));
		}
		if (minutes > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (minutes==1)
				builder.append("1 %s".formatted(lu.getLocalized(locale, "misc.time.minute")));
			else
				builder.append("%s %s".formatted(minutes, lu.getLocalized(locale, "misc.time.minutes")));
		}
		if (seconds > 0) {
			if (!builder.isEmpty()) builder.append(" ");
			if (seconds==1)
				builder.append("1 %s".formatted(lu.getLocalized(locale, "misc.time.second")));
			else
				builder.append("%s %s".formatted(seconds, lu.getLocalized(locale, "misc.time.seconds")));
		}

		return builder.toString();
	}

	public static String formatTime(TemporalAccessor time, boolean full) {
		if (time == null) return "";
		if (full) {
			return String.format(
				"%s (%s)",
				TimeFormat.DATE_TIME_SHORT.format(time),
				TimeFormat.RELATIVE.format(time)
			);
		}
		return String.format(
			"%s %s",
			TimeFormat.DATE_SHORT.format(time),
			TimeFormat.TIME_SHORT.format(time)
		);
	}

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
		.withZone(ZoneId.systemDefault());
	public static String timeToString(TemporalAccessor time) {
		if (time == null) return "indefinitely";
		return formatter.format(time);
	}

	public static String formatDuration(LocaleUtil lu, DiscordLocale locale, Instant startTime, Duration duration) {
		return duration.isZero() ?
			lu.getLocalized(locale, "misc.permanently")
			:
			lu.getLocalized(locale, "misc.temporary").formatted(formatTime(startTime.plus(duration), false));
	}

}
