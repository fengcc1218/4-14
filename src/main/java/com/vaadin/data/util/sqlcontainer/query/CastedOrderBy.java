package com.vaadin.data.util.sqlcontainer.query;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.util.sqlcontainer.query.OrderBy;

/**
 * OrderBy represents a sorting rule to be applied to a query made by the
 * SQLContainer's QueryDelegate.
 * 
 * The sorting rule is simple and contains only the affected column's name and
 * the direction of the sort.
 */
@SuppressWarnings("serial")
public class CastedOrderBy extends OrderBy implements Serializable {
	final static Logger logger = LoggerFactory.getLogger(CastedOrderBy.class);
    private String castType;

    public CastedOrderBy(String column, boolean isAscending) {
    	super(column, isAscending);
    }
    
    public CastedOrderBy(String column, boolean isAscending, String castType) {
    	super(column, isAscending);
    	setCastType(castType);
    }
    
	/**
	 * @return the castType
	 */
	public String getCastType() {
		return castType;
	}

	/**
	 * @param casttype the casttype to set
	 */
	public void setCastType(String castType) {
		this.castType = castType;
	}
}
