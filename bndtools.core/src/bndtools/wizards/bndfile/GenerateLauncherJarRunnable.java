package bndtools.wizards.bndfile;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.PropertiesResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

class GenerateLauncherJarRunnable implements IRunnableWithProgress {

	private final Project	project;
	private final String	path;
	private final boolean	folder;

	GenerateLauncherJarRunnable(Project project, String path, boolean folder) {
		this.project = project;
		this.path = path;
		this.folder = folder;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			Entry<String, Resource> export = project.export("bnd.executablejar", Collections.emptyMap());
			if (export != null) {
				try (JarResource r = (JarResource) export.getValue()) {
					File destination = new File(path);
					Jar jar = r.getJar();
					if (folder) {
						// Set launch.embedded=false since we expanded to folder
						Resource launcherprops = jar.getResource("launcher.properties");
						if (launcherprops != null) {
							PropertiesResource updated;
							if (launcherprops instanceof PropertiesResource) {
								updated = (PropertiesResource) launcherprops;
							} else {
								updated = new PropertiesResource();
								try (InputStream in = launcherprops.openInputStream()) {
									updated.getProperties()
										.load(in);
								}
							}
							updated.getProperties()
								.setProperty("launch.embedded", Boolean.toString(false));
							jar.putResource("launcher.properties", updated);
						}
						jar.writeFolder(destination);
						File start = IO.getFile(destination, "start");
						if (start.isFile()) {
							start.setExecutable(true);
						}
					} else {
						jar.write(destination);
					}
				}
			}
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

}
