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
package name.neilbartlett.eclipse.bndtools.internal.libs;

import java.util.Collection;
import java.util.Iterator;

public class CollectionStringifier<T> implements Function<Collection<T>, String> {
	
	private final Function<? super T, String> itemStringifier;
	private final String separator;

	public CollectionStringifier(String separator) {
		this(new ObjectStringifier(), separator);
	}
	
	public CollectionStringifier(Function<? super T, String> itemStringifier, String separator) {
		this.itemStringifier = itemStringifier;
		this.separator = separator;
	}

	public String invoke(Collection<T> c) {
		StringBuilder builder = new StringBuilder();
		for(Iterator<T> iter = c.iterator(); iter.hasNext(); ) {
			T item = iter.next();
			builder.append(itemStringifier.invoke(item));
			if(iter.hasNext()) builder.append(separator);
		}
		return builder.toString();
	}

}
