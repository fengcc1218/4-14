/**
 * 
 */
package edu.missouri.operations.reportstart;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.sqlcontainer.query.OracleQuery;

import edu.missouri.operations.reportcenter.Pools;

/**
 * @author graumannc
 * 
 */
@SuppressWarnings("serial")
public class UserLoginHistory extends OracleQuery {

	static final transient Logger logger = LoggerFactory.getLogger(UserLoginHistory.class);

	public UserLoginHistory() {
		super(Pools.getConnectionPool(Pools.Names.REPORTCENTER));
		setQueryString("select * from USERLOGINHISTORY");
		setRowQueryString("select * from USERLOGINHISTORY where id = ?");
		setPrimaryKeyColumns("ID");
	}
	
	public void setLastUserId(String userId) {
		
		setQueryString("select * from LASTUSERLOGINHISTORY");
		setRowQueryString("select * from LASTUSERLOGINHISTORY where id = ?");
		
		removeMandatoryFilters();
		setMandatoryFilters(new Compare.Equal("USERID", userId));
			
	}

	public static void storeItem(Item row) {

		Connection conn = null;
		try {
			conn = Pools.getConnection(Pools.Names.REPORTCENTER);

			int retval = 0;

			try (CallableStatement call = conn.prepareCall("{ ? = call core.userloginhistory(?,?,?,?) }")) {
				int i = 1;
				call.registerOutParameter(i++, Types.VARCHAR);

				setString(call, i++, null);
				setString(call, i++, getString(row, "USERID"));
				setTimestamp(call, i++, getOracleTimestamp(row, "LOGGEDIN"));
				setString(call, i++, getString(row, "IPADDRESS"));

				retval = call.executeUpdate();
			}
			
			conn.commit();

		} catch (SQLException sqle) {
			
			if(logger.isErrorEnabled()) {
				logger.error("Unable to save userloginhistory",sqle);
			}

		} finally {
			Pools.releaseConnection(Pools.Names.REPORTCENTER, conn);
		}
	}
	
	public String lastLoginID(String userid) {
		Connection conn = null;
		String Id = null;

		try {
			conn = Pools.getConnection(Pools.Names.REPORTCENTER);

			try (PreparedStatement stmt = conn
					.prepareStatement("select max(to_number(id)) as id from userloginhistory where userid = ?")) {

				setString(stmt, 1, userid);
				

				try (ResultSet rs = stmt.executeQuery()) {

					if (rs.next()) {
						Id = rs.getString("ID");
					}

				}

			}
		} catch (SQLException sqle) {
			logger.error("Could not retrieve last login id from userLoginHistory {}", userid, sqle);
		} finally {
			Pools.releaseConnection(Pools.Names.REPORTCENTER, conn);
		}

		return Id;
	}

}
