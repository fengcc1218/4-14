/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.data.util.sqlcontainer.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;

@SuppressWarnings("serial")
public class OracleFreeformQuery extends FreeformQuery {

    public OracleFreeformQuery(OracleStatementDelegate delegate, JDBCConnectionPool connectionPool, String... primaryKeys) {
    	super(connectionPool);
    	
    	if (primaryKeyColumns == null) {
            primaryKeyColumns = Collections.unmodifiableList(new ArrayList<String>());
        } else {
        	primaryKeyColumns = Arrays.asList(primaryKeys);
        }
    	
	    if (primaryKeyColumns.contains("")) {
	        throw new IllegalArgumentException(
	                "The primary key columns contain an empty string!");
	    } else if (delegate == null) {
            throw new IllegalArgumentException(
                    "The delegate may not be null!");
    	} else if (connectionPool == null) {
            throw new IllegalArgumentException(
                    "The connectionPool may not be null!");
        }
	    
		this.queryString = delegate.getUnmodifiedQueryStatement();
	    setDelegate(delegate);
	    
    }

}
