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
public class NLSSortedOrderBy extends OrderBy implements Serializable {
	final static Logger logger = LoggerFactory.getLogger(NLSSortedOrderBy.class);
    private String nlssetting;

    public NLSSortedOrderBy(String column, boolean isAscending) {
    	super(column, isAscending);
    }
    
    public NLSSortedOrderBy(String column, boolean isAscending, String nlssetting) {
    	super(column, isAscending);
    	setNLSSetting(nlssetting);
    }
    
	/**
	 * @return the nlssetting
	 */
	public String getNLSSetting() {
		if(logger.isDebugEnabled()) {
			logger.debug("NLSSetting = " + nlssetting);
		}
		return nlssetting;
	}

	/**
	 * @param nlssetting the nlssetting to set
	 */
	public void setNLSSetting(String nlssetting) {
		this.nlssetting = nlssetting;
	}
}
