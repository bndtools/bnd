/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package test.bndtools.diff.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AsmClassLoader extends ClassLoader {

	private final Map<String, byte[]> asmClasses = Collections.synchronizedMap(new HashMap<String, byte[]>());
	
	@Override
	public Class<?> findClass(String name) {
		if (!asmClasses.containsKey(name)) {
			return null;
		}
		byte[] b = asmClasses.get(name);
		return defineClass(name, b, 0, b.length);
	}

	public void addClass(String name, byte[] classData) {
		asmClasses.put(name, classData);
	}
}
