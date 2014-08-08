package aQute.bnd.service.lifecycle;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

/**
 * The base class for a plugin that wants to intercept workspace life cycle
 * events.
 */
public abstract class LifeCyclePlugin {

	private String	name;

	/**
	 * Add this plugin to the workspace.
	 * <p>
	 * This method will by default call {@link #getName()} to get the name of
	 * the plugin and create an {@code cnf/ext/<name>.bnd} file. This file will
	 * create a {@code -plugin.<name>} property in this file with the class name
	 * of this plugin. Implementations can override the protected method
	 * {@link #getPluginSetup()} to return the setup string for the plugin.
	 * <p>
	 * Actual implementations are free to do whatever they want.
	 * 
	 * @param ws
	 * @throws Exception
	 */
	public void add(Workspace ws) throws Exception {
		System.out.println("In add");
		File ext = ws.getFile(Workspace.CNFDIR + "/" + Workspace.EXT);
		ext.mkdirs();

		String p = getPluginSetup();
		if (p != null) {
			File f = new File(ext, getName() + ".bnd");
			IO.store(p, f);
		} else
			ws.trace("no setup for %s",getName());
		

	}

	/**
	 * Return a short name consisting only of simple characters.
	 * <p>
	 * The default implementation returns the last part of the class name minus
	 * a 'Plugin' suffix if it ends with this.
	 * 
	 * @return
	 */
	public String getName() {
		if (name == null) {
			String name = getClass().getName();
			int n = name.lastIndexOf('.');
			this.name = name.substring(n+1).toLowerCase();
			if ( this.name.endsWith("plugin")) {
				this.name = this.name.substring(0, this.name.length()-"plugin".length());
			}
		}
		return name;
	}

	/**
	 * Return a string that can act as the value part of a
	 * {@value Constants#PLUGIN} property.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected String getPluginSetup() throws Exception {
		Formatter f = new Formatter();
		try {
			f.format("#\n" //
					+ "# Plugin %s setup\n" //
					+ "#\n", getName());
			f.format("-plugin.%s = %s", getName(), getClass().getName());
			return f.toString();
		}
		finally {
			f.close();
		}
	}

	/**
	 * Remove this plugin from the workspace.
	 * <p>
	 * The default implementation will just remove the {@code ext/<name>.bnd} file.
	 * 
	 * @param workspace
	 * @throws Exception
	 */
	public void remove(Workspace w) {
		File file = w.getFile("cnf/ext/"+getName()+".bnd");
		IO.delete(file);
	}

	public void opened(Project project) throws Exception {}

	public void close(Project project) throws Exception {}

	public void created(Project project) throws Exception {}

	public void delete(Project project) throws Exception {}
}
