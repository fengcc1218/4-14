package com.vaadin.data.util.sqlcontainer;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.filter.Like;
import com.vaadin.data.util.filter.UnsupportedFilterException;
import com.vaadin.data.util.sqlcontainer.query.OracleQuery;
import com.vaadin.data.util.sqlcontainer.query.OrderBy;
import com.vaadin.data.util.sqlcontainer.query.QueryDelegate;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;

import edu.missouri.operations.data.OracleObjectMapper;

/* 
 * This class implements our enhancements of SQLContainer.
 *
 * Depends on patched version of SQLContainer to expose certain private data 
 * and to allow extension.
 * 
 * Bug Fixes are still to be made to SQLContainer.
 *
 */
@SuppressWarnings("serial")
public class OracleContainer extends SQLContainer {

	private static final Logger logger = LoggerFactory.getLogger(OracleContainer.class);

	private boolean debug = false;

	public interface ObjectMapper {

		public boolean handlesColumn(String columnName);

		public Class<?> assignType(String columnName);

		public Class<?> assignType(Class<?> columnClass, int precision, int scale);

		public Object createObject(Object object, Class<?> columnClass);

		public Object createObject(String columnName, ResultSet rs) throws SQLException;

	}

	protected ObjectMapper objectMapper = null;

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public OracleContainer(QueryDelegate queryDelegate) throws SQLException {

		if (queryDelegate == null) {
			throw new IllegalArgumentException("QueryDelegate must not be null.");
		}
		this.queryDelegate = queryDelegate;
		objectMapper = new OracleObjectMapper();
		getPropertyIds(); // Queries Database.
		cachedItems.setCacheLimit(CACHE_RATIO * getPageLength() + cacheOverlap);

	}

	public OracleContainer(QueryDelegate queryDelegate, ObjectMapper objectMapper) throws SQLException {

		if (queryDelegate == null) {
			throw new IllegalArgumentException("QueryDelegate must not be null.");
		}
		this.queryDelegate = queryDelegate;
		this.objectMapper = objectMapper;
		getPropertyIds(); // Queries Database.
		cachedItems.setCacheLimit(CACHE_RATIO * getPageLength() + cacheOverlap);

	}

