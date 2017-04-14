/**
 * 
 */
package edu.missouri.operations.reportstart;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.sqlcontainer.query.OracleQuery;

import edu.missouri.operations.reportcenter.Pools;

/**
 * @author graumannc
 * 
 */
@SuppressWarnings("serial")
public class Users extends OracleQuery {

	static Logger logger = LoggerFactory.getLogger(Users.class);

	public Users() {
		super(Pools.getConnectionPool(Pools.Names.REPORTCENTER));
		setQueryString("select * from users");
		setRowQueryString("select * from users where id = ?");
		setPrimaryKeyColumns("ID");
	}

	@Override
	public int storeRow(Connection conn, Item row) throws UnsupportedOperationException, SQLException {

		java.util.Date start = new java.util.Date();

		int retval = 0;
		try (CallableStatement call = conn.prepareCall("{ ? = call core.user(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")) {

			int x = 1;
			call.registerOutParameter(x++, Types.VARCHAR);

			setString(call, x++, getString(row,"ID"));
			setString(call, x++, getString(row, "ROWSTAMP"));
			setString(call, x++, getString(row, "USERLOGIN"));
			setString(call, x++, getString(row, "FULLNAME"));
			setString(call, x++, getString(row, "SORTNAME"));
			setString(call, x++, getString(row, "EMPLID"));
			setString(call, x++, getString(row, "PASSWORD"));
			setBoolean(call, x++, getOracleBoolean(row, "ISACTIVE"));
			setString(call, x++, getString(row, "SECRETKEY"));
			setTimestamp(call, x++, getOracleTimestamp(row, "CREATED"));
			setString(call, x++, getString(row, "CREATEDBY"));

			retval = call.executeUpdate();
			setLastId(call.getString(1));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Actual User save took {} ms", new java.util.Date().getTime() - start.getTime());
		}
		return retval;
	}

}
