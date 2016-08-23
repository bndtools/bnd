---
layout: default
class: Workspace
title: driver ( ';' NAME )?
summary: the driver of the environment (e.g. gradle, eclipse, intellij)
---

Added support for an environment driver of bnd. This driver should be set when bnd is started by for example gradle or ant. The driver can be overridden with the -bnd-driver property.
	
	public void testDriver() throws Exception {
		Attrs attrs = new Attrs();
		attrs.put("x", "10");

		Workspace w = new Workspace(tmp);
		
		assertEquals("unset", w.getDriver());
		assertEquals( "unset", w.getReplacer().process("${driver}"));
		assertEquals( "unset", w.getReplacer().process("${driver;unset}"));
		assertEquals( "", w.getReplacer().process("${driver;set}"));
		
		Workspace.setDriver("test");
		assertEquals("test", w.getDriver());
		assertEquals( "test", w.getReplacer().process("${driver}"));
		assertEquals( "test", w.getReplacer().process("${driver;test}"));
		assertEquals( "", w.getReplacer().process("${driver;nottest}"));

		w.setProperty("-bnd-driver", "test2");
		assertEquals("test2", w.getDriver());
		assertEquals( "test2", w.getReplacer().process("${driver}"));
		assertEquals( "test2", w.getReplacer().process("${driver;test2}"));
		assertEquals( "", w.getReplacer().process("${driver;nottest}"));
		
		
		w.close();
	}
	
	/**
	 * Get the bnddriver, can be null if not set. The overallDriver is the
	 * environment that runs this bnd.
	 */
	public String getDriver() {
		if (driver == null) {
			driver = getProperty(Constants.BNDDRIVER, null);
			if ( driver != null)
				driver = driver.trim();
		}

		if (driver != null)
			return driver;
		
		return overallDriver;
	}

	/**
	 * Set the driver of this environment
	 */
	public static void setDriver(String driver) {
		overallDriver = driver;
	}

	/**
	 * Macro to return the driver. Without any arguments, we return the name of
	 * the driver. If there are arguments, we check each of the arguments
	 * against the name of the driver. If it matches, we return the driver name.
	 * If none of the args match the driver name we return an empty string
	 * (which is false).
	 */

	public String _driver(String args[]) {
		if (args.length == 1) {
			return getDriver();
		}
		String driver = getDriver();
		if (driver == null)
			driver = getProperty(Constants.BNDDRIVER);

		if (driver != null) {
			for (int i = 1; i < args.length; i++) {
				if (args[i].equalsIgnoreCase(driver))
					return driver;
			}
		}
		return "";
	}

		public void testGestalt() throws Exception {
		Attrs attrs = new Attrs();
		attrs.put("x", "10");
		Workspace.addGestalt("peter", attrs);
		Workspace w = new Workspace(tmp);
		
		assertEquals( "peter", w.getReplacer().process("${gestalt;peter}"));
		assertEquals( "10", w.getReplacer().process("${gestalt;peter;x}"));
		assertEquals( "10", w.getReplacer().process("${gestalt;peter;x;10}"));
		assertEquals( "", w.getReplacer().process("${gestalt;peter;x;11}"));
		assertEquals( "", w.getReplacer().process("${gestalt;peter;y}"));
		assertEquals( "", w.getReplacer().process("${gestalt;john}"));
		assertEquals( "", w.getReplacer().process("${gestalt;john;x}"));
		assertEquals( "", w.getReplacer().process("${gestalt;john;x;10}"));
		
		w.close();
		w = new Workspace(tmp);
		w.setProperty("-gestalt", "john;z=100, mieke;a=1000, ci");
		assertEquals( "peter", w.getReplacer().process("${gestalt;peter}"));
		assertEquals( "10", w.getReplacer().process("${gestalt;peter;x}"));
		assertEquals( "10", w.getReplacer().process("${gestalt;peter;x;10}"));
		assertEquals( "", w.getReplacer().process("${gestalt;peter;x;11}"));
		assertEquals( "", w.getReplacer().process("${gestalt;peter;y}"));
		assertEquals( "john", w.getReplacer().process("${gestalt;john}"));
		assertEquals( "100", w.getReplacer().process("${gestalt;john;z}"));
		assertEquals( "100", w.getReplacer().process("${gestalt;john;z;100}"));
		assertEquals( "", w.getReplacer().process("${gestalt;john;z;101}"));
		assertEquals( "mieke", w.getReplacer().process("${gestalt;mieke}"));
		assertEquals( "", w.getReplacer().process("${gestalt;mieke;x}"));
		
		w.close();
	}
	
