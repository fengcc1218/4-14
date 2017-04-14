package com.vaadin.data.util.sqlcontainer.query.generator.filter;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.filter.LowercaseFilter;
import com.vaadin.data.util.sqlcontainer.query.generator.StatementHelper;

@SuppressWarnings("serial")
public class LowercaseTranslator implements FilterTranslator {

	@Override
    public boolean translatesFilter(Filter filter) {
        return filter instanceof LowercaseFilter;
    }
	
	@Override
    public String getWhereStringForFilter(Filter filter, StatementHelper sh) {
		LowercaseFilter lcf = (LowercaseFilter) filter;
		sh.addParameterValue(lcf.getFilterString());
		return " lower(" + QueryBuilder.quote(lcf.getPropertyId()) + ") = lower(?)";
	}
}
