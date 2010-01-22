/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class HeaderClause implements Cloneable {
	
	private static final String INTERNAL_LIST_SEPARATOR = ";";
	private static final String INTERNAL_LIST_SEPARATOR_NEWLINES = INTERNAL_LIST_SEPARATOR + "\\\n\t\t";

	protected String name;
	protected final Map<String,String> attribs;
	
	public HeaderClause(String name, Map<String, String> attribs) {
		assert name != null;
		assert attribs != null;
		
		this.name = name;
		this.attribs = attribs;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public Map<String, String> getAttribs() {
		return attribs;
	}
	public List<String> getListAttrib(String attrib) {
		String string = attribs.get(attrib);
		if(string == null)
			return null;
		
		List<String> result = new ArrayList<String>();	
		StringTokenizer tokenizer = new StringTokenizer(string, ",");
		while(tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken().trim());
		}
		
		return result;
	}
	public void setListAttrib(String attrib, Collection<? extends String> value) {
		if(value == null || value.isEmpty())
			attribs.remove(attrib);
		else {
			StringBuilder buffer = new StringBuilder();
			boolean first = true;
			for (String string : value) {
				if(!first)
					buffer.append(',');
				buffer.append(string);
				first = false;
			}
			attribs.put(attrib, buffer.toString());
		}
	}
	public void formatTo(StringBuilder buffer) {
		String separator = newlinesBetweenAttributes() ? INTERNAL_LIST_SEPARATOR_NEWLINES : INTERNAL_LIST_SEPARATOR;
		buffer.append(name);
		for(Iterator<Entry<String,String>> iter = attribs.entrySet().iterator(); iter.hasNext(); ) {
			Entry<String, String> entry = iter.next();
			String name = entry.getKey();
			String value = entry.getValue();
			
			if(value != null && value.length() > 0) {
				buffer.append(separator);
			
				// Quote commas in the value
				value = value.replaceAll(",", "','");
				buffer.append(name).append('=').append(value);
			}
		}
	}
	protected boolean newlinesBetweenAttributes() {
		return false;
	}
	@Override
	public HeaderClause clone() {
		return new HeaderClause(this.name, new HashMap<String, String>(this.attribs));
	}
}
