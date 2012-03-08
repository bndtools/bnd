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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import aQute.lib.osgi.Resource;

public class ByteArrayResource implements Resource {
	
	byte[] bytes;
	String extra;
	long lastModified;
	
	public ByteArrayResource(byte[] bytes) {
		this.bytes = bytes;
		this.lastModified = System.currentTimeMillis();
	}
	public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(bytes);
	}

	public void write(OutputStream out) throws IOException {
		out.write(bytes);
	}

	public long lastModified() {
		return lastModified;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getExtra() {
		return extra;
	}
	
	public long size() throws Exception {
		return bytes.length;
	}

}
