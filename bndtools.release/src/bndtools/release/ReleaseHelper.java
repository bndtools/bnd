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

import aQute.bnd.build.Project;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import aQute.libg.sed.Replacer;
import aQute.libg.sed.Sed;
import bndtools.diff.JarDiff;
import bndtools.diff.PackageInfo;
import bndtools.release.api.IReleaseParticipant;
import bndtools.release.api.ReleaseContext;
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
	
	public static boolean release(ReleaseContext context, JarDiff jarDiff) throws Exception {
		boolean ret = true;

		List<IReleaseParticipant> participants = Activator.getReleaseParticipants();
		
		IProject proj = ReleaseUtils.getProject(context.getProject());
		proj.refreshLocal(IResource.DEPTH_INFINITE, context.getProgressMonitor());
		
		if (!preRelease(context, participants)) {
			Activator.getDefault().error(context.getProject().getErrors());
			return false;
		}
		
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
		postRelease(context, participants, ret);
		return ret;
	}
	
	private static boolean release(ReleaseContext context, List<IReleaseParticipant> participants, Builder builder) throws Exception {

		context.getProject().refresh();
		context.getProject().setChanged();
		
		Jar jar = builder.build();

		String symbName = ReleaseUtils.getBundleSymbolicName(jar);
		String version = ReleaseUtils.getBundleVersion(jar);

		
		boolean proceed = preJarRelease(context, participants, jar);
		if (!proceed) {
			Activator.getDefault().error(builder.getErrors());
			return false;
		}
		
		context.getProject().release(context.getRepository().getName(), jar);
		context.getProject().refresh();
		
		File[] files = context.getRepository().get(symbName, '[' + version + ',' + version + ']');
		if (files.length == 1) {
			ReleaseUtils.toResource(files[0]).refreshLocal(IResource.DEPTH_ZERO, null);
			jar = new Jar(files[0]);
		}
		
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

	private static boolean preRelease(ReleaseContext context, List<IReleaseParticipant> participants) {
		for (IReleaseParticipant participant : participants) {
			if (!participant.preRelease(context)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean preJarRelease(ReleaseContext context, List<IReleaseParticipant> participants, Jar jar) {
		for (IReleaseParticipant participant : participants) {
			if (!participant.preJarRelease(context, jar)) {
				return false;
			}
		}
		return true;
	}

	private static void postJarRelease(ReleaseContext context, List<IReleaseParticipant> participants, Jar jar) {
		for (IReleaseParticipant participant : participants) {
			participant.postJarRelease(context, jar);
		}
	}

	private static void postRelease(ReleaseContext context, List<IReleaseParticipant> participants, boolean success) {
		for (IReleaseParticipant participant : participants) {
			participant.postRelease(context, success);
		}
	}
}
