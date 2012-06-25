/*
 * Copyright (c) OSGi Alliance (2012). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aQute.lib.osgi;

import java.io.*;

import aQute.libg.command.*;

public class CommandResource extends WriteResource {
	final long		lastModified;
	final Builder	domain;
	final String	command;

	public CommandResource(String command, Builder domain, long lastModified) {
		this.lastModified = lastModified;
		this.domain = domain;
		this.command = command;
	}

	@Override
	public void write(OutputStream out) throws IOException, Exception {
		StringBuilder errors = new StringBuilder();
		StringBuilder stdout = new StringBuilder();
		try {
			domain.trace("executing command %s", command);
			Command cmd = new Command("sh");
			cmd.inherit();
			String oldpath = cmd.var("PATH");

			String path = domain.getProperty("-PATH");
			if (path != null) {
				path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
				path = path.replaceAll("\\$\\{@\\}", oldpath);
				cmd.var("PATH", path);
				domain.trace("PATH: %s", path);
			}
			OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
			int result = cmd.execute(command, stdout, errors);
			osw.append(stdout);
			osw.flush();
			if (result != 0) {
				domain.error("executing command failed %s %s", command, stdout + "\n" + errors);
			}
		}
		catch (Exception e) {
			domain.error("executing command failed %s %s", command, e.getMessage());
		}
	}

	@Override
	public long lastModified() {
		return lastModified;
	}

}
