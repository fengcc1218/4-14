package com.vaadin.data.util.filter;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

/**
 * Terms filter for ElasticSearch.
 * @author reynoldsjj
 *
 */
@SuppressWarnings("serial")
public class Terms implements Filter {

	private final Object propertyId;
	private List<Object> values = new ArrayList<>();
	
	public Terms(String propertyId, Object... values) {
		this.propertyId = propertyId;
		for (Object value : values) {
			this.values.add(value);
		}
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
		
		return values.contains(otherValue);
		
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
	
	public List<Object> getValues() {
		return values;
	}

}
