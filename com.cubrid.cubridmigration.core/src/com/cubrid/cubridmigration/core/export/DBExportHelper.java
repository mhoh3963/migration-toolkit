/*
 * Copyright (C) 2009 Search Solution Corporation. All rights reserved by Search Solution. 
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met: 
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer. 
 *
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution. 
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without 
 *   specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE. 
 *
 */
package com.cubrid.cubridmigration.core.export;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.common.log.LogUtil;
import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.dbtype.IDependOnDatabaseType;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.config.SourceColumnConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceEntryTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceSQLTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceSequenceConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceTableConfig;
import com.cubrid.cubridmigration.core.export.handler.BlobTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.BytesTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.CharTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.ClobTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.DateTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.DefaultHandler;
import com.cubrid.cubridmigration.core.export.handler.IntTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.LongBytesTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.NumberTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.TimeTypeHandler;
import com.cubrid.cubridmigration.core.export.handler.TimestampTypeHandler;
import com.cubrid.cubridmigration.core.sql.SQLHelper;
import com.cubrid.cubridmigration.cubrid.export.CUBRIDExportHelper;
import com.cubrid.cubridmigration.graph.dbobj.Edge;
import com.cubrid.cubridmigration.graph.dbobj.Vertex;

/**
 * a class help to export database data and verify database sql statement
 * 
 * @author moulinwang Kevin Cao
 * 
 */
