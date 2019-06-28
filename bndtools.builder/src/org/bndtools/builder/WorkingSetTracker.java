package org.bndtools.builder;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.collections.ExtList;
import aQute.service.reporter.Reporter.SetLocation;

/**
 * Utility to track working sets from the bnd.bnd file You can set the
 * -workingset to a list of working set names. This will automatically add this
 * project to that working set. If there is no such working set it will create
 * one. For each parameter in the -workingset proeprty you can set an attribute
 * {@code member}. If this attribute is truthy (as defined in Processor.isTrue)
 * or not set then the project is made a member of the working set, otherwise it
 * is removed.
 *
 * <pre>
 * -workingset: foo
 * -workingset: impl;member=${;${p};.*.impl.*}
 * </pre>
 */
public class WorkingSetTracker {
	private static final Pattern JAVAID_P = Pattern.compile("\\p{javaJavaIdentifierPart}+");

	static void doWorkingSets(final Project model, final IProject targetProject) {

		IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkingSetManager workingSetManager = workbench.getWorkingSetManager();

		String mergeProperties = model.mergeProperties(Constants.WORKINGSET);
		if (mergeProperties == null)
			return;

		Parameters memberShips = new Parameters(mergeProperties);
		IWorkingSet[] allWorkingSets = workingSetManager.getAllWorkingSets();

		for (IWorkingSet currentWorkingSet : allWorkingSets) {

			if (!JAVAID_P.matcher(currentWorkingSet.getName())
				.matches())
				continue;

			Attrs attrs = memberShips.remove(currentWorkingSet.getName());
			boolean shouldBeMember = attrs != null && Processor.isTrue(attrs.get("member", "true"));

			IAdaptable[] elements = currentWorkingSet.getElements();
			List<IAdaptable> members = new ExtList<>(elements);

			boolean foundProjectInCurrentWorkingSet = false;

			for (Iterator<IAdaptable> it = members.iterator(); it.hasNext();) {
				IAdaptable member = it.next();
				if (member.getAdapter(IProject.class) == targetProject) {
					foundProjectInCurrentWorkingSet = true;
					if (!shouldBeMember) {
						it.remove();
					}
				}
			}

			if (!foundProjectInCurrentWorkingSet && shouldBeMember) {
				members.add(targetProject);
			}

			if (elements.length != members.size()) {
				updateWorkingSet(currentWorkingSet, members);
			}
		}

		for (final Entry<String, Attrs> e : memberShips.entrySet()) {
			String name = e.getKey();
			boolean isMember = Processor.isTrue(e.getValue()
				.get("member", "true"));
			if (!isMember)
				continue;

			if (!JAVAID_P.matcher(name)
				.matches()) {
				SetLocation error = model.warning("Invalid working set name '%s'. Must use pattern of Java identifier",
					name);
				error.file(model.getPropertiesFile()
					.getAbsolutePath());
				error.header("-workingset");
				continue;
			}
			IAdaptable[] members = new IAdaptable[1];
			members[0] = targetProject;
			IWorkingSet newWorkingSet = workingSetManager.createWorkingSet(name, members);
			newWorkingSet.setId("org.eclipse.jdt.ui.JavaWorkingSetPage");
			newWorkingSet.setLabel(null);
			workingSetManager.addWorkingSet(newWorkingSet);
		}
	}

	static private void updateWorkingSet(final IWorkingSet wset, final List<IAdaptable> members) {
		IAdaptable[] elements;
		elements = members.toArray(new IAdaptable[0]);
		wset.setElements(elements);
	}

}
