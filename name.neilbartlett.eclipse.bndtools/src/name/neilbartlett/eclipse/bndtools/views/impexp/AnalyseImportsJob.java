package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.eclipse.EclipseClasspath;

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
		Builder builder = new Builder();
		
		// Set up the builder classpath
		try {
			File projectDir = file.getProject().getLocation().toFile();
			EclipseClasspath eclipseClasspath = new EclipseClasspath(null, projectDir.getParentFile(), projectDir);
			Set<File> classpath = eclipseClasspath.getClasspath();
			
			builder.setClasspath(classpath.toArray(new File[classpath.size()]));
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error setting up analyser classpath.", e);
		}
		
		// Read the properties
		try {
			Properties props = new Properties();
			InputStream content = file.getContents();
			props.load(content);
			builder.setProperties(props);
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		}
		
		// Calculate the manifest
		try {
			builder.build();
			Manifest manifest = builder.getJar().getManifest();
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
			return Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error calculating imports/exports.", e);
		}
	}
	


}
