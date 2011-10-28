/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package test.diff;

import java.io.*;
import java.net.*;
import java.util.jar.*;

import aQute.lib.io.*;
import aQute.lib.jardiff.java.*;
import aQute.lib.osgi.*;

public class Utils {

	public static ClassInfo getClassInfo(Class<?> theClass) throws Exception {
		
		Resource resource = createResource(theClass, null);
		return new ClassInfo(null, getClassResourceName(theClass), resource);
	}
	
	
	public static Resource createResource(Manifest mf, String extra) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mf.write(baos);
		
		ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
		resource.setExtra(extra);
		return resource;
	}
	
	public static Resource createResource(Class<?> clazz, String extra) throws IOException {

		String name = getClassResourceName(clazz);
		URL url = clazz.getClassLoader().getResource(name);
		
		URLConnection conn = url.openConnection();
		long lastModified = conn.getLastModified();

		InputStream is = conn.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		IO.copy(is, baos);

		ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
		if (lastModified != 0) {
			resource.setLastModified(lastModified);
		}
		resource.setExtra(extra);
		return resource;
	}

	public static String getClassResourceName(Class<?> clazz) {
		return clazz.getName().replace('.', '/') + ".class";
	}
}
