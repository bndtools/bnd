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
package bndtools.release;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import aQute.libg.reporter.Reporter;
import aQute.libg.sed.Replacer;
import aQute.libg.sed.Sed;
import bndtools.diff.JarDiff;
import bndtools.diff.PackageInfo;
import bndtools.release.api.IReleaseParticipant;
import bndtools.release.api.IReleaseParticipant.Scope;
import bndtools.release.api.ReleaseContext;
import bndtools.release.api.ReleaseContext.Error;
import bndtools.release.api.ReleaseUtils;

public class ReleaseHelper {

	public static void updateProject(ReleaseContext context) throws Exception {
		
		Collection<? extends Builder> builders = context.getProject().getBuilder(null).getSubBuilders();
		for (Builder b : builders) {
			JarDiff current = null;
			for (JarDiff jd : context.getJarDiffs()) {
				if (jd.getSymbolicName().equals(b.getBsn())) {
					current = jd;
					break;
				}
			}
			if (current != null) {
				String bundleVersion = current.getSuggestedVersion();
				for (PackageInfo pi : current.getModifiedExportedPackages()) {
					if (pi.getVersion() != null && !pi.getVersion().equals(pi.getSuggestedVersion())) {
						updatePackageInfoFile(context.getProject(), pi);
					}
				}
				
				for (PackageInfo pi : current.getNewExportedPackages()) {
					updatePackageInfoFile(context.getProject(), pi);
				}
				
				if (bundleVersion != null) {
					File file = context.getProject().getPropertiesFile();
					Replacer replacer = context.getProject().getReplacer();
					if (b.getPropertiesFile() != null) {
						file = b.getPropertiesFile();
						replacer = b.getReplacer();
					}
					Sed sed = new Sed(replacer, file);
					sed.replace("(Bundle-Version\\s*(:|=)\\s*)(([0-9]+(\\.[0-9]+(\\.[0-9]+)?)?))",
		                    "$1${version;===;" + bundleVersion + "}");
					sed.doIt();
				}
			}
		}
	}
	
	public static boolean release(ReleaseContext context, List<JarDiff> jarDiffs) throws Exception {

		boolean ret = true;

		List<IReleaseParticipant> participants = Activator.getReleaseParticipants();

		if (!preUpdateProjectVersions(context, participants)) {
			postRelease(context, participants, false);
			displayErrors(context, Scope.PRE_UPDATE_VERSIONS);
			return false;
		}
		
		ReleaseHelper.updateProject(context);
		
		IProject proj = ReleaseUtils.getProject(context.getProject());
		proj.refreshLocal(IResource.DEPTH_INFINITE, context.getProgressMonitor());
		
		if (!preRelease(context, participants)) {
			postRelease(context, participants, false);
			displayErrors(context, Scope.PRE_RELEASE);
			return false;
		}
		
		for (JarDiff jarDiff : jarDiffs) {
			Collection<? extends Builder> builders = context.getProject().getBuilder(null).getSubBuilders();
			Builder builder = null;
			for (Builder b : builders) {
				if (b.getBsn().equals(jarDiff.getSymbolicName())) {
					builder = b;
					break;
				}
			}
			if (builder != null) {
				if (!release(context, participants, builder)) {
					ret = false;
				}
			}
		}
		
		postRelease(context, participants, ret);
		return ret;
	}
	
	private static void handleBuildErrors(ReleaseContext context, Reporter reporter, Jar jar) {
		String symbName = null;
		String version = null;
		if (jar != null) {
			symbName = ReleaseUtils.getBundleSymbolicName(jar);
			version = ReleaseUtils.getBundleVersion(jar);
		}
		for (String message : reporter.getErrors()) {
			context.getErrorHandler().error(symbName, version, message);
		}
	}
	
	private static void displayErrors(ReleaseContext context, Scope scope) {
		
		
		
		final String name = context.getProject().getName();
		final List<Error> errors = context.getErrorHandler().getErrors();
		if (errors.size() > 0) {
			Runnable runnable = new Runnable() {
				public void run() {
					Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
					ErrorDialog error = new ErrorDialog(shell, name, errors);
					error.open();
				}
			};

			if (Display.getCurrent() == null) {
				Display.getDefault().asyncExec(runnable);
			} else {
				runnable.run();
			}
		
		}
		
	}

	private static boolean release(ReleaseContext context, List<IReleaseParticipant> participants, Builder builder) throws Exception {

		context.getProject().refresh();
		context.getProject().setChanged();
		
		Jar jar = builder.build();
		
		handleBuildErrors(context, builder, jar);

		String symbName = ReleaseUtils.getBundleSymbolicName(jar);
		String version = ReleaseUtils.getBundleVersion(jar);

		boolean proceed = preJarRelease(context, participants, jar);
		if (!proceed) {
			postRelease(context, participants, false);
			displayErrors(context, Scope.PRE_JAR_RELEASE);
			return false;
		}
		
		context.getProject().release(context.getRepository().getName(), jar);
		context.getProject().refresh();
	
		File[] files = context.getRepository().get(symbName, '[' + version + ',' + version + ']');
		if (files.length == 1) {
			ReleaseUtils.toResource(files[0]).refreshLocal(IResource.DEPTH_ZERO, null);
			jar = new Jar(files[0]);
		}
		
		context.addReleasedJar(jar);
		
		postJarRelease(context, participants, jar);
		return true;
	}
	
	private static void updatePackageInfoFile(Project project, PackageInfo packageInfo) throws FileNotFoundException {
		String path = packageInfo.getPackageName().replace('.', '/') + "/packageinfo";
		File file = getSourceFile(project, path);
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter pw = new PrintWriter(fos);
		pw.println("version " + packageInfo.getSuggestedVersion());
		pw.flush();
		pw.close();
		
	}
	
	private static File getSourceFile(Project project, String path) {
		String src = project.getProperty("src", "src");
		return project.getFile(src + "/" + path);
	}

	private static boolean preUpdateProjectVersions(ReleaseContext context, List<IReleaseParticipant> participants) {
		context.setCurrentScope(Scope.PRE_UPDATE_VERSIONS);
		for (IReleaseParticipant participant : participants) {
			if (!participant.preUpdateProjectVersions(context)) {
				return false;
			}
		}
		return true;
	}

	private static boolean preRelease(ReleaseContext context, List<IReleaseParticipant> participants) {
		context.setCurrentScope(Scope.PRE_RELEASE);
		for (IReleaseParticipant participant : participants) {
			if (!participant.preRelease(context)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean preJarRelease(ReleaseContext context, List<IReleaseParticipant> participants, Jar jar) {
		context.setCurrentScope(Scope.PRE_JAR_RELEASE);
		for (IReleaseParticipant participant : participants) {
			if (!participant.preJarRelease(context, jar)) {
				return false;
			}
		}
		return true;
	}

	private static void postJarRelease(ReleaseContext context, List<IReleaseParticipant> participants, Jar jar) {
		context.setCurrentScope(Scope.POST_JAR_RELEASE);
		for (IReleaseParticipant participant : participants) {
			participant.postJarRelease(context, jar);
		}
	}

	private static void postRelease(ReleaseContext context, List<IReleaseParticipant> participants, boolean success) {
		context.setCurrentScope(Scope.POST_RELEASE);
		for (IReleaseParticipant participant : participants) {
			participant.postRelease(context, success);
		}
	}
}
