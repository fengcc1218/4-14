/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.data.util.sqlcontainer.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.sqlcontainer.RowId;
import com.vaadin.data.util.sqlcontainer.RowItem;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.OrderBy;
import com.vaadin.data.util.sqlcontainer.query.generator.OracleStatementHelper;
import com.vaadin.data.util.sqlcontainer.query.generator.filter.QueryBuilder;
import com.vaadin.ui.UI;

import edu.missouri.operations.data.OracleBoolean;
import edu.missouri.operations.data.OracleCurrency;
import edu.missouri.operations.data.OracleDate;
import edu.missouri.operations.data.OracleDecimal;
import edu.missouri.operations.data.OracleHelper;
import edu.missouri.operations.data.OracleString;
import edu.missouri.operations.data.OracleTimestamp;

/**
 * This is an attempt to simplify FreeformQuery so that we don't have to
 * duplicate code all the time.
 * 
 * Attempt to eliminate delegate reliance.
 * 
 * @author graumannc
 * 
 */
/* TODO Do we really need to implement QueryDelegate anymore ? */
@SuppressWarnings("serial")
public class OracleQuery extends AbstractTransactionalQuery implements QueryDelegate {

	protected static final transient Logger logger = LoggerFactory.getLogger(OracleQuery.class);

	protected String queryString = null;
	protected String rowQueryString = null;
	protected String countString = null;
	protected boolean fastQuery = false;
	protected Object lastId = null;

	protected boolean oracle12Syntax = true;

	public void setOracle12Syntax(boolean oracle12Syntax) {
		this.oracle12Syntax = oracle12Syntax;
	}

	public void setLastId(Object lastId) {
		this.lastId = lastId;
	}

