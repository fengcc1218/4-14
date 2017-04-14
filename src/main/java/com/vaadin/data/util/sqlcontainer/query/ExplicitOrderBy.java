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
public class ExplicitOrderBy extends OrderBy implements Serializable {
	final static Logger logger = LoggerFactory.getLogger(ExplicitOrderBy.class);
	
	String[] values;
	
	public ExplicitOrderBy(String column, String... values) {
		super(column, true);
		this.values = values;
	}

	@Deprecated
    public ExplicitOrderBy(String column, boolean isAscending, String...values) {
    	super(column, true);
    	this.values = values;
    }
    
    public String[] getValues() {
    	return values;
    }
    
}
