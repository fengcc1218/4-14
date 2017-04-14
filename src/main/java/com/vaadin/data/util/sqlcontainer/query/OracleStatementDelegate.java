package com.vaadin.data.util.sqlcontainer.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.util.sqlcontainer.query.OrderBy;
import com.vaadin.data.util.sqlcontainer.query.generator.StatementHelper;
import com.vaadin.data.util.sqlcontainer.query.generator.filter.QueryBuilder;

@SuppressWarnings("serial")
public class OracleStatementDelegate implements FreeformStatementDelegate {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	private String queryStatement = null;
	private String rowQueryStatement = null;
	protected List<Filter> filters;
	protected List<OrderBy> orderBys;

	public void setQueryStatement(String queryStatement) {
		this.queryStatement = queryStatement + " ";
		setCountStatement("select count(*) from (" + queryStatement + ")");
	}

	public String getUnmodifiedQueryStatement() {
		return this.queryStatement;
	}

	public void setRowQueryStatement(String rowQueryStatement) {
		this.rowQueryStatement = rowQueryStatement;
	}

	public String getUnmodifiedRowQueryStatement() {
		return this.rowQueryStatement;
	}

	@Deprecated
	@Override
	public String getQueryString(int offset, int limit) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Use getQueryStatement instead.");
	}

	@Deprecated
	@Override
	public String getCountQuery() {
		throw new UnsupportedOperationException("Use getCountStatement instead.");
	}

	@Override
	public void setFilters(List<Filter> filters) {
		if (filters != null && filters.size() > 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("Filters being set {}", filters.size());
			}
			this.filters = filters;
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Filters is null or empty");
			}
		}
	}

	@Override
	public void setOrderBy(List<OrderBy> orderBys) throws UnsupportedOperationException {
		this.orderBys = orderBys;
	}

	@Override
	public int storeRow(Connection conn, Item row) throws UnsupportedOperationException, SQLException {
		throw new UnsupportedOperationException("Cannot Save Rows");
	}

	@Override
	public boolean removeRow(Connection conn, Item row) throws UnsupportedOperationException, SQLException {
		throw new UnsupportedOperationException("Cannot Delete Rows");
	}

	@Deprecated
	@Override
	public String getContainsRowQueryString(Object... keys) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Use getContainsRowQueryStatement instead.");
	}

	@Override
	public StatementHelper getQueryStatement(int offset, int limit) {
		StatementHelper sh = new StatementHelper();
		StringBuffer query = new StringBuffer(queryStatement);

		if (filters != null && !filters.isEmpty()) {

			query.append(QueryBuilder.getWhereStringForFilters(filters, sh));

		}

		/*
		 * 
		 * Differences between this an old OracleStatementDelegate
		 * 
		 * I moved order appending section to before the offset and limit
		 * calculation Also added support for NLSSortedOrderBy
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
					query.append("CASE ");
					int x = 1;
					for (String s : ((ExplicitOrderBy) orderBy).getValues()) {
						query.append(" WHEN ");
						query.append(QueryBuilder.quote(orderBy.getColumn()));
						query.append(" = '");
						query.append(s);
						query.append("' THEN ");
						query.append(x);
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

		if (offset != 0 || limit != 0) {
			logger.debug("offset = {}, limit = {}", new Object[] { offset, limit });

			if (offset == 0 && limit == 1) {
				// limit = 200;
			}

			/* +FIRST_ROWS(200) */
			query.insert(0, "select /*+FIRST_ROWS(200)*/ * from ( select a.*, rownum r from ( ");
			query.append(" ) a ) where r between " + (offset + 1) + " and " + (offset + limit));
			/*
			 * query.append(" ) a )  where r between " + offset + " and " +
			 * (offset + limit - 1));
			 */
		}

		if (logger.isDebugEnabled()) {
			logger.debug(query.toString());
		}

		sh.setQueryString(query.toString());
		return sh;
	}

	protected String countStatement = null;

	public void setCountStatement(String countStatement) {
		this.countStatement = countStatement + " ";
	}

	public String getUnmodifiedCountStatement() {
		return this.countStatement;
	}

	@Override
	public StatementHelper getCountStatement() throws UnsupportedOperationException {
		StatementHelper sh = new StatementHelper();
		if (countStatement != null) {
			StringBuffer query = new StringBuffer(countStatement);
			if (filters != null) {
				try {
					query.append(QueryBuilder.getWhereStringForFilters(filters, sh));
				} catch (Exception e) {
					if (logger.isErrorEnabled()) {
						logger.error("Exception occurred in getCountStatement", e);
					}
				}
			}
			sh.setQueryString(query.toString());
		}
		return sh;

	}

	/**
	 * Added filters to the contains query, because otherwise it doesn't include
	 * the filters and containsId() will always be true if the itemId is in the
	 * container regardless of applied filters. The way that the FreeformQuery
	 * uses the PreparedStatement and sets the values, the Primary key needs to
	 * be the last parameter. Which means that the rowQueryStatement needs to be
	 * rearranged.
	 */
	@Override
	public StatementHelper getContainsRowQueryStatement(Object... keys) throws UnsupportedOperationException {
		StatementHelper sh = new StatementHelper();
		// System.out.println("Row Query Statement: " + rowQueryStatement);
		StringBuffer query = new StringBuffer(rowQueryStatement);
		logger.debug("query = {}", query.toString());
		if (filters != null) {
			// apply filters without the "where" part of the where clause
			String[] str = query.toString().toLowerCase().split("where");
			System.out.println(str.length);
			query = new StringBuffer(str[0]);
			query.append(QueryBuilder.getWhereStringForFilters(filters, sh));
			if (query.toString().toLowerCase().contains("where")) {
				query.append(" AND ");
			} else {
				query.append(" WHERE ");
			}
			query.append(str[1]);
		}
		sh.addParameterValue(keys[0]);
		sh.setQueryString(query.toString());
		logger.debug("getContainsRowQueryStatement() = {}", sh.getQueryString());
		return sh;
	}

}