	public Object getLastId() {
		return lastId;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Deprecated
	public void setQueryStatement(String queryString) {
		setQueryString(queryString);
	}

	public void setRowQueryString(String rowQueryString) {
		this.rowQueryString = rowQueryString;
	}

	public String getRowQueryString() {
		return rowQueryString;
	}

	public void setCountString(String countString) {
		this.countString = countString;
	}

	public String getCountString() {
		return countString;
	}

	@Deprecated
	public void setRowQueryStatement(String rowQueryString) {
		setRowQueryString(rowQueryString);
	}

	protected List<String> primaryKeyColumns;

	public void setPrimaryKeyColumns(List<String> primaryKeyColumns) {
		this.primaryKeyColumns = primaryKeyColumns;
	}

	public void setPrimaryKeyColumns(String... primaryKeyColumns) {
		this.primaryKeyColumns = Arrays.asList(primaryKeyColumns);
	}

	/**
	 * Prevent no-parameters instantiation of FreeformQuery
	 */
	@SuppressWarnings("unused")
	private OracleQuery() {
	}
	
	protected JDBCConnectionPool connectionPool;

	public OracleQuery(JDBCConnectionPool connectionPool) {
		super(connectionPool);
		this.connectionPool = connectionPool;
	}

	public List<Filter> combineFilters() {
		ArrayList<Filter> combined = new ArrayList<Filter>();
		if (mandatoryFilters != null && !mandatoryFilters.isEmpty()) {
			combined.addAll(mandatoryFilters);
		}
		if (filters != null && !filters.isEmpty()) {
			combined.addAll(filters);
		}
		return combined;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() throws SQLException {
		return getCount(true);
	}
	
	private boolean ignoreWhereForCounting = false;
	
	public void setIgnoreWhereForCounting(boolean ignoreWhereForCounting) {
		this.ignoreWhereForCounting = ignoreWhereForCounting;
	}

	public int getCount(boolean useExperimentalCode) throws SQLException {
		int count = -1;

		OracleStatementHelper sh = new OracleStatementHelper();
		StringBuffer query;

		if (countString != null) {

			query = new StringBuffer(countString);
			
			if(!ignoreWhereForCounting) {
				
				List<Filter> filters = combineFilters();

				if (filters != null && !filters.isEmpty()) {
					try {
						query.append(QueryBuilder.getWhereStringForFilters(filters, sh));
					} catch (Exception e) {
						if (logger.isErrorEnabled()) {
							logger.error("Exception occurred in getCountStatement", e);
						}
					}
				}
	
			}

		} else {

			if (useExperimentalCode) {

				int index = queryString.indexOf("FROM");
				if (index == -1) {
					index = queryString.indexOf("from");
				}
				query = new StringBuffer("select count(*) ").append(queryString.substring(index));

			} else {
				query = new StringBuffer("select count(*) from (").append(queryString).append(")");
			}

			List<Filter> filters = combineFilters();

			if (filters != null && !filters.isEmpty()) {
				try {
					query.append(QueryBuilder.getWhereStringForFilters(filters, sh));
				} catch (Exception e) {
					if (logger.isErrorEnabled()) {
						logger.error("Exception occurred in getCountStatement", e);
					}
				}
			}

		}

		sh.setQueryString(query.toString());

		try (Connection c = getConnection()) {

			java.util.Date start = new java.util.Date();

			try (PreparedStatement pstmt = c.prepareStatement(sh.getQueryString())) {

				sh.setParameterValuesToStatement(pstmt);

				try (ResultSet rs = pstmt.executeQuery()) {
					rs.next();
					count = rs.getInt(1);

					if (logger.isDebugEnabled()) {
						java.util.Date end = new java.util.Date();
						logger.debug("count statement - {} - took {} ms", sh.getQueryString(), end.getTime() - start.getTime());
					}

				}
			}

		} catch (SQLException e) {
			

			if (useExperimentalCode) {

				if (logger.isErrorEnabled()) {
					logger.error("Error occurred in experimental code for count statement {} - trying fallback", sh.getQueryString());
				}
				return getCount(false);

			} else {

				if (logger.isErrorEnabled()) {
					logger.debug("Error in Count Statement {}", sh.getQueryString(), e);
				}
				throw e;

			}

		}

		if (logger.isDebugEnabled()) {
			logger.debug("Counted {} rows", count);
		}

		return count;
	}

	/**
	 * Used by SQLContainer to quickly get a ResultSet.
	 * 
	 * @param itemId
	 * @return
	 * @throws SQLException
	 * @author reynoldsjj
	 */
	public ResultSet getItemResult(RowId itemId) throws SQLException {

		ResultSet rs = null;

		OracleStatementHelper sh = new OracleStatementHelper();
		StringBuffer query = new StringBuffer(queryString);

		Object[] pKeys = itemId.getId();

		List<Filter> filtersAndKeys = combineFilters();
		int i = 0;
		for (String key : getPrimaryKeyColumns()) {
			filtersAndKeys.add(new Compare.Equal(key, pKeys[i++]));
		}

		// updated to use queryString instead of rowQueryString, so that it can
		// build the where
		// clause based on the primary keys and filters.
		logger.debug("primary key columns = {}", getPrimaryKeyColumns());
		if (filtersAndKeys != null && !filtersAndKeys.isEmpty()) {
			logger.debug("# of filtersAndKeys = {}", filtersAndKeys.size());
			query.append(QueryBuilder.getWhereStringForFilters(filtersAndKeys, sh));
		}

		sh.setQueryString(query.toString());

		try {
			logger.debug(sh.getQueryString());

			PreparedStatement pstmt = getConnection().prepareStatement(sh.getQueryString());
			sh.setParameterValuesToStatement(pstmt);
			rs = pstmt.executeQuery();
			return rs;
		} catch (SQLException e) {
			logger.debug("Error in query", e);
			throw e;
		}

	}

	/**
	 * Fetches the results for the query. This implementation always fetches the
	 * entire record set, ignoring the offset and page length parameters. In
	 * order to support lazy loading of records, you must supply a
	 * FreeformQueryDelegate that implements the
	 * FreeformQueryDelegate.getQueryString(int,int) method.
	 * 
	 * @throws SQLException
	 * 
	 * @see FreeformQueryDelegate#getQueryString(int, int)
	 */
	@Override
	public ResultSet getResults(int offset, int pagelength) throws SQLException {
		ensureTransaction();
		ResultSet rs = null;

		OracleStatementHelper sh = new OracleStatementHelper();
		StringBuffer query = new StringBuffer(queryString);

		List<Filter> filters = combineFilters();

		if (filters != null && !filters.isEmpty()) {
			// QueryBuilder.addFilterTranslator(new InTranslator());
			// QueryBuilder.addFilterTranslator(new InSQLTranslator());
			query.append(QueryBuilder.getWhereStringForFilters(filters, sh));
		}

		/*
		 * 
		 * Differences between this an old OracleStatementDelegate
		 * 
		 * I moved order appending section to before the offset and limit
		 * calculation Also added support for NLSSortedOrderBy and Casted sort
		 */

		if (orderBys != null && !orderBys.isEmpty()) {
			query.append(" ORDER BY ");
			OrderBy lastOrderBy = orderBys.get(orderBys.size() - 1);
			for (OrderBy orderBy : orderBys) {
				if (orderBy instanceof NLSSortedOrderBy) {
					query.append("NLSSORT(");
					query.append(QueryBuilder.quote(orderBy.getColumn()));
					query.append(",'NLS_SORT=");
					query.append(((NLSSortedOrderBy) orderBy).getNLSSetting());
					query.append("')");
				} else if (orderBy instanceof CastedOrderBy) {
					query.append("CAST(");
					query.append(QueryBuilder.quote(orderBy.getColumn()));
					query.append(" AS ");
					query.append(((CastedOrderBy) orderBy).getCastType());
					query.append(")");
				} else if (orderBy instanceof ExplicitOrderBy) {
					query.append(" CASE ");
					int x = 1;
					for (String s : ((ExplicitOrderBy) orderBy).getValues()) {
						query.append(" WHEN ");
						query.append(QueryBuilder.quote(orderBy.getColumn()));
						query.append(" = '");
						query.append(s);
						query.append("' THEN ");
						query.append(x);
						x++;
					}
					query.append(" ELSE ");
					query.append(x);
					query.append(" END ");
				} else {
					query.append(QueryBuilder.quote(orderBy.getColumn()));
				}
				if (orderBy.isAscending()) {
					query.append(" ASC");
				} else {
					query.append(" DESC");
				}
				if (orderBy != lastOrderBy) {
					query.append(", ");
				}
			}
		}

		String fastQueryString = "";
		if (fastQuery) {
			fastQueryString = " /*+FIRST_ROWS(" + (pagelength - offset) + ")*/";
		}

		if (offset == 0 && pagelength == 1) {

			if (!oracle12Syntax) {
				query.insert(0, "select" + fastQueryString + " * from ( select a.*, rownum r from ( ");
				query.append(" ) a ) where r = 1");
			} else {
				query.append(" fetch first row only ");
			}

		} else if (offset != 0 || pagelength != 0) {

			if (!oracle12Syntax) {
				query.insert(0, "select" + fastQueryString + " * from ( select a.*, rownum r from ( ");
				query.append(" ) a ) where r between " + (offset + 1) + " and " + (offset + pagelength));
			} else {
				query.append(" offset " + offset + " rows fetch first " + pagelength + " rows only");
			}

		}

		sh.setQueryString(query.toString());

		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sh.getQueryString());
			sh.setParameterValuesToStatement(pstmt);
			rs = pstmt.executeQuery();
			return rs;
		} catch (SQLException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("offset = {}, pagelength = {}", new Object[] { offset, pagelength });
				logger.debug(query.toString());
				logger.debug(sh.getQueryString());
				logger.debug("Error in query", e);
			}
			throw e;
		}
	}

	// @Override
	public boolean implementationRespectsPagingLimits() {
		return true;
	}

	protected List<Filter> mandatoryFilters = new ArrayList<Filter>();
	
	public void addMandatoryFilters(List<Filter> filters) {
		mandatoryFilters.addAll(filters);
		Filter and = new And(mandatoryFilters.toArray(new Filter[mandatoryFilters.size()]));
		mandatoryFilters = new ArrayList<Filter>();
		mandatoryFilters.add(and);
	}

	public void setMandatoryFilters(List<Filter> filters) {
		mandatoryFilters = filters;
	}

	public List<Filter> getMandatoryFilters() {
		return mandatoryFilters;
	}

	public void setMandatoryFilters(Filter... filters) {
		setMandatoryFilters(Arrays.asList(filters));
	}

	public void removeMandatoryFilters() {
		mandatoryFilters = null;
	}

	protected List<Filter> filters;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vaadin.data.util.sqlcontainer.query.QueryDelegate#setFilters(java
	 * .util.List)
	 */
	@Override
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	public List<Filter> getFilters() {
		return filters;
	}

	public void setFilters(Filter... filters) {
		setFilters(Arrays.asList(filters));
	}

	protected List<OrderBy> orderBys;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vaadin.data.util.sqlcontainer.query.QueryDelegate#setOrderBy(java
	 * .util.List)
	 */
	@Override
	public void setOrderBy(List<OrderBy> orderBys) {
		this.orderBys = orderBys;
	}

	public void setOrderBy(OrderBy... orderBys) {
		setOrderBy(Arrays.asList(orderBys));
	}

	public List<OrderBy> getOrderBy() {
		return orderBys;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vaadin.data.util.sqlcontainer.query.QueryDelegate#storeRow(com.vaadin
	 * .data.util.sqlcontainer.RowItem)
	 */

	protected boolean allowStoringWithoutTransaction = false;

	public void setAllowStoringWithoutTransaction(boolean b) {
		allowStoringWithoutTransaction = b;
	}

	@Override
	public int storeRow(Item row) throws SQLException {
		if (!allowStoringWithoutTransaction) {
			ensureTransaction();
		}

		/* if(logger.isDebugEnabled()) { */

		java.util.Date start = new java.util.Date();
		int retval = storeRow(getConnection(), row);
		System.err.println(this.getClass().getCanonicalName() + " storeRow time = "
				+ (new java.util.Date().getTime() - start.getTime()) + "ms");
		return retval;

		/*
		 * } else { return storeRow(getConnection(),row); }
		 */
	}

	public int storeExternalRow(Item row) throws SQLException {
		Connection conn = null;
		int retval = 0;
		try {
			conn = connectionPool.reserveConnection();
			retval = storeRow(conn, row);
			conn.commit();
			if(logger.isDebugEnabled()) {
				logger.debug("Row should have committed");
			}
			
		} catch (SQLException e) {
			if(logger.isErrorEnabled()) {
				logger.error("Unable to store external row", e);
				
			} 
			throw e;
		} finally{
			connectionPool.releaseConnection(conn);
		}
		
		
		return retval;
	}

	@Deprecated
	public int storeRow(RowItem row) throws SQLException {
		return storeRow((Item) row);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.data.util.sqlcontainer.query.QueryDelegate#removeRow(com.
	 * vaadin .data.util.sqlcontainer.RowItem)
	 */
	@Override
	public boolean removeRow(Item row) throws SQLException {
		ensureTransaction();
		return removeRow(getConnection(), row);
	}

	/**
	 * Override and implement just as you would in FreeformStatementDelegate
	 * 
	 * @param conn
	 * @param row
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws SQLException
	 */
	protected int storeRow(Connection conn, Item row) throws UnsupportedOperationException, SQLException {
		throw new UnsupportedOperationException("Cannot Save Rows");
	}

	@Deprecated
	protected int storeRow(Connection conn, RowItem row) throws UnsupportedOperationException, SQLException {
		throw new UnsupportedOperationException("Cannot Save Rows");
	}

	/**
	 * 
	 * Override and implement just as you would in FreeformStatementDelegate
	 * 
	 * @param conn
	 * @param row
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws SQLException
	 */
	protected boolean removeRow(Connection conn, Item row) throws UnsupportedOperationException, SQLException {
		throw new UnsupportedOperationException("Cannot Delete Rows");
	}
	
	public boolean removeExternalRow(Item row) throws SQLException {
		Connection conn = null;
		boolean retval = false;
		try {
			conn = connectionPool.reserveConnection();
			retval = removeRow(conn, row);
			conn.commit();
			
		} catch (SQLException e) {
			if(logger.isErrorEnabled()) {
				logger.error("Unable to store external row", e);
				
			} 
			throw e;
		} finally{
			connectionPool.releaseConnection(conn);
		}
		
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.data.util.sqlcontainer.query.QueryDelegate#
	 * getPrimaryKeyColumns ()
	 */
	@Override
	public List<String> getPrimaryKeyColumns() {
		return primaryKeyColumns;
	}

	public String getQueryString() {
		return queryString;
	}

	/**
	 * This implementation of the containsRowWithKey method rewrites existing
	 * WHERE clauses in the query string. The logic is, however, not very
	 * complex and some times can do the Wrong Thing<sup>TM</sup>. For the
	 * situations where this logic is not enough, you can implement the
	 * getContainsRowQueryString method in FreeformQueryDelegate and this will
	 * be used instead of the logic.
	 * 
	 * @see FreeformQueryDelegate#getContainsRowQueryString(Object...)
	 * 
	 */
	@Override
	public boolean containsRowWithKey(Object... keys) throws SQLException {
		boolean contains = false;

		// TODO Find out why this is being called upon scrolling.
		// Why does it become true?

		OracleStatementHelper sh = new OracleStatementHelper();
		StringBuffer query = new StringBuffer(queryString);

		List<Filter> filters = combineFilters();

		// updated to use queryString instead of rowQueryString, so that it can
		// build the where
		// clause based on the primary keys and filters.

		if (filters != null && !filters.isEmpty()) {
			query.append(QueryBuilder.getWhereStringForFilters(filters, sh));
			for (int i = 0; i < getPrimaryKeyColumns().size(); i++) {
				query.append(" AND " + getPrimaryKeyColumns().get(i) + " = ? ");
			}
		} else {
			for (int i = 0; i < getPrimaryKeyColumns().size(); i++) {
				if (i == 0) {
					query.append(" WHERE " + getPrimaryKeyColumns().get(i) + " = ? ");
				} else {
					query.append(" AND " + getPrimaryKeyColumns().get(i) + " = ? ");
				}
			}
		}

		if (keys[0] != null) {
			sh.addParameterValue(keys[0]);
		}

		sh.setQueryString(query.toString());

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection c = getConnection();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug(sh.getQueryString());
			}
			pstmt = c.prepareStatement(sh.getQueryString());
			UI.getCurrent().getPage().getLocation();
			sh.setParameterValuesToStatement(pstmt);
			rs = pstmt.executeQuery();
			contains = rs.next();

		} catch (SQLException e) {
			
			System.err.println(UI.getCurrent().getPage().getLocation() + "ContainsRowWithKey = " + sh.getQueryString());
			if (logger.isErrorEnabled()) {
				logger.error("Query has error", e);
			}
			throw e;
			
		} finally {
			releaseConnection(c, pstmt, rs);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Row contains = {}", contains);
		}

		return contains;
	}

	public void setFastQuery(boolean b) {
		this.fastQuery = b;
	}

	public static void setString(PreparedStatement call, int pos, Object value) throws SQLException {
		OracleHelper.setString(call, pos, value);
	}

	public static void setTimestamp(PreparedStatement call, int pos, Object value) throws SQLException {
		OracleHelper.setTimestamp(call, pos, value);
	}

	public static void setBigDecimal(PreparedStatement call, int pos, Object value) throws SQLException {
		OracleHelper.setBigDecimal(call, pos, value);
	}

	public static void setBoolean(PreparedStatement call, int pos, Object value) throws SQLException {
		OracleHelper.setBoolean(call, pos, value);
	}

	/*
	 * These are merely convenience methods to make using an OracleQuery easier.
	 */

	private static Object getValue(Item row, Object id) {
		if (row.getItemProperty(id) != null) {
			return row.getItemProperty(id).getValue();
		} else {
			return null;
		}
	}

	public static String getString(Item row, Object id) {

		if (row.getItemProperty(id) != null) {
			Object o = row.getItemProperty(id).getValue();
			if (o != null) {
				if (o instanceof String) {
					return (String) o;
				} else {
					return o.toString();
				}
			}
		}

		return null;
	}

	public static OracleBoolean getOracleBoolean(Item row, Object id) {
		Object o = getValue(row, id);
		if (o instanceof Boolean) {
			return new OracleBoolean(((Boolean) o).booleanValue());
		} else {
			return (OracleBoolean) getValue(row, id);
		}
	}

	public static OracleDecimal getOracleDecimal(Item row, Object id) {
		Object o = getValue(row, id);
		if (o == null) {
			return null;
		}
		if (o instanceof OracleDecimal) {
			return (OracleDecimal) o;
		} else if (o instanceof BigDecimal) {
			// Handle downcast from BigDecimal without loss of precision.
			return new OracleDecimal((BigDecimal) o);
		} else if (o instanceof BigInteger) {
			// Handle conversion from BigInteger without loss of precision.
			return new OracleDecimal((BigInteger) o);
		} else if (o instanceof Integer) {
			return new OracleDecimal((Integer) o);
		} else {
			return new OracleDecimal(((Number) o).doubleValue());
		}
	}

	public static BigDecimal getDecimal(Item row, Object id) {
		return (BigDecimal) getValue(row, id);
	}

	public static OracleCurrency getOracleCurrency(Item row, Object id) {
		return (OracleCurrency) getValue(row, id);
	}

	public static OracleDate getOracleDate(Item row, Object id) {
		return (OracleDate) getValue(row, id);
	}

	public static OracleTimestamp getOracleTimestamp(Item row, Object id) {

		Object o = getValue(row, id);
		if (o instanceof java.util.Date) {
			return new OracleTimestamp(((java.util.Date) o).getTime());
		} else if (o instanceof java.sql.Date) {
			return new OracleTimestamp(((java.sql.Date) o).getTime());
		} else if (o instanceof OracleDate) {
			return new OracleTimestamp(((OracleDate) o).getTime());
		} else {
			return (OracleTimestamp) getValue(row, id);
		}
	}

	public static OracleString getOracleString(Item row, Object id) {
		return (OracleString) getValue(row, id);
	}

	public static String getId(Item row) {
		Object object = getValue(row, "ID");
		if (object != null) {
			return object.toString();
		} else {
			return null;
		}
	}

	public static String getRowStamp(Item row) {
		return getRowStamp(row, "ROWSTAMP");
	}

	public static String getRowStamp(Item row, Object id) {

		String rowstamp = null;
		Object rowstamp_t = getValue(row, id);
		if (rowstamp_t == null) {
			rowstamp = "AAAA";
		} else {
			rowstamp = rowstamp_t.toString();
		}
		return rowstamp;

	}

	public static OracleTimestamp getNow() {

		return new OracleTimestamp(System.currentTimeMillis());

	}

}
