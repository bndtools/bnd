package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.BndFileClasspathCalculator;
import name.neilbartlett.eclipse.bndtools.utils.IClasspathCalculator;
import name.neilbartlett.eclipse.bndtools.utils.ProjectClasspathCalculator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;

public class AnalyseImportsJob extends Job {

	private final IFile file;
	private final IWorkbenchPage page;

	public AnalyseImportsJob(String name, IFile file, IWorkbenchPage page) {
		super(name);
		this.file = file;
		this.page = page;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			Jar jar;
			if(file.getName().endsWith(".bnd")) {
				jar = getJarForBndfile();
			} else {
				jar = new Jar(file.getName(), file.getLocation().toFile());
			}
			showManifest(jar);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		}
	}
	
	Jar getJarForBndfile() throws IOException, CoreException {
		Builder builder = new Builder();
		
		// Read the properties
		Properties props = new Properties();
		InputStream content = file.getContents();
		props.load(content);
		builder.setProperties(props);
		
		// Set up the builder classpath
		IClasspathCalculator classpathCalculator;
		String classpathStr = builder.getProperty(Constants.CLASSPATH);
		if(classpathStr != null) {
			classpathCalculator = new BndFileClasspathCalculator(classpathStr, file.getWorkspace().getRoot(), file.getFullPath());
		} else {
			classpathCalculator = new ProjectClasspathCalculator(JavaCore.create(file.getProject()));
		}
		builder.setClasspath(classpathCalculator.classpathAsFiles().toArray(new File[0]));
		
		// Calculate the manifest
		try {
			builder.build();
			return builder.getJar();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		} finally {
			builder.close();
		}
	}
	protected void showManifest(Jar jar) throws IOException {
		Manifest manifest = jar.getManifest();
		if(manifest != null) {
			Attributes attribs = manifest.getMainAttributes();
			final Map<String, Map<String, String>> imports = Processor.parseHeader(attribs.getValue(Constants.IMPORT_PACKAGE), null);
			final Map<String, Map<String, String>> exports = Processor.parseHeader(attribs.getValue(Constants.EXPORT_PACKAGE), null);
			
			imports.keySet().removeAll(exports.keySet());
			
			Display display = page.getWorkbenchWindow().getShell().getDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					IViewReference viewRef = page.findViewReference(ImportsExportsView.VIEW_ID);
					if(viewRef != null) {
						ImportsExportsView view = (ImportsExportsView) viewRef.getView(false);
						if(view != null) {
							view.setInput(file, imports, exports);
						}
					}
				}
			});
		}
	}
}
