package dev.fileeditor.votl.utils.database.managers;

import static dev.fileeditor.votl.utils.CastUtil.getOrDefault;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.fileeditor.votl.objects.constants.Constants;
import dev.fileeditor.votl.utils.FixedCache;
import dev.fileeditor.votl.utils.database.ConnectionUtil;
import dev.fileeditor.votl.utils.database.LiteBase;

public class TicketSettingsManager extends LiteBase {
	
	private final Set<String> columns = Set.of(
		"autocloseTime", "autocloseLeft", "timeToReply",
		"rowName1", "rowName2", "rowName3", "otherRole"
	);

	// Cache
	private final FixedCache<Long, TicketSettings> cache = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);
	private final TicketSettings defaultSettings = new TicketSettings();

	public TicketSettingsManager(ConnectionUtil cu) {
		super(cu, "ticketSettings");
	}

	public TicketSettings getSettings(long guildId) {
		if (cache.contains(guildId))
			return cache.get(guildId);
		TicketSettings settings = applyNonNull(getData(guildId), TicketSettings::new);
		if (settings == null)
			settings = defaultSettings;
		cache.put(guildId, settings);
		return settings;
	}

	private Map<String, Object> getData(long guildId) {
		return selectOne("SELECT * FROM %s WHERE (guildId=%d)".formatted(table, guildId), columns);
	}

	public void remove(long guildId) {
		invalidateCache(guildId);
		execute("DELETE FROM %s WHERE (guildId=%d)".formatted(table, guildId));
	}

	public boolean setRowText(long guildId, int row, String text) {
		if (row < 1 || row > 3)
			throw new IndexOutOfBoundsException(row);
		invalidateCache(guildId);
		return execute("INSERT INTO %1$s(guildId, rowName%2$d) VALUES (%3$d, %4$s) ON CONFLICT(guildId) DO UPDATE SET rowName%2$d=%4$s".formatted(table, row, guildId, quote(text)));
	}

	public boolean setAutocloseTime(long guildId, int hours) {
		invalidateCache(guildId);
		return execute("INSERT INTO %s(guildId, autocloseTime) VALUES (%d, %d) ON CONFLICT(guildId) DO UPDATE SET autocloseTime=%<d".formatted(table, guildId, hours));
	}

	public boolean setAutocloseLeft(long guildId, boolean close) {
		invalidateCache(guildId);
		return execute("INSERT INTO %s(guildId, autocloseLeft) VALUES (%d, %d) ON CONFLICT(guildId) DO UPDATE SET autocloseLeft=%<d".formatted(table, guildId, close ? 1 : 0));
	}

	public boolean setTimeToReply(long guildId, int hours) {
		invalidateCache(guildId);
		return execute("INSERT INTO %s(guildId, timeToReply) VALUES (%d, %d) ON CONFLICT(guildId) DO UPDATE SET timeToReply=%<d".formatted(table, guildId, hours));
	}

	public boolean setOtherRole(long guildId, boolean otherRole) {
		invalidateCache(guildId);
		return execute("INSERT INTO %s(guildId, otherRole) VALUES (%d, %d) ON CONFLICT(guildId) DO UPDATE SET otherRole=%<d".formatted(table, guildId, otherRole ? 1 : 0));
	}

	private void invalidateCache(long guildId) {
		cache.pull(guildId);
	}

	public static class TicketSettings {
		private final int autocloseTime, timeToReply;
		private final boolean autocloseLeft, otherRole;
		private final List<String> rowText;

		public TicketSettings() {
			this.autocloseTime = 0;
			this.autocloseLeft = false;
			this.timeToReply = 0;
			this.otherRole = true;
			this.rowText = Collections.nCopies(3, "Select roles");
		}

		public TicketSettings(Map<String, Object> data) {
			this.autocloseTime = getOrDefault(data.get("autocloseTime"), 0);
			this.autocloseLeft = getOrDefault(data.get("autocloseLeft"), 0) == 1;
			this.timeToReply = getOrDefault(data.get("timeToReply"), 0);
			this.otherRole = getOrDefault(data.get("otherRole"), 0) == 1;
			this.rowText = List.of(
				getOrDefault(data.get("rowName1"), "Select roles"),
				getOrDefault(data.get("rowName2"), "Select roles"),
				getOrDefault(data.get("rowName3"), "Select roles")
			);
		}

		public int getAutocloseTime() {
			return autocloseTime;
		}

		public boolean autocloseLeftEnabled() {
			return autocloseLeft;
		}

		public int getTimeToReply() {
			return timeToReply;
		}

		public boolean otherRoleEnabled() {
			return otherRole;
		}

		public String getRowText(int n) {
			if (n < 1 || n > 3)
				throw new IndexOutOfBoundsException(n);
			return rowText.get(n-1);
		}
	}

}
