---
layout: default
class: Macro
title: system_allow_fail ';' STRING ( ';' STRING )?
summary: Execute a system command but ignore any failures
---



	public String _system(String args[]) throws Exception {
		return system_internal(false, args);
	}

	public String _system_allow_fail(String args[]) throws Exception {
		String result = "";
		try {
			result = system_internal(true, args);
		}
		catch (Throwable t) {
			/* ignore */
		}
		return result;
	}

		/**
	 * System command. Execute a command and insert the result.
	 * 
	 * @param args
	 * @param help
	 * @param patterns
	 * @param low
	 * @param high
	 */
	public String system_internal(boolean allowFail, String args[]) throws Exception {
		if (nosystem)
			throw new RuntimeException("Macros in this mode cannot excute system commands");

		verifyCommand(args, "${" + (allowFail ? "system-allow-fail" : "system")
				+ ";<command>[;<in>]}, execute a system command", null, 2, 3);
		String command = args[1];
		String input = null;

		if (args.length > 2) {
			input = args[2];
		}
		
		if ( File.separatorChar == '\\')
			command = "cmd /c \"" + command + "\"";
		

		Process process = Runtime.getRuntime().exec(command, null, domain.getBase());
		if (input != null) {
			process.getOutputStream().write(input.getBytes("UTF-8"));
		}
		process.getOutputStream().close();

		String s = IO.collect(process.getInputStream(), "UTF-8");
		int exitValue = process.waitFor();
		if (exitValue != 0)
			return exitValue + "";

		if (exitValue != 0) {
			if (!allowFail) {
				domain.error("System command " + command + " failed with exit code " + exitValue);
			} else {
				domain.warning("System command " + command + " failed with exit code " + exitValue + " (allowed)");

			}
		}

		return s.trim();
	}
	