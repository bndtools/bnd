---
layout: default
class: Project
title: -runenv PROPERTIES
summary: Specify a JDB port on invocation when launched outside a debugger so the debugger can attach later. 
---

	public Map<String,String> getRunEnv() {
		String runenv = project.getProperty(Constants.RUNENV);
		if ( runenv != null) {
			return OSGiHeader.parseProperties(runenv);
		}		
		return Collections.emptyMap();
	}
	
	public int launch() throws Exception {
		prepare();
		java = new Command();
		
		
		//
		// Handle the environment
		//
		
		Map<String,String> env = getRunEnv();
		for ( Map.Entry<String,String> e:env.entrySet()) {
			java.var(e.getKey(), e.getValue());
		}
		
		java.add(project.getProperty("java", "java"));
		String javaagent = project.getProperty(Constants.JAVAAGENT);
		if (Processor.isTrue(javaagent)) {
			for (String agent : agents) {
				java.add("-javaagent:" + agent);
			}
		}

		String jdb = getRunJdb();
		if (jdb != null) {
			int port = 1044;
			try {
				port = Integer.parseInt(project.getProperty(Constants.RUNJDB));
			}
			catch (Exception e) {
				// ok, value can also be ok, or on, or true
			}
			String suspend = port > 0 ? "y" : "n";

			java.add("-Xrunjdwp:server=y,transport=dt_socket,address=" + Math.abs(port) + ",suspend=" + suspend);
		}
		
		java.add("-cp");
		java.add(Processor.join(getClasspath(), File.pathSeparator));
		java.addAll(getRunVM());
		java.add(getMainTypeName());
		java.addAll(getRunProgramArgs());
		if (timeout != 0)
			java.setTimeout(timeout + 1000, TimeUnit.MILLISECONDS);

		File cwd = getCwd();
		if (cwd != null)
			java.setCwd(cwd);

		project.trace("cmd line %s", java);
		try {
			int result = java.execute(System.in, System.err, System.err);
			if (result == Integer.MIN_VALUE)
				return TIMEDOUT;
			reportResult(result);
			return result;
		}
		finally {
			cleanup();
			listeners.clear();
		}
	}