	/**
	 * Fetches a page from the data source based on the values of pageLenght and
	 * currentOffset. Also updates the set of primary keys, used in
	 * identification of RowItems.
	 */
	protected void getPage() {
		if (debug)
			logger.debug("getPage()");
		updateCount();
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		// cachedItems.clear(); // Why are we clearing?
		// itemIndexes.clear(); // Why are we clearing?
		try {
			try {
				queryDelegate.setOrderBy(sorters);
			} catch (UnsupportedOperationException e) {
				/* The query Delegate doesn't support sorting. */
				/* No need to do anything. */
				logger.debug("The query Delegate doesn't support sorting", e);
			}
			queryDelegate.beginTransaction();
			int fetchedRows = pageLength * CACHE_RATIO + cacheOverlap;
			rs = queryDelegate.getResults(currentOffset, fetchedRows);
			rsmd = rs.getMetaData();
			List<String> pKeys = queryDelegate.getPrimaryKeyColumns();
			// }
			/* Create new items and column properties */
			ColumnProperty cp = null;
			int rowCount = currentOffset;
			if (!queryDelegate.implementationRespectsPagingLimits()) {
				rowCount = currentOffset = 0;
				setPageLengthInternal(size);
			}
			while (rs.next()) {
				List<ColumnProperty> itemProperties = new ArrayList<ColumnProperty>();
				/* Generate row itemId based on primary key(s) */
				Object[] itemId = new Object[pKeys.size()];
				for (int i = 0; i < pKeys.size(); i++) {
					itemId[i] = rs.getObject(pKeys.get(i));
				}
				RowId id = null;
				if (pKeys.isEmpty()) {
					id = new ReadOnlyRowId(rs.getRow());
				} else {
					id = new RowId(itemId);
				}
				List<String> propertiesToAdd = new ArrayList<String>(propertyIds);
				if (!removedItems.containsKey(id)) {
					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						if (!isColumnIdentifierValid(rsmd.getColumnLabel(i))) {
							continue;
						}
						String colName = rsmd.getColumnLabel(i);

						Object value = rs.getObject(i);
						Class<?> type;
						if (value != null) {

							if (objectMapper == null) {
								type = value.getClass();
							} else {

								if (overrideTypes.containsKey(colName)) {

									if (logger.isDebugEnabled()) {
										// logger.debug("Column Type is
										// overridden for {}",colName);
									}
									type = overrideTypes.get(colName);
									value = objectMapper.createObject(value, type);

								} else if (objectMapper.handlesColumn(colName)) {

									value = objectMapper.createObject(colName, rs);
									type = value.getClass();

								} else {

									if (debug && logger.isDebugEnabled()) {
										logger.debug("Column Type is not overridden for {}", colName);
									}

									value = objectMapper.createObject(value, objectMapper.assignType(value.getClass(), rsmd.getPrecision(i), rsmd.getScale(i)));
									type = value.getClass();
								}

							}

						} else {
							type = Object.class;
							for (String propName : propertyTypes.keySet()) {
								if (propName.equals(rsmd.getColumnLabel(i))) {
									type = propertyTypes.get(propName);
									break;
								}
							}
						}
						/*
						 * In case there are more than one column with the same
						 * name, add only the first one. This can easily happen
						 * if you join many tables where each table has an ID
						 * column.
						 */
						if (propertiesToAdd.contains(colName)) {
							if (debug && logger.isDebugEnabled()) {
								logger.debug("value class = {}, type = {}", value != null ? value.getClass() : null, type);
							}
							cp = new ColumnProperty(colName, propertyReadOnly.get(colName), propertyPersistable.get(colName), propertyNullable.get(colName), propertyPrimaryKey.get(colName), value,
									type);
							itemProperties.add(cp);
							propertiesToAdd.remove(colName);
						}
					}
					if (debug && logger.isDebugEnabled()) {
						logger.debug("Caching index in itemIndexes {} {}", rowCount, id);
					}
					itemIndexes.put(rowCount, id);

					// if an item with the id is contained in the modified
					// cache, then use this record and add it to the cached
					// items. Otherwise create a new item
					int modifiedIndex = indexInModifiedCache(id);
					if (modifiedIndex != -1) {
						cachedItems.put(id, modifiedItems.get(modifiedIndex));
					} else {
						cachedItems.put(id, new RowItem(this, id, itemProperties));
					}

					rowCount++;
				}
			}
			rs.getStatement().close();
			rs.close();
			queryDelegate.commit();
			if (debug && logger.isDebugEnabled()) {
				logger.debug("Fetched {} rows starting from {}", new Object[] { fetchedRows, currentOffset });
			}
		} catch (SQLException e) {
			logger.error("Failed to fetch rows, rolling back", e);
			try {
				queryDelegate.rollback();
			} catch (SQLException e1) {
				logger.error("Failed to roll back", e1);
			}
			try {
				if (rs != null) {
					if (rs.getStatement() != null) {
						rs.getStatement().close();
						rs.close();
					}
				}
			} catch (SQLException e1) {
				logger.error("Failed to close session", e1);
			}
			throw new RuntimeException("Failed to fetch page.", e);
		}
	}

	/**
	 * Fetches property id's (column names and their types) from the data
	 * source.
	 * 
	 * @throws SQLException
	 */
	@Override
	protected void getPropertyIds() throws SQLException {
		propertyIds.clear();
		propertyTypes.clear();
		queryDelegate.setFilters(null);
		queryDelegate.setOrderBy(null);
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		try {
			queryDelegate.beginTransaction();
			rs = queryDelegate.getResults(0, 1);
			rsmd = rs.getMetaData();
			boolean resultExists = rs.next();
			Class<?> type = null;
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				if (!isColumnIdentifierValid(rsmd.getColumnLabel(i))) {
					continue;
				}
				String colName = rsmd.getColumnLabel(i);
				/*
				 * Make sure not to add the same colName twice. This can easily
				 * happen if the SQL query joins many tables with an ID column.
				 */
				if (!propertyIds.contains(colName)) {
					propertyIds.add(colName);
				}

				if (debug && logger.isDebugEnabled()) {
					logger.debug("Determining Type for Column " + colName);
				}

				/* Try to determine the column's JDBC class by all means. */
				if (overrideTypes.containsKey(colName)) {
					// Manual Override of type.
					type = overrideTypes.get(colName);
				} else if (objectMapper != null) {

					if (objectMapper.handlesColumn(colName)) {
						// ObjectMapper Explicit Override of type based on
						// column Name;
						type = objectMapper.assignType(colName);
					} else if (resultExists && rs.getObject(i) != null) {
						// ObjectMapper standardizes based on Objects and
						// resultset metadata.
						type = objectMapper.assignType(rs.getObject(i).getClass(), rsmd.getPrecision(i), rsmd.getScale(i));
					} else {

						if (debug && logger.isDebugEnabled()) {
							logger.debug("Metadata class name = " + rsmd.getColumnClassName(i));
						}

						try {

							/**
							 * ObjectMapper standardizes based on Column Class
							 * returned by JDBC driver and resultset metadata
							 */

							type = objectMapper.assignType(Class.forName(rsmd.getColumnClassName(i)), rsmd.getPrecision(i), rsmd.getScale(i));

						} catch (Exception e) {

							logger.error("Class not found");

							/*
							 * On failure revert to Object and hope for the
							 * best.
							 */
							type = Object.class;
						}
					}

				} else if (resultExists && rs.getObject(i) != null) {

					if (debug && logger.isDebugEnabled()) {
						logger.debug("Default type assignment");
					}

					type = rs.getObject(i).getClass();
				} else {
					try {
						type = Class.forName(rsmd.getColumnClassName(i));
					} catch (Exception e) {

						logger.error("Class not found", e);
						/*
						 * On failure revert to Object and hope for the best.
						 */
						type = Object.class;
					}
				}

				if (debug && logger.isDebugEnabled()) {
					logger.debug("type = {}", type);
				}

				/*
				 * Determine read only and nullability status of the column. A
				 * column is read only if it is reported as either read only or
				 * auto increment by the database, and also it is set as the
				 * version column in a TableQuery delegate.
				 */
				boolean readOnly = rsmd.isAutoIncrement(i) || rsmd.isReadOnly(i);

				boolean persistable = !rsmd.isReadOnly(i);

				if (queryDelegate instanceof TableQuery) {
					if (rsmd.getColumnLabel(i).equals(((TableQuery) queryDelegate).getVersionColumn())) {
						readOnly = true;
					}
				}

				propertyReadOnly.put(colName, readOnly);
				propertyPersistable.put(colName, persistable);
				propertyNullable.put(colName, rsmd.isNullable(i) == ResultSetMetaData.columnNullable);
				propertyPrimaryKey.put(colName, queryDelegate.getPrimaryKeyColumns().contains(rsmd.getColumnLabel(i)));
				propertyTypes.put(colName, type);
			}
			rs.getStatement().close();
			rs.close();
			queryDelegate.commit();

			if (debug && logger.isDebugEnabled()) {
				logger.debug("Property IDs fetched.");
			}

		} catch (SQLException e) {

			logger.error("Failed to fetch property ids, rolling back", e);

			try {
				queryDelegate.rollback();
			} catch (SQLException e1) {
				logger.error("Failed to roll back", e1);
			}

			// TODO Change to JDBC 4 autocloser

			try {

				if (rs != null) {

					if (rs.getStatement() != null) {
						rs.getStatement().close();
					}

					rs.close();
				}

			} catch (SQLException e1) {
				logger.warn("Failed to close session", e1);
			}

			throw e;
		}
	}

	protected final Map<String, Class<?>> overrideTypes = new HashMap<String, Class<?>>();

	public void overrideType(Object propertyId, Class<?> propertyClass) {
		overrideTypes.put(propertyId.toString(), propertyClass);
	}

	@Override
	public Class<?> getType(Object propertyId) {
		if (!propertyIds.contains(propertyId)) {
			return null;
		}
		if (overrideTypes.containsKey(propertyId)) {
			return overrideTypes.get(propertyId);
		}
		return propertyTypes.get(propertyId);
	}

	private boolean autoRefresh = true;

	/**
	 * {@inheritDoc}
	 */

	@Override
	public void addContainerFilter(Filter filter) throws UnsupportedFilterException {
		// filter.setCaseSensitive(!ignoreCase);

		filters.add(filter);
		if (autoRefresh) {
			refresh();
		}
	}

	@Override
	public void removeContainerFilter(Filter filter) {
		filters.remove(filter);
		if (autoRefresh) {
			// added this if statement to bypass refreshing a combobox.
			// if (!isComboBox) {
			refresh();
			// }
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addContainerFilter(Object propertyId, String filterString, boolean ignoreCase, boolean onlyMatchPrefix) {
		if (propertyId == null || !propertyIds.contains(propertyId)) {
			return;
		}

		/* Generate Filter -object */
		String likeStr = onlyMatchPrefix ? filterString + "%" : "%" + filterString + "%";
		Like like = new Like(propertyId.toString(), likeStr);
		like.setCaseSensitive(!ignoreCase);
		filters.add(like);
		if (autoRefresh) {
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeContainerFilters(Object propertyId) {
		ArrayList<Filter> toRemove = new ArrayList<Filter>();
		for (Filter f : filters) {
			if (f.appliesToProperty(propertyId)) {
				toRemove.add(f);
			}
		}
		filters.removeAll(toRemove);
		if (autoRefresh) {
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see removeAllContainerFilters(boolean refresh)
	 */
	@Override
	public void removeAllContainerFilters() {
		filters.clear();
		if (autoRefresh) {
			refresh();
		}
	}

	/**
	 * Adds the given OrderBy to this container and refreshes the container
	 * contents with the new sorting rules.
	 * 
	 * Note that orderBy.getColumn() must return a column name that exists in
	 * this container.
	 * 
	 * @param orderBy
	 *            OrderBy to be added to the container sorting rules
	 */
	@Override
	public void addOrderBy(OrderBy orderBy) {
		if (orderBy == null) {
			return;
		}
		if (!propertyIds.contains(orderBy.getColumn())) {
			throw new IllegalArgumentException("The column given for sorting does not exist in this container.");
		}
		sorters.add(orderBy);
		if (autoRefresh) {
			refresh();
		}
	}

	public void addOrderBy(OrderBy... orderBys) {

		if (orderBys == null) {
			return;
		}

		for (OrderBy orderBy : orderBys) {

			if (!propertyIds.contains(orderBy.getColumn())) {
				throw new IllegalArgumentException("The column given for sorting does not exist in this container.");
			}

			sorters.add(orderBy);
		}

		if (autoRefresh) {
			refresh();
		}
	}

	/**
	 * Justin added to find an item by a specific property and value.
	 * 
	 * @param propertyId
	 * @param value
	 * @return Item found or null
	 * @author reynoldsjj
	 */

	public Item getItemByProperty(Object propertyId, Object value) {
		if (propertyId == null || !propertyIds.contains(propertyId)) {
			return null;
		}
		for (int i = 0; i < size(); i++) {

			if (String.valueOf(getContainerProperty(getIdByIndex(i), propertyId).getValue()).equals(String.valueOf(value))) {
				if (debug && logger.isDebugEnabled()) {
					logger.debug("propertyId = {}, compared {} = {}", new Object[] { propertyId, getContainerProperty(getIdByIndex(i), propertyId).getValue(), value });
				}
				return getItem(getIdByIndex(i));
			}
		}
		return null;
	}

	/**
	 * Quick way to get an item without having to worry about searching through
	 * the entire SQLContainer. {@link SQLContainer#getItem(Object)} will search
	 * through the entire container and load everything into the cache until it
	 * finds the itemId given.
	 * 
	 * @param itemId
	 * @return Item found or null if not found.
	 * @author reynoldsjj
	 */
	public Item getItemById(RowId itemId) {

		// First check if the id is in the added items
		for (int ix = 0; ix < addedItems.size(); ix++) {
			RowItem item = addedItems.get(ix);
			if (item.getId().equals(itemId)) {
				if (itemPassesFilters(item)) {
					return item;
				} else {
					return null;
				}
			}
		}

		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		try {

			rs = ((OracleQuery) queryDelegate).getItemResult(itemId);
			rsmd = rs.getMetaData();

			/* Create new items and column properties */
			ColumnProperty cp = null;
			while (rs.next()) {
				List<ColumnProperty> itemProperties = new ArrayList<ColumnProperty>();

				List<String> propertiesToAdd = new ArrayList<String>(propertyIds);
				if (!removedItems.containsKey(itemId)) {
					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						if (!isColumnIdentifierValid(rsmd.getColumnLabel(i))) {
							continue;
						}
						String colName = rsmd.getColumnLabel(i);

						Object value = rs.getObject(i);
						Class<?> type;
						if (value != null) {

							if (objectMapper == null) {
								type = value.getClass();
							} else {

								if (overrideTypes.containsKey(colName)) {

									if (debug && logger.isDebugEnabled()) {
										logger.debug("Column Type is overridden for {}", colName);
									}
									type = overrideTypes.get(colName);
									value = objectMapper.createObject(value, type);

								} else if (objectMapper.handlesColumn(colName)) {

									value = objectMapper.createObject(colName, rs);
									type = value.getClass();

								} else {

									if (debug && logger.isDebugEnabled()) {
										logger.debug("Column Type is not overridden for {}", colName);
									}

									value = objectMapper.createObject(value, objectMapper.assignType(value.getClass(), rsmd.getPrecision(i), rsmd.getScale(i)));
									type = value.getClass();
								}

							}

						} else {
							type = Object.class;
							for (String propName : propertyTypes.keySet()) {
								if (propName.equals(rsmd.getColumnLabel(i))) {
									type = propertyTypes.get(propName);
									break;
								}
							}
						}

						/*
						 * In case there are more than one column with the same
						 * name, add only the first one. This can easily happen
						 * if you join many tables where each table has an ID
						 * column.
						 */

						if (propertiesToAdd.contains(colName)) {
							if (debug && logger.isDebugEnabled()) {
								logger.debug("value class = {}, type = {}", value != null ? value.getClass() : null, type);
							}
							cp = new ColumnProperty(colName, propertyReadOnly.get(colName), propertyPersistable.get(colName), propertyNullable.get(colName), propertyPrimaryKey.get(colName), value,
									type);
							itemProperties.add(cp);
							propertiesToAdd.remove(colName);
						}
					}

					/**
					 * if an item with the id is contained in the modified
					 * cache, then use this record and add it to the cached
					 * items. Otherwise create a new item
					 */

					int modifiedIndex = indexInModifiedCache(itemId);
					if (modifiedIndex != -1) {
						return modifiedItems.get(modifiedIndex);
					} else {
						return new RowItem(this, itemId, itemProperties);
					}

				}
			}
			rs.getStatement().close();
			rs.close();
		} catch (SQLException e) {
			logger.error("Failed to fetch rows, rolling back", e);
			try {
				if (rs != null) {
					if (rs.getStatement() != null) {
						rs.getStatement().close();
						rs.close();
					}
				}
			} catch (SQLException e1) {
				logger.error("Failed to close session", e1);
			}
			throw new RuntimeException("Failed to fetch page.", e);
		}

		return null;

	}

	/**
	 * @return the autoRefresh
	 */
	public boolean isAutoRefresh() {
		return autoRefresh;
	}

	/**
	 * @param autoRefresh
	 *            the autoRefresh to set
	 */
	public void setAutoRefresh(boolean autoRefresh) {
		this.autoRefresh = autoRefresh;
		if (autoRefresh) {
			refresh();
		}
	}

	protected boolean isComboBox = false;

	public void setComboBox(boolean isComboBox) {
		this.isComboBox = isComboBox;
	}

	protected boolean threadCommit;

	public void setThreadCommit(boolean threadCommit) {
		this.threadCommit = threadCommit;
	}

	public boolean isThreadCommit() {
		return threadCommit;
	}

	/**
	 * Commits all the changes, additions and removals made to the items of this
	 * container.
	 * 
	 * @throws UnsupportedOperationException
	 * @throws SQLException
	 */
	@Override
	public void commit() throws UnsupportedOperationException, SQLException {
		if (logger.isTraceEnabled()) {
			logger.trace("Commiting changes through delegate...");
			logger.trace("removed Items = {}", removedItems.keySet());
			logger.trace("modified Items = {}", modifiedItems);
			logger.trace("added Items = {}", addedItems);
			logger.trace("threadCommit = {}", threadCommit);
		}

		if (!threadCommit) {
			
			try {

			super.commit();
			
			} catch (Exception e) {
				if(logger.isErrorEnabled()) {
					logger.error("Error occurred in OracleContainer.commit - delegate was {}", queryDelegate.getClass().getCanonicalName(), e);
				}
				throw e;
			}

		} else {

			/* Experimental Code */

			Runnable backgroundRunnable = new Runnable() {

				@Override
				public void run() {

					Runnable commitRunnable = new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub

							try {

								queryDelegate.beginTransaction();
								/* Perform buffered deletions */
								for (RowItem item : removedItems.values()) {

									if (item.getId() instanceof TemporaryRowId) {
										removedItems.remove(item);
										modifiedItems.remove(item);
									} else {
										if (!queryDelegate.removeRow(item)) {
											throw new SQLException("Removal failed for row with ID: " + item.getId());
										}
									}
									
								}
								/* Perform buffered modifications */
								for (RowItem item : modifiedItems) {
									/**
									 * JRR - Added below check, because an Item
									 * can be modified and later deleted.
									 * Current ArrayList implementation does not
									 * remove item from modified.
									 */
									if (!removedItems.containsKey(item.getId())) {
										if (queryDelegate.storeRow(item) > 0) {
											/*
											 * Also reset the modified state in
											 * the item in case it is reused
											 * e.g. in a form.
											 */
											item.commit();
										} else {
											queryDelegate.rollback();
											refresh();
											System.err.println(queryDelegate.getClass().getCanonicalName());
											throw new ConcurrentModificationException("Item with the ID '" + item.getId() + "' has been externally modified.");
										}
									}
								}
								/* Perform buffered additions */
								for (RowItem item : addedItems) {
									queryDelegate.storeRow(item);
								}
								queryDelegate.commit();

							} catch (SQLException e) {

								e.printStackTrace();

								try {
									queryDelegate.rollback();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}

							} catch (OptimisticLockException e) {

								try {
									queryDelegate.rollback();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}
								e.printStackTrace();

							}
						}
					};

					Thread commitThread = new Thread(commitRunnable);
					commitThread.start();

					try {
						commitThread.join();
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}

					/* These three are not thread safe */
					removedItems.clear();
					addedItems.clear();
					modifiedItems.clear();
					refresh();
				}
			};

			Thread backgroundThread = new Thread(backgroundRunnable);
			backgroundThread.start();

		}

	}
}
