package com.vaadin.data.util.filter;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

/**
 * Prefix filter for ElasticSearch.
 * @author reynoldsjj
 *
 */

@SuppressWarnings("serial")
public class Prefix implements Filter {

	private final Object propertyId;
	private String value;
	private boolean ignoreCase = true;
	
	public Prefix(String propertyId, String value) {
		this.propertyId = propertyId;
		this.value = value;
	}
	
	public Prefix(String propertyId, String value, boolean ignoreCase) {
		this.propertyId = propertyId;
		this.value = value;
		this.ignoreCase = ignoreCase;
	}
	
	@Override
	public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
		final Property<?> p = item.getItemProperty(getPropertyId());
		if (null == p) {
			return false;
		}
		Object otherValue = p.getValue();
		if (null == otherValue) {
			return false;
		}
		
		if (ignoreCase) {
			return otherValue.toString().toLowerCase().startsWith(value.toString().toLowerCase());
		} else {
			return otherValue.toString().startsWith(value.toString());
		}
	
	}

	@Override
	public boolean appliesToProperty(Object propertyId) {
		return getPropertyId().equals(propertyId);
	}

	/**
	 * @return the propertyId
	 */
	public Object getPropertyId() {
		return propertyId;
	}
	
	/**
     * Returns the value to compare the property against.
     * 
     * @return comparison reference value
     */
    public String getValue() {
        return value;
    }
	
}
