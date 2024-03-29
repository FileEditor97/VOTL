package votl.utils.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteDBBase {

	private DBUtil util;

	public SQLiteDBBase(DBUtil util) {
		this.util = util;
	}

	// INSERT sql
	protected void insert(String table, String insertKey, Object insertValueObj) {
		insert(table, List.of(insertKey), List.of(insertValueObj));
	}
	
	protected void insert(final String table, final List<String> insertKeys, final List<Object> insertValuesObj) {
		List<String> insertValues = new ArrayList<String>(insertValuesObj.size());
		for (Object obj : insertValuesObj) {
			insertValues.add(quote(obj));
		}

		String sql = "INSERT INTO "+table+" ("+String.join(", ", insertKeys)+") VALUES ("+String.join(", ", insertValues)+")";
		util.logger.debug(sql);
		try (Connection conn = util.connectSQLite();
		PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at INSERT\nrequest: {}", sql, ex);
		}
	}

	// SELECT sql
	protected List<Object> select(String table, String selectKey, String condKey, Object condValueObj) {
		return select(table, selectKey, List.of(condKey), List.of(condValueObj));
	}

	protected List<Object> select(final String table, final String selectKey, final List<String> condKeys, final List<Object> condValuesObj) {
		List<String> condValues = new ArrayList<String>(condValuesObj.size());
		for (Object obj : condValuesObj) {
			condValues.add(quote(obj));
		}

		String sql = "SELECT * FROM "+table+" WHERE ";
		for (int i = 0; i < condKeys.size(); i++) {
			if (i > 0) {
				sql += " AND ";
			}
			sql += condKeys.get(i)+"="+condValues.get(i);
		}

		List<Object> results = new ArrayList<Object>();
		util.logger.debug(sql);
		try (Connection conn = util.connectSQLite();
		PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				results.add(rs.getObject(selectKey));
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nrequest: {}", sql, ex);
		}
		return results;
	}

	protected List<Map<String, Object>> select(String table, List<String> selectKeys, String condKey, Object condValueObj) {
		return select(table, selectKeys, List.of(condKey), List.of(condValueObj));
	}

	protected List<Map<String, Object>> select(final String table, final List<String> selectKeys, final List<String> condKeys, final List<Object> condValuesObj) {
		List<String> condValues = new ArrayList<String>(condValuesObj.size());
		for (Object obj : condValuesObj) {
			condValues.add(quote(obj));
		}

		String sql = "SELECT * FROM "+table+" WHERE ";
		for (int i = 0; i < condKeys.size(); i++) {
			if (i > 0) {
				sql += " AND ";
			}
			sql += condKeys.get(i)+"="+condValues.get(i);
		}

		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

		util.logger.debug(sql);
		try (Connection conn = util.connectSQLite();
		PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();
			List<String> keys = new ArrayList<>();
			
			if (selectKeys.size() == 0) {
				for (int i = 1; i<=rs.getMetaData().getColumnCount(); i++) {
					keys.add(rs.getMetaData().getColumnName(i));
				}
			} else {
				keys = selectKeys;
			}

			while (rs.next()) {
				Map<String, Object> data = new HashMap<>();
				for (String key : keys) {
					data.put(key, rs.getObject(key));
				}
				results.add(data);
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nrequest: {}", sql, ex);
		}
		return results;
	}

	protected Object selectLast(final String table, final String selectKey) {
		String sql = "SELECT * FROM "+table+" ORDER BY "+selectKey+" DESC LIMIT 1";

		Object result = null;
		util.logger.debug(sql);
		try (Connection conn = util.connectSQLite();
		PreparedStatement st = conn.prepareStatement(sql)) {
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				result = rs.getObject(selectKey);
			}
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at SELECT\nrequest: {}", sql, ex);
		}
		return result;
	}

	// UPDATE sql
	protected void update(String table, String updateKey, Object updateValueObj, String condKey, Object condValueObj) {
		update(table, List.of(updateKey), List.of(updateValueObj), List.of(condKey), List.of(condValueObj));
	}

	protected void update(String table, String updateKey, Object updateValueObj, List<String> condKeys, List<Object> condValuesObj) {
		update(table, List.of(updateKey), List.of(updateValueObj), condKeys, condValuesObj);
	}

	protected void update(String table, List<String> updateKeys, List<Object> updateValuesObj, String condKey, Object condValueObj) {
		update(table, updateKeys, updateValuesObj, List.of(condKey), List.of(condValueObj));
	}

	protected void update(final String table, final List<String> updateKeys, final List<Object> updateValuesObj, final List<String> condKeys, final List<Object> condValuesObj) {
		List<String> updateValues = new ArrayList<String>(updateValuesObj.size());
		for (Object obj : updateValuesObj) {
			updateValues.add(quote(obj));
		}
		List<String> condValues = new ArrayList<String>(condValuesObj.size());
		for (Object obj : condValuesObj) {
			condValues.add(quote(obj));
		}

		String sql = "UPDATE "+table+" SET ";
		for (int i = 0; i<updateKeys.size(); i++) {
			if (i > 0) {
				sql += ", ";
			}
			sql += updateKeys.get(i)+"="+updateValues.get(i);
		}
		sql += " WHERE ";
		for (int i = 0; i<condKeys.size(); i++) {
			if (i > 0) {
				sql += " AND ";
			}
			sql += condKeys.get(i)+"="+condValues.get(i);
		}

		util.logger.debug(sql);
		try (Connection conn = util.connectSQLite();
		PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at UPDATE\nrequest: {}", sql, ex);
		}
	}

	// DELETE sql
	protected void delete(String table, String condKey, Object condValueObj) {
		delete(table, List.of(condKey), List.of(condValueObj));
	}

	protected void delete(final String table, final List<String> condKeys, final List<Object> condValuesObj) {
		List<String> condValues = new ArrayList<String>(condValuesObj.size());
		for (Object obj : condValuesObj) {
			condValues.add(quote(obj));
		}

		String sql = "DELETE FROM "+table+" WHERE ";
		for (int i = 0; i<condKeys.size(); i++) {
			if (i > 0) {
				sql += " AND ";
			}
			sql += condKeys.get(i)+"="+condValues.get(i);
		}

		util.logger.debug(sql);
		try (Connection conn = util.connectSQLite();
		PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
		} catch (SQLException ex) {
			util.logger.warn("DB SQLite: Error at DELETE\nrequest: {}", sql, ex);
		}
	}

	private String quote(Object value) {
		// Convert to string and replace '(single quote) with ''(2 single quotes) for sql
		String str = String.valueOf(value);
		if (str == "NULL") {
			return str;
		}
		return "'" + String.valueOf(value).replaceAll("'", "''") + "'"; // smt's -> 'smt''s'
	}

}
