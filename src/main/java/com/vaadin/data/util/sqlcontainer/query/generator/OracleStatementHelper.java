package com.vaadin.data.util.sqlcontainer.query.generator;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.missouri.operations.data.OracleBoolean;
import edu.missouri.operations.data.OracleString;

/**
 * Extended Vaadin's StatementHelper, because it would only call {@link java.sql.PreparedStatement#setObject(int, Object)}.
 * This handles the Oracle objects in Projex4/Archives by using {@link java.lang.Class#isAssignableFrom(Class)} instead of
 * {@link java.lang.Class#equals(Object)} since some of our objects extend the base types. It handles the situation where
 * OracleBoolean would use {@link java.lang.Class#toString()} to set a value instead of using the BigDecimal value.
 * @author reynoldsjj
 *
 */
@SuppressWarnings("serial")
public class OracleStatementHelper extends StatementHelper {

	transient protected final static Logger logger = LoggerFactory.getLogger(OracleStatementHelper.class);
	
	@Override
	public void setParameterValuesToStatement(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) == null) {
                handleNullValue(i, pstmt);
            } else {
            	handleValue(i, pstmt);
            }
        }
	}
	
	 protected void handleValue(int i, PreparedStatement pstmt) throws SQLException {
		 Class<? extends Object> type = parameters.get(i).getClass();
		 Object value = parameters.get(i);

		 if (OracleString.class.isAssignableFrom(type)) {
			 pstmt.setString(i+1, ((OracleString) value).getValue());
		 } else if (BigDecimal.class.isAssignableFrom(type)) {
			 // added OracleBoolean because it overrides toString() which messes things up.
			 if (OracleBoolean.class.isAssignableFrom(type)) {
				 if (OracleBoolean.TRUE.equals((OracleBoolean) parameters.get(i))) {
					 pstmt.setInt(i+1, 1);
				 } else {
					 pstmt.setInt(i+1, 0);
				 }
			 } else {
				 pstmt.setBigDecimal(i+1, (BigDecimal) value);
			 }
		 } else if (Boolean.class.isAssignableFrom(type)) {
			 pstmt.setBoolean(i+1, (Boolean) value);
		 } else if (Byte.class.isAssignableFrom(type)) {
			 pstmt.setByte(i+1, (Byte) value);
		 } else if (Date.class.isAssignableFrom(type)) {
			 pstmt.setDate(i+1, (Date) value);
		 } else if (java.util.Date.class.isAssignableFrom(type)) {
			 pstmt.setDate(i+1, new Date(((java.util.Date) value).getTime()));
		 } else if (Double.class.isAssignableFrom(type)) {
			 pstmt.setDouble(i+1, (Double) value);
		 } else if (Float.class.isAssignableFrom(type)) {
			 pstmt.setFloat(i+1, (Float) value);
		 } else if (Integer.class.isAssignableFrom(type)) {
			 pstmt.setInt(i+1, (Integer) value);
		 } else if (Long.class.isAssignableFrom(type)) {
			 pstmt.setLong(i+1, (Long) value);
		 } else if (Short.class.isAssignableFrom(type)) {
			 pstmt.setShort(i+1, (Short) value);
		 } else if (String.class.isAssignableFrom(type)) {
			 pstmt.setString(i+1, value.toString());
		 } else if (Time.class.isAssignableFrom(type)) {
			 pstmt.setTime(i+1, (Time) value);
		 } else if (Timestamp.class.isAssignableFrom(type)) {
			 pstmt.setTimestamp(i+1, (Timestamp) value);
		 } else {
			 
			 throw new SQLException("Data type not supported by SQLContainer: " 
					 + parameters.get(i).getClass().toString() + ". You will need to add the type to OracleStatementHelper");
		 }
	 }

}