public abstract class DBExportHelper implements
		IDependOnDatabaseType {
	protected static final Logger LOG = LogUtil.getLogger(CUBRIDExportHelper.class);

	protected static final int DEFAULT_FETCH_SIZE = 500;

	public static final int LONGNVARCHAR = -16;
	public static final int NCHAR = -15;
	public static final int NCLOB = 2011;
	public static final int NVARCHAR = -9;

	protected static final String PARTITITON_INDEX = "pidx"; // be same as Exporter.PARTITITON_INDEX
	protected Map<Integer, IExportDataHandler> handlerMap1 = new HashMap<Integer, IExportDataHandler>();
	protected Map<String, IExportDataHandler> handlerMap2 = new HashMap<String, IExportDataHandler>();

	public DBExportHelper() {
		handlerMap1.put(Types.CHAR, new CharTypeHandler());
		handlerMap1.put(Types.VARCHAR, new CharTypeHandler());
		handlerMap1.put(Types.NCHAR, new CharTypeHandler());
		handlerMap1.put(Types.NVARCHAR, new CharTypeHandler());

		handlerMap1.put(Types.BINARY, new BytesTypeHandler());
		handlerMap1.put(Types.VARBINARY, new BytesTypeHandler());

		handlerMap1.put(Types.BIT, new DefaultHandler());
		handlerMap1.put(Types.TINYINT, new IntTypeHandler());
		handlerMap1.put(Types.SMALLINT, new IntTypeHandler());
		handlerMap1.put(Types.INTEGER, new DefaultHandler());
		handlerMap1.put(Types.BIGINT, new DefaultHandler());
		handlerMap1.put(Types.REAL, new DefaultHandler());
		handlerMap1.put(Types.FLOAT, new DefaultHandler());
		handlerMap1.put(Types.DOUBLE, new DefaultHandler());

		handlerMap1.put(Types.DECIMAL, new NumberTypeHandler());
		handlerMap1.put(Types.NUMERIC, new NumberTypeHandler());

		handlerMap1.put(Types.DATE, new DateTypeHandler());
		handlerMap1.put(Types.TIME, new TimeTypeHandler());
		handlerMap1.put(Types.TIMESTAMP, new TimestampTypeHandler());

		handlerMap1.put(Types.LONGVARBINARY, new LongBytesTypeHandler());
		handlerMap1.put(Types.BLOB, new BlobTypeHandler());

		handlerMap1.put(Types.LONGVARCHAR, new ClobTypeHandler());
		handlerMap1.put(Types.LONGNVARCHAR, new ClobTypeHandler());
		handlerMap1.put(Types.CLOB, new ClobTypeHandler());
		handlerMap1.put(Types.NCLOB, new ClobTypeHandler());
	}

	/**
	 * get JDBC Object
	 * 
	 * @param rs ResultSet
	 * @param column Column
	 * @return Object
	 * @throws SQLException e
	 */
	public Object getJdbcObject(final ResultSet rs, final Column column) throws SQLException {
		if (column == null) {
			throw new RuntimeException("Column can't be null.");
		}
		Integer dataTypeID = column.getJdbcIDOfDataType();
		if (dataTypeID != null) {
			IExportDataHandler edh = handlerMap1.get(dataTypeID);
			if (edh == null) {
				return rs.getObject(column.getName());
			}
			return edh.getJdbcObject(rs, column);
		}
		LOG.error("Unknown SQL data type:" + column.getDataType() + "(Column name="
				+ column.getName() + ")");
		return null;
	}
	
	public Object getJdbcObjectForCSV(final ResultSet rs, final Column column) throws SQLException {
			if (column == null) {
				throw new RuntimeException("Column can't be null.");
			}
	
		Object obj = rs.getObject(column.getName());
			
			if (obj == null) {
				return "";
			}
			
		return obj;
	}

	/**
	 * return database object name
	 * 
	 * @param objectName String
	 * @return String
	 */
	protected abstract String getQuotedObjName(String objectName);

	/**
	 * to return SELECT SQL
	 * 
	 * @param stc SourceTableConfig
	 * @return String
	 */
	public String getSelectSQL(final SourceTableConfig stc) {
		//SQL table
		if (stc instanceof SourceSQLTableConfig) {
			return ((SourceSQLTableConfig) stc).getSql();
		}
		//Entry table 
		SourceEntryTableConfig setc = (SourceEntryTableConfig) stc;
		StringBuffer buf = new StringBuffer(256);
		buf.append("SELECT ");
		final List<SourceColumnConfig> columnList = setc.getColumnConfigList();
		for (int i = 0; i < columnList.size(); i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(getQuotedObjName(columnList.get(i).getName()));
		}
		buf.append(" FROM ");
		// it will make a query with a schema and table name 
		// if it required a schema name when there create sql such as SCOTT.EMP
		addSchemaPrefix(setc, buf);
		buf.append(getQuotedObjName(setc.getName()));

		String condition = setc.getCondition();
		if (StringUtils.isNotBlank(condition)) {
			condition = condition.trim();
			if (!condition.toLowerCase(Locale.US).startsWith("where")) {
				buf.append(" WHERE ");
			}
			if (condition.trim().endsWith(";")) {
				condition = condition.substring(0, condition.length() - 1);
			}
			buf.append(" ").append(condition);
		}
		return buf.toString();
	}

	/**
	 * If add a schema prefix before the table name.
	 * 
	 * @param setc SourceEntryTableConfig
	 * @param buf StringBuffer
	 */
	protected void addSchemaPrefix(SourceEntryTableConfig setc, StringBuffer buf) {
		if (StringUtils.isNotBlank(setc.getOwner())) {
			buf.append(getQuotedObjName(setc.getOwner())).append(".");
		}
	}

	/**
	 * to return SELECT SQL
	 * 
	 * @param setc SourceEntryTableConfig
	 * @return String
	 */
	public String getSelectCountSQL(final SourceEntryTableConfig setc) {
		StringBuffer buf = new StringBuffer(256);
		buf.append("SELECT COUNT(1) ");
		buf.append(" FROM ");
		// it will make a query with a schema and table name 
		// if it required a schema name when there create sql such as SCOTT.EMP
		addSchemaPrefix(setc, buf);
		buf.append(getQuotedObjName(setc.getName()));

		String condition = setc.getCondition();
		if (StringUtils.isNotBlank(condition)) {
			if (!condition.trim().toLowerCase(Locale.US).startsWith("where")) {
				buf.append(" WHERE ");
			}
			condition = condition.trim();
			if (condition.trim().endsWith(";")) {
				condition = condition.substring(0, condition.length() - 1);
			}
			buf.append(" ").append(condition);
		}
		return buf.toString();
	}

	//	/**
	//	 * return database special table name
	//	 * 
	//	 * @param sourceTable Table
	//	 * @return String
	//	 */
	//	public String getTableNameKey(SourceTable sourceTable) {
	//		return sourceTable.getName();
	//	}

	/**
	 * If source DB is online, fill the tableRowCount attribute of source
	 * tables.
	 * 
	 * The attribute may be used by some other function such as monitor and
	 * reporter.
	 * 
	 * It is not recommended to call this method often because of it may take a
	 * long time.
	 * 
	 * @param config MigrationConfiguration
	 */
	public void fillTablesRowCount(MigrationConfiguration config) {
		if (!config.sourceIsOnline()) {
			return;
		}
		if (config.isImplicitEstimate()) {
			setAllTableRowCountTo0(config);
			return;
		}
		try {
			Connection con = config.getSourceConParams().createConnection();
			if (con == null) {
				return;
			}
			Statement stmt = con.createStatement();
			if (stmt == null) {
				return;
			}

			try {
				for (SourceEntryTableConfig setc : config.getExpEntryTableCfg()) {
					if (!setc.isMigrateData()) {
						continue;
					}
					// It will put an owner name from Table object 
					// whenever counting total records
					// because SourceEntryTableConfig.setOwner() is volatility.
					Table tbl = config.getSrcTableSchema(setc.getOwner(), setc.getName());
					if (tbl != null && StringUtils.isNotEmpty(tbl.getOwner())) {
						setc.setOwner(tbl.getOwner());
					}
					String sql = getSelectCountSQL(setc);
					setTableRowCount(config, stmt, setc.getOwner(), setc.getName(), sql);
				}
				SQLHelper sqlHelper = config.getSourceDBType().getSQLHelper(null);
				for (SourceSQLTableConfig sstc : config.getExpSQLCfg()) {
					if (!sstc.isMigrateData()) {
						continue;
					}
					String cleanSQL = cleanSQLTableSQL(sstc);
					String executableSQL = sqlHelper.replacePageQueryParameters(cleanSQL,
							Long.MAX_VALUE, 0);

					boolean useDerivedQuery = true;

					String trimSql = executableSQL.toLowerCase();
					
					String splitQuery = trimSql.split("return")[0];

					StringBuffer sb = new StringBuffer();

					sb.append(splitQuery);
					sb.append(" return count(*)");
					
					
//					int sp = trimSql.indexOf("select");
//					int ep = trimSql.indexOf("from");
//					int lmt = trimSql.indexOf("limit");
//					if (sp != -1 && ep != -1 && lmt == -1) {
//						sp += "select".length();
//						String pre = executableSQL.substring(0, sp);
//						String post = executableSQL.substring(ep, executableSQL.length());
//						String finalsql = pre + " COUNT(1) " + post;
//						if (setTableRowCount(config, stmt, sstc.getOwner(), sstc.getName(),
//								finalsql)) {
//							useDerivedQuery = false;
//						}
//					}

					if (useDerivedQuery) {
						executableSQL = "SELECT COUNT(1) FROM (" + executableSQL + ") tbl";
						setTableRowCount(config, stmt, sstc.getOwner(), sstc.getName(),
								sb.toString());
					}
				}
			} finally {
				stmt.close();
				con.close();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param sstc SourceSQLTableConfig
	 * @return cleaned SQL
	 */
	protected String cleanSQLTableSQL(SourceSQLTableConfig sstc) {
		String cleanSQL = sstc.getSql().trim();
		if (cleanSQL.endsWith(";")) {
			cleanSQL = new StringBuffer(cleanSQL).deleteCharAt(cleanSQL.length() - 1).toString();
			sstc.setSql(cleanSQL);
		}
		return cleanSQL;
	}

	/**
	 * @param config MigrationConfiguration
	 */
	protected void setAllTableRowCountTo0(MigrationConfiguration config) {
		for (SourceEntryTableConfig setc : config.getExpEntryTableCfg()) {
			if (!setc.isMigrateData()) {
				continue;
			}
			Table tbl = config.getSrcTableSchema(setc.getOwner(), setc.getName());
			if (tbl == null) {
				continue;
			}
			tbl.setTableRowCount(0);
		}

		for (SourceSQLTableConfig sstc : config.getExpSQLCfg()) {
			if (!sstc.isMigrateData()) {
				continue;
			}
			Table tbl = config.getSrcTableSchema(sstc.getOwner(), sstc.getName());
			if (tbl == null) {
				continue;
			}
			tbl.setTableRowCount(0);
		}
	}

	/**
	 * Set a table's row count
	 * 
	 * @param config MigrationConfiguration
	 * @param stat Statement
	 * @param schema table's schema name
	 * @param stname String
	 * @param sql String
	 * 
	 * @return true if setting successfully
	 */
	private boolean setTableRowCount(MigrationConfiguration config, Statement stat, String schema,
			String stname, String sql) {
		Table tbl = config.getSrcTableSchema(schema, stname);
		if (tbl == null) {
			return false;
		}

		try {
			if (config.getSourceType() == DatabaseType.TURBO.getID()) {
				tbl.setTableRowCount(0);
				
				return true;
			} 
			
			ResultSet rs = stat.executeQuery(sql); //NOPMD
			try {
				if (rs.next()) {
					tbl.setTableRowCount(rs.getLong(1));
				} else {
					tbl.setTableRowCount(0);
				}
			} finally {
				rs.close();
			}
		} catch (SQLException e) {
			LOG.error("SQL error", e);
			return false;
		}

		return true;
	}

	/**
	 * config Statement Object
	 * 
	 * @param stmt Statement
	 */
	public void configStatement(Statement stmt) {
		try {
			stmt.setFetchSize(DEFAULT_FETCH_SIZE);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	//	public abstract String getPagedSelectSQL(final Table sourceTable,
	//			final List<SourceColumnConfig> columnList, int rows,
	//			long exportedRecords);

	/**
	 * Retrieves the SQL with page condition
	 * 
	 * @param sql to be change
	 * @param pageSize record count per-page
	 * @param exportedRecords record count to be skipped.
	 * @param pk table's primary key
	 * @return SQL
	 */
	public abstract String getPagedSelectSQL(String sql, long pageSize, long exportedRecords, PK pk);

	public String getPagedSelectSQLForVertexCSV(Vertex v, String sql, long rows, long exportedRecords, PK pk) {
		String cleanSql = sql.toUpperCase().trim();
		
		String editedQuery = editQueryForVertexCSV(v, sql);
		
		StringBuilder buf = new StringBuilder(editedQuery.trim());
		
		Pattern pattern = Pattern.compile("GROUP\\s+BY", Pattern.MULTILINE
				| Pattern.CASE_INSENSITIVE);
		Pattern pattern2 = Pattern.compile("ORDER\\s+BY", Pattern.MULTILINE
				| Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(cleanSql);
		Matcher matcher2 = pattern2.matcher(cleanSql);
		if (matcher.find()) {
			//End with group by 
			if (cleanSql.indexOf("HAVING") < 0) {
				buf.append(" HAVING ");
			} else {
				buf.append(" AND ");
			}
			buf.append(" GROUPBY_NUM() ");
		} else if (matcher2.find()) {
			//End with order by 
			buf.append(" FOR ORDERBY_NUM() ");
		} else {
			StringBuilder orderby = new StringBuilder();
			if (pk != null) {
				// if it has a pk, a pk scan is better than full range scan
				for (String pkCol : pk.getPkColumns()) {
					if (orderby.length() > 0) {
						orderby.append(", ");
					}
					orderby.append("\"").append(pkCol).append("\"");
				}
			}
			if (orderby.length() > 0) {
				buf.append(" ORDER BY ");
				buf.append(orderby);
				buf.append(" FOR ORDERBY_NUM() ");
			} else {
				if (cleanSql.indexOf("WHERE") < 0) {
					buf.append(" WHERE");
				} else {
					buf.append(" AND");
				}
				buf.append(" ROWNUM ");
			}
		}

		return buf.toString(); 
	}
	
	public String getPagedSelectSQLForEdgeCSV(Edge e, String sql, long rows, long exportedRecords, PK pk, boolean hasMultiSchema) {
		StringBuilder buf = new StringBuilder(sql.trim());

		Pattern pattern = Pattern.compile("GROUP\\s+BY", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(sql); 
		
		Pattern pattern2 = Pattern.compile("ORDER\\s+BY", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher matcher2 = pattern2.matcher(sql);
		
		Pattern pattern3 = Pattern.compile("FOR ORDERBY_NUM\\(\\)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher matcher3 = pattern3.matcher(sql);
		
		if (matcher.find()) {
			//End with group by 
			if (sql.indexOf("HAVING") < 0) {
				buf.append(" HAVING ");
			} else {
				buf.append(" AND ");
			}
			buf.append(" GROUPBY_NUM() ");
		} else if (matcher2.find()) {
			//End with order by 
			buf.append(" FOR ORDERBY_NUM() ");
		} else {
			StringBuilder orderby = new StringBuilder();
			if (pk != null) {
				// if it has a pk, a pk scan is better than full range scan
				for (String pkCol : pk.getPkColumns()) {
					if (orderby.length() > 0) {
						orderby.append(", ");
					}
					orderby.append("\"").append(pkCol).append("\"");
				}
			}
			if (orderby.length() > 0) {
				buf.append(" ORDER BY ");
				buf.append(orderby);
				buf.append(" FOR ORDERBY_NUM() ");
			} else {
				if (sql.indexOf("WHERE") < 0) {
					buf.append(" WHERE");
				} else {
					buf.append(" AND");
				}
				buf.append(" ROWNUM ");
			}
		}

		buf.append(" BETWEEN ").append(exportedRecords + 1L);
		buf.append(" AND ").append(exportedRecords + rows);
		
		buf.append(" LIMIT ").append(rows);
		buf.append(" OFFSET ").append(exportedRecords);

		if (hasMultiSchema && matcher3.find()) {
			return buf.toString().replaceAll("for orderby_num\\(\\)", "");
		}
		
		return buf.toString();
	}
	
	private String editQueryForJoinTableEdge(Edge e, String sql, Connection conn, long innerTotalExport, long pageCount) {
		String editString = new String(sql);
		
		String innerTableName = addDoubleQuote(getInnerTable(conn, e));
		
		List<String> refColList = e.getFKColumnNames();
		
		String startVertexName = addDoubleQuote(e.getStartVertexName().toUpperCase());
		String endVertexName = addDoubleQuote(e.getEndVertexName().toUpperCase());
		
		String startIdCol = "\":START_ID(" + e.getStartVertexName().toUpperCase() + ")\"";
		String endIdCol = "\":END_ID(" + e.getEndVertexName().toUpperCase() + ")\"";
		
		String dupStartVertexName = null;
		String dupEndVertexName = null;
		
		String startVertexRownumColumnName = null;
		String endVertexRownumColumnName = null;
		
		if (e.getStartVertexName().equalsIgnoreCase(e.getEndVertexName())) {
			dupStartVertexName = addDoubleQuote(e.getStartVertexName() + "_1");
			dupEndVertexName = addDoubleQuote(e.getEndVertexName() + "_2");
		} else {
			dupStartVertexName = startVertexName;
			dupEndVertexName = endVertexName;
		}
		
		StringBuilder sVertexOrderby = new StringBuilder();
		StringBuilder eVertexOrderby = new StringBuilder();
		
		if (e.getStartVertex() != null) {
			PK pk = e.getStartVertex().getPK();
			if (pk != null) {
				
				if (pk.getPkColumns().size() == 1) {
					startVertexRownumColumnName = pk.getPkColumns().get(1);
				} else {
					startVertexRownumColumnName = "ROWNUM";
				}
				
				// if it has a pk, a pk scan is better than full range scan
				for (String pkCol : pk.getPkColumns()) {
					if (sVertexOrderby.length() > 0) {
						sVertexOrderby.append(", ");
					}
					sVertexOrderby.append(addDoubleQuote(pkCol));
				}
			}
		}
		
		if (e.getEndVertex() != null) {
			PK pk = e.getEndVertex().getPK();
			if (pk != null) {
				
				if (pk.getPkColumns().size() == 1) {
					endVertexRownumColumnName = pk.getPkColumns().get(1);
				} else {
					endVertexRownumColumnName = "ROWNUM";
				}
				
				// if it has a pk, a pk scan is better than full range scan
				for (String pkCol : pk.getPkColumns()) {
					if (eVertexOrderby.length() > 0) {
						eVertexOrderby.append(", ");
					}
					eVertexOrderby.append(addDoubleQuote(pkCol));
				}
			}
		}
		
		String edgeLabel = addDoubleQuote(e.getEdgeLabel().toUpperCase());
		
		Pattern startVertexIdPattern = Pattern.compile(addDoubleQuote(refColList.get(0)), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher startVertexIdMatcher = startVertexIdPattern.matcher(editString);
		
		if (startVertexIdMatcher.find()) {
			String column = "\"main\"." + addDoubleQuote(refColList.get(0));
			
			editString = startVertexIdMatcher.replaceFirst(column);
		}
		
		Pattern startIdPattern = Pattern.compile("\":START_ID", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher startIdMatcher = startIdPattern.matcher(editString);
		
		if (startIdMatcher.find()) {
			String startId = dupStartVertexName + ".\":START_ID";
			
			editString = startIdMatcher.replaceFirst(startId);
		}
		
		Pattern endIdPattern = Pattern.compile("\":END_ID", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);;
		Matcher endIdMatcher = endIdPattern.matcher(editString);
		
		if (endIdMatcher.find()) {
			String endId = dupEndVertexName + ".\":END_ID";
			
			editString = endIdMatcher.replaceFirst(endId);
		}
		
		Pattern endVertexIdPattern = Pattern.compile(addDoubleQuote(refColList.get(1)), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher endVertexIdMatcher = endVertexIdPattern.matcher(editString);
		
		if (endVertexIdMatcher.find()) {
			
			String column = "\"main\"." + addDoubleQuote(refColList.get(1));
			
			editString = endVertexIdMatcher.replaceFirst(column);
		}
		
		Pattern fromPattern = Pattern.compile("FROM\\s.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher fromMatcher = fromPattern.matcher(editString);
		
		if (fromMatcher.find()) {
			StringBuffer buffer = new StringBuffer();
			
			startVertexIdMatcher.reset();
			endVertexIdMatcher.reset();
			
			if (startVertexIdMatcher.find() || endVertexIdMatcher.find()) {
				buffer.append("FROM " + edgeLabel + " as main, " + "(SELECT " + startVertexRownumColumnName + " as " + startIdCol + ", ");
				
				edgeLabel = new String("\"main\"");
				
			} else {
				buffer.append("FROM " + edgeLabel + ", " + "(SELECT " + startVertexRownumColumnName + " as " + startIdCol + ", ");
			}
			
			buffer.append(sVertexOrderby);
			if (sVertexOrderby.indexOf(e.getfkCol2RefMapping().get(refColList.get(0))) == -1) {
				buffer.append(", " + addDoubleQuote(e.getfkCol2RefMapping().get(refColList.get(0))));
			}
			
			buffer.append(" from " + startVertexName);
			
			if (innerTableName.equalsIgnoreCase(startVertexName)) {
				buffer.append(" order by " + sVertexOrderby);
				buffer.append(" for orderby_num()");
				buffer.append(" between ").append(innerTotalExport + 1L);
				buffer.append(" and ").append(innerTotalExport + pageCount);
			}
			
			buffer.append(") as " + dupStartVertexName + ", (SELECT " + endVertexRownumColumnName + " as " + endIdCol + ", ");
			
			buffer.append(eVertexOrderby);
			if (eVertexOrderby.indexOf(e.getfkCol2RefMapping().get(refColList.get(1))) == -1 
					&& e.getEndVertex().getColumnByName(refColList.get(1)) != null) {
				buffer.append(", " + addDoubleQuote(e.getfkCol2RefMapping().get(refColList.get(1))));
			}
			
			buffer.append(" from " + endVertexName);
			
			if (innerTableName.equalsIgnoreCase(endVertexName)) {
				buffer.append(" order by " + eVertexOrderby);
				buffer.append(" for orderby_num()");
				buffer.append(" between ").append(innerTotalExport + 1L);
				buffer.append(" and ").append(innerTotalExport + pageCount);
			}
			
			buffer.append(") as " + dupEndVertexName);
			
			editString = fromMatcher.replaceFirst(buffer.toString());
		}
		
		StringBuffer originalString = new StringBuffer(editString);
		StringBuffer whereBuffer = new StringBuffer();
		
		whereBuffer.append(" where " + dupStartVertexName + ".");
		
		whereBuffer.append(addDoubleQuote(e.getfkCol2RefMapping().get(refColList.get(0))));
		
		whereBuffer.append(" = " + edgeLabel + ".");
		
		whereBuffer.append(addDoubleQuote(refColList.get(0)));
		
		if (e.getEndVertex().getColumnByName(refColList.get(1)) != null) {
			whereBuffer.append(" and " + dupEndVertexName + ".");
			
			whereBuffer.append(addDoubleQuote(e.getfkCol2RefMapping().get(refColList.get(1))));
			
			whereBuffer.append(" = " + edgeLabel + ".");
			
			whereBuffer.append(addDoubleQuote(refColList.get(1)));			
		}
		
		whereBuffer.append(" order by " + edgeLabel + ".");
		
		whereBuffer.append(addDoubleQuote(refColList.get(0)));
		
		if (e.getEndVertex().getColumnByName(refColList.get(1)) != null) {
			whereBuffer.append(", " + edgeLabel + ".");
			
			whereBuffer.append(addDoubleQuote(refColList.get(1)));
		}
		
		originalString.append(whereBuffer.toString());
		
		editString = originalString.toString();
		
		return editString;
	}
	
	public String addDoubleQuote(String str) {
		return "\"" + str + "\"";
	}
	
	private String editQueryForVertexCSV(Vertex v, String sql) {
		Pattern selectPattern = Pattern.compile("SELECT\\s", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher selectMatcher = selectPattern.matcher(sql);
		
		String vertexIdColumnValue = "ROWNUM";
		
//		if (v.getHasPK() && v.getPK().getPkColumns().size() == 1) {
//			Column col = v.getColumnByName(v.getPK().getPkColumns().get(0));
//			if (col.getGraphDataType().equalsIgnoreCase("INTEGER")) {
//				vertexIdColumnValue = v.getPK().getPkColumns().get(0);
//			}
//		}
		
		if (selectMatcher.find()) {
			StringBuffer buffer = new StringBuffer("SELECT " + vertexIdColumnValue + " as ");
			
			sql = selectMatcher.replaceFirst(buffer.toString());
		}
		
		return sql;
	}
	
	/**
	 * Is support fast search with PK.
	 * 
	 * @param conn Connection
	 * @return true or false
	 */
	public boolean supportFastSearchWithPK(Connection conn) {
		return false;
	}

	/**
	 * Retrieves the current value of input serial.
	 * 
	 * @param sourceConParams JDBC connection configuration
	 * @param sq sequence to be synchronized.
	 * @return The current value of the input SQ
	 */
	public BigInteger getSerialStartValue(ConnParameters sourceConParams, SourceSequenceConfig sq) {
		return null;
	}
	
	public abstract String getGraphSelectSQL(Edge e, boolean targetIsCSV);

	public String getGraphSelectSQL(Vertex v, boolean isTargetCSV) {
		StringBuffer buf = new StringBuffer(256);
		buf.append("SELECT ");

		final List<Column> columnList = v.getColumnList();
		for (int i = 0; i < columnList.size(); i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(getQuotedObjName(columnList.get(i).getName()));
		}
		buf.append(" FROM ");
		// it will make a query with a schema and table name 
		// if it required a schema name when there create sql such as SCOTT.EMP
		addGraphSchemaPrefix(v, buf);
		buf.append(getQuotedObjName(v.getTableName()));

//		String condition = setc.getCondition();
//		if (StringUtils.isNotBlank(condition)) {
//			condition = condition.trim();
//			if (!condition.toLowerCase(Locale.US).startsWith("where")) {
//				buf.append(" WHERE ");
//			}
//			if (condition.trim().endsWith(";")) {
//				condition = condition.substring(0, condition.length() - 1);
//			}
//			buf.append(" ").append(condition);
//		}
		return buf.toString();
	}
	
	public String getGraphSelectSQL(Edge e) {
		StringBuffer buf = new StringBuffer(256);
		buf.append("SELECT /*+ use_merge */");
		final List<Column> columnList = e.getColumnList();
		for (int i = 0; i < columnList.size(); i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(getQuotedObjName(columnList.get(i).getName()));
		}
		buf.append(" FROM ");
		// it will make a query with a schema and table name 
		// if it required a schema name when there create sql such as SCOTT.EMP
		addGraphSchemaPrefix(e, buf);
		buf.append(getQuotedObjName(e.getEdgeLabel()));

//		String condition = setc.getCondition();
//		if (StringUtils.isNotBlank(condition)) {
//			condition = condition.trim();
//			if (!condition.toLowerCase(Locale.US).startsWith("where")) {
//				buf.append(" WHERE ");
//			}
//			if (condition.trim().endsWith(";")) {
//				condition = condition.substring(0, condition.length() - 1);
//			}
//			buf.append(" ").append(condition);
//		}
		return buf.toString();
		
	}
	
	public String getGraphSelectCountSQL(final Vertex v) {
		StringBuffer buf = new StringBuffer(256);
		buf.append("SELECT COUNT(1) ");
		buf.append(" FROM ");
		// it will make a query with a schema and table name 
		// if it required a schema name when there create sql such as SCOTT.EMP
		addGraphSchemaPrefix(v, buf);
		buf.append(getQuotedObjName(v.getVertexLabel()));

//		String condition = v.getCondition();
//		if (StringUtils.isNotBlank(condition)) {
//			if (!condition.trim().toLowerCase(Locale.US).startsWith("where")) {
//				buf.append(" WHERE ");
//			}
//			condition = condition.trim();
//			if (condition.trim().endsWith(";")) {
//				condition = condition.substring(0, condition.length() - 1);
//			}
//			buf.append(" ").append(condition);
//		}
		return buf.toString();
	}
	
	protected void addGraphSchemaPrefix(Vertex v, StringBuffer buf) {
		if (StringUtils.isNotBlank(v.getOwner())) {
			buf.append(getQuotedObjName(v.getOwner())).append(".");
		}
	}
	
	protected void addGraphSchemaPrefix(Edge e, StringBuffer buf) {
		if (StringUtils.isNotBlank(e.getOwner())) {
			buf.append(getQuotedObjName(e.getOwner())).append(".");
		}
	}

	public String getFkRecordCounterSql(Edge e, Map<String, String> fkMapping) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPagedFkRecords(Edge e, String sql, long rows, long exportedRecords, boolean hasMultiSchema) {
		StringBuilder buf = new StringBuilder(sql.trim());
		
//		buf.append(" BETWEEN ").append(exportedRecords + 1L);
//		buf.append(" AND ").append(exportedRecords + rows);
		
//		buf.append(" LIMIT ").append(rows);
//		buf.append(" OFFSET ").append(exportedRecords);
		
		return buf.toString();
	}
	
	public String getInnerQuery(Edge e, String sql, Connection conn, long exportedCount, long rows) {
		String cleanSql = sql.toUpperCase().trim();
		
		return editQueryForFk(e, cleanSql, conn, exportedCount, rows);
	}
	
	public String getJoinTableInnerQuery(Edge e, String sql, Connection conn, long innerTotalExported, long pageCount) {
		String cleanSql = sql.trim();
		
		return editQueryForJoinTableEdge(e, cleanSql, conn, innerTotalExported, pageCount);
	}
	
	private String editQueryForFk(Edge e, String sql, Connection conn, long exportedCount, long rows) {
		Map<String, String> fkMapping = e.getfkCol2RefMapping();
		List<String> keySet = e.getFKColumnNames();
		
		String innerTableName = getInnerTable(conn, e);
		
		String startVertexName;
		String endVertexName;
		
		if (e.getStartVertexName().equals(e.getEndVertexName())) {
			startVertexName = e.getStartVertexName() + "_1";
			endVertexName = e.getEndVertexName() + "_2";
		} else {
			startVertexName = e.getStartVertexName();
			endVertexName = e.getEndVertexName();
		}
		
		String fkCol = keySet.get(0);
		String refCol = fkMapping.get(keySet.get(0));
		
		String startVertexRownumColumnName = null;
		String endVertexRownumColumnName = null;
		
		StringBuilder sVertexOrderby = new StringBuilder();
		StringBuilder eVertexOrderby = new StringBuilder();
		
		if (e.getStartVertex() != null) {
			PK pk = e.getStartVertex().getPK();
			if (pk != null) {
				
				if (pk.getPkColumns().size() == 1) {
					startVertexRownumColumnName = pk.getPkColumns().get(1);
				} else {
					startVertexRownumColumnName = "ROWNUM";
				}
				
				// if it has a pk, a pk scan is better than full range scan
				for (String pkCol : pk.getPkColumns()) {
					if (sVertexOrderby.length() > 0) {
						sVertexOrderby.append(", ");
					}
					sVertexOrderby.append(pkCol);
				}
			}
		}
		
		if (e.getEndVertex() != null) {
		
		PK pk = e.getEndVertex().getPK();
		if (pk != null) {
			
				if (pk.getPkColumns().size() == 1) {
					startVertexRownumColumnName = pk.getPkColumns().get(1);
				} else {
					startVertexRownumColumnName = "ROWNUM";
				}
				
				// if it has a pk, a pk scan is better than full range scan
				for (String pkCol : pk.getPkColumns()) {
					if (eVertexOrderby.length() > 0) {
						eVertexOrderby.append(", ");
					}
					eVertexOrderby.append(pkCol);
				}
			}
		}
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("SELECT ");
		buffer.append(startVertexName + ".\":START_ID(" + e.getStartVertexName() + ")\"");
		buffer.append(", ");
		buffer.append(endVertexName + ".\":END_ID(" + e.getEndVertexName() + ")\"");
		
		buffer.append(" FROM ");
		
		buffer.append("(SELECT "+ startVertexRownumColumnName +" as \":START_ID(" + e.getStartVertexName() + ")\"");
		
		if (sVertexOrderby.length() != 0) {
			buffer.append(" ," + sVertexOrderby);
		}
		
		if (sVertexOrderby.indexOf(fkCol) == -1) {
			buffer.append(", " + fkCol);
		}
		buffer.append(" FROM ");
		buffer.append(e.getStartVertexName());
		buffer.append(" order by ");
		buffer.append(sVertexOrderby);
//		buffer.append(" for orderby_num()) as ");
		
		
		if (startVertexName.equals(innerTableName)) {
			buffer.append(" for orderby_num()");
			buffer.append(" BETWEEN ").append(exportedCount + 1L);
			buffer.append(" AND ").append(exportedCount + rows);
		}
		
		buffer.append(") as ");
		
		buffer.append(startVertexName);
		buffer.append(", ");
		
		buffer.append("(SELECT " + endVertexRownumColumnName + " as \":END_ID(" + e.getEndVertexName() + ")\"");
		
		if (eVertexOrderby.length() != 0) {
			buffer.append(" ," + eVertexOrderby);
		}
		
		if (eVertexOrderby.indexOf(refCol) == -1) {
			buffer.append(", " + refCol);
		}
		buffer.append(" FROM ");
		buffer.append(e.getEndVertexName());
		buffer.append(" order by ");
		buffer.append(eVertexOrderby);
//		buffer.append(" for orderby_num()) as ");
		
		
		if (endVertexName.equals(innerTableName)) {
			buffer.append(" for orderby_num()");
			buffer.append(" BETWEEN ").append(exportedCount + 1L);
			buffer.append(" AND ").append(exportedCount + rows);
		}
		
		buffer.append(") as ");
		
		buffer.append(endVertexName);
		
		buffer.append(" where ");
		buffer.append(startVertexName + "." + fkCol);
		buffer.append(" = ");
		buffer.append(endVertexName + "." + refCol);
		
		buffer.append(" and \":START_ID(" + e.getStartVertexName() + ")\"");
		
		return buffer.toString();
	}
	
	/**
	 * return table record cound
	 * 
	 * need to fine inner table
	 * 
	 * @param conn
	 * @param e
	 * @return count of table record
	 */
	
	protected String getInnerTable(Connection conn, Edge e) {
		String startTableName = e.getStartVertexName();
		String endTableName = e.getEndVertexName();
		String returnString = null;
		
		PreparedStatement stmt1 = null;
		PreparedStatement stmt2 = null;
		
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		
		try {
			stmt1 = conn.prepareStatement("SELECT count(*) FROM " + startTableName);
			stmt2 = conn.prepareStatement("SELECT count(*) FROM " + endTableName);
			
			rs1 = stmt1.executeQuery();
			rs2 = stmt2.executeQuery();
			
			rs1.next();
			rs2.next();
			
			int startTableRecordCount = rs1.getInt(1);
			int endTableRecordCount = rs2.getInt(1);
			
			if (startTableRecordCount > endTableRecordCount) {
				returnString = endTableName;
			} else {
				returnString = startTableName;
			}
			
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			Closer.close(stmt1);
			Closer.close(stmt2);
			
			Closer.close(rs1);
			Closer.close(rs2);
		}
		
		if (returnString != null) {
			return returnString;
		} else {
			return null;
		}
	}

	public String getPagedSelectSQL(Vertex v, String sql, long realPageCount,
			long totalExported, PK pk) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPagedSelectSQL(Edge e, String sql, long realPageCount,
			long totalExported, PK pk) {
		// TODO Auto-generated method stub
		return null;
	}
}
