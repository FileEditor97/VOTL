package dev.fileeditor.votl.utils.database;

import dev.fileeditor.votl.metrics.Metrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class LiteBase {

	private final ConnectionUtil util;
	protected final String table;

	public LiteBase(ConnectionUtil connectionUtil, String table) {
		this.util = connectionUtil;
		this.table = table;
	}

	/**
	 * @param sql SQL statement to execute
	 * @return true if exception
	 */
	// Execute statement
	protected boolean execute(final String sql) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue(sql.split(" ")[0].toUpperCase()).inc();
		// Log
		util.logger.debug(sql);
		// Execute
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at statement execution\nRequest: {}", sql, ex);
			return true;
		}
		return false;
	}

	protected int executeWithRow(final String sql) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue(sql.split(" ")[0].toUpperCase()).inc();
		// Log
		util.logger.debug(sql);
		// Execute
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
			return st.getGeneratedKeys().getInt(1);
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at statement execution\nRequest: {}", sql, ex);
			return 0;
		}
	}

	// Select
	protected <T> T selectOne(final String sql, String selectKey, Class<T> selectClass) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue("SELECT").inc();
		// Log
		util.logger.debug(sql);
		// Execute
		T result = null;
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();

			try {
				if (rs.next()) result = rs.getObject(selectKey, selectClass);
			} catch (SQLException ex) {
				if (!rs.wasNull()) throw ex;
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nRequest: {}", sql, ex);
		}
		return result;
	}

	protected <T> List<T> select(final String sql, String selectKey, Class<T> selectClass) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue("SELECT").inc();
		// Log
		util.logger.debug(sql);
		// Execute
		List<T> results = new ArrayList<>();
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				try {
					results.add(rs.getObject(selectKey, selectClass));
				} catch (SQLException ex) {
					if (!rs.wasNull()) throw ex;
				}
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nRequest: {}", sql, ex);
		}
		return results;
	}

	@Nullable
	protected Map<String, Object> selectOne(final String sql, final Set<String> selectKeys) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue("SELECT").inc();
		// Log
		util.logger.debug(sql);
		// Execute
		Map<String, Object> result = new HashMap<>();
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();

			if (rs.next())
				for (String key : selectKeys) {
					result.put(key, rs.getObject(key));
				}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nRequest: {}", sql, ex);
		}
		return result.isEmpty() ? null : result;
	}

	protected List<Map<String, Object>> select(final String sql, final Set<String> selectKeys) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue("SELECT").inc();
		// Log
		util.logger.debug(sql);
		// Execute
		List<Map<String, Object>> results = new ArrayList<>();
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Map<String, Object> data = new HashMap<>();
				for (String key : selectKeys) {
					data.put(key, rs.getObject(key));
				}
				results.add(data);
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nRequest: {}", sql, ex);
		}
		return results;
	}

	protected int count(final String sql) {
		// Metrics
		Metrics.databaseLiteQueries.labelValue("SELECT").inc();
		// Log
		util.logger.debug(sql);
		// Execute
		int result = 0;
		try (Connection conn = DriverManager.getConnection(util.getUrlSQLite());
			 PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();

			try {
				if (rs.next()) result = rs.getInt(1);
			} catch (SQLException ex) {
				if (!rs.wasNull()) throw ex;
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nRequest: {}", sql, ex);
		}
		return result;
	}


	// UTILS
	protected String quote(@Nullable final Object value) {
		// Convert to string and replace '(single quote) with ''(2 single quotes) for sql
		if (value == null ) return "NULL";
		final String text = String.valueOf(value);
		if (text.isBlank() || text.equalsIgnoreCase("NULL")) return "NULL";

		return String.format("'%s'", String.valueOf(value).replaceAll("'", "''"));
	}

	protected <T, V> T applyNonNull(V obj, @NotNull Function<V, T> function) {
		return (obj != null) ? function.apply(obj) : null;
	}

}
