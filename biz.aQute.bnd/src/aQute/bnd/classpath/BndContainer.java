package aQute.bnd.classpath;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;

public class BndContainer implements IClasspathContainer {

	final static IClasspathEntry	ICLASSPATHENTRY_EMPTY[]	= new IClasspathEntry[0];

	final Project					project;
	volatile int					count;
	volatile IClasspathEntry[]		cachedEntries;

	BndContainer(Project project) {
		this.project = project;
	}

	/**
	 * Get the classpath entries ... seems to be called VERY often
	 */

	public IClasspathEntry[] getClasspathEntries() {
		boolean cached = cachedEntries != null && count == project.getChanged();
		if (!cached) {
			try {
				if (project.lock("get class path " + project) )
				try {
					project.clear();
					count = project.getChanged();
					ArrayList<IClasspathEntry> result = new ArrayList<IClasspathEntry>();
					// fetch the names of all files that match our filter
					List<Container> entries = new ArrayList<Container>();
					entries.addAll(project.getBuildpath());

					// The first file is always the project directory, Eclipse
					// already includes that for us.
					if (entries.size() > 0)
						entries.remove(0);
					else
						; //System.err.println("Huh? Should have the bin dir! " + entries);
					// Eclipse does not know a boot classpath, but it compiles
					// against
					// a jre. We add anything on the bootpath
					entries.addAll(project.getBootclasspath());

					for (Container c : entries) {
						IClasspathEntry cpe;
						IPath sourceAttachment = null;

						if (c.getError() == null) {
							File file = c.getFile();
							assert file.isAbsolute();

							IPath p = Central.toPath(project, file);
							// JDT seems to ignore files when they
							// are outside the workspace
							if (p == null)
								p = Path.fromOSString(file.getAbsolutePath());
							try {
								Central.refresh(p);
							} catch (Throwable e) {

							}
							if (c.getType() == Container.TYPE.PROJECT) {
								File sourceDir = c.getProject().getSrc();
								if (sourceDir.isDirectory())
									sourceAttachment = Central.toPath(c.getProject(), sourceDir);
							}

							cpe = JavaCore.newLibraryEntry(p, sourceAttachment, null);
							result.add(cpe);
						}
					}
					cachedEntries = result.toArray(ICLASSPATHENTRY_EMPTY);
				} finally {
					project.unlock();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return cachedEntries;
	}

	public String getDescription() {
		return "bnd";
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return BndContainerInitializer.ID;
	}

	public Project getModel() {
		return project;
	}
}
