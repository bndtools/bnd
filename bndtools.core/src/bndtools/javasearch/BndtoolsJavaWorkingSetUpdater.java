/*******************************************************************************
 * Copyright (c) 2018, 2020 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *     Gregory Amerson - copied original and adapted for Bndtools usage
 *******************************************************************************/
package bndtools.javasearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.ui.progress.WorkbenchJob;

import bndtools.central.Central;

public class BndtoolsJavaWorkingSetUpdater implements IWorkingSetUpdater {

	public static final String ID = "bndtools.core.BndtoolsJavaWorkingSet";
	public static final String	WORKING_SET_NAME	= "Bndtools Java Model";

	private static final String	TASK_NAME			= WORKING_SET_NAME + "Updating Bndtools Java Working Set";

	private class JavaElementChangeListener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent event) {
			processJavaDelta(event.getDelta());
		}

		private boolean processJavaDelta(IJavaElementDelta delta) {
			IJavaElement javaElement = delta.getElement();
			int type = javaElement.getElementType();
			if (type == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				int kind = delta.getKind();
				if (kind == IJavaElementDelta.ADDED || kind == IJavaElementDelta.REMOVED) {
					// this can happen without "classpath changed" event, if the
					// directory corresponding to an optional source folder is
					// created.
					triggerUpdate();
					return true;
				}
				// do not traverse into children
			} else if (type == IJavaElement.JAVA_PROJECT) {
				int kind = delta.getKind();
				int flags = delta.getFlags();
				if (kind == IJavaElementDelta.ADDED || kind == IJavaElementDelta.REMOVED
					|| (flags & (IJavaElementDelta.F_OPENED | IJavaElementDelta.F_CLOSED
						| IJavaElementDelta.F_CLASSPATH_CHANGED)) != 0) {
					triggerUpdate();
					return true;
				}
				for (IJavaElementDelta element : delta.getAffectedChildren()) {
					if (processJavaDelta(element))
						return true;
				}
			} else if (type == IJavaElement.JAVA_MODEL) {
				for (IJavaElementDelta element : delta.getAffectedChildren()) {
					if (processJavaDelta(element))
						return true;
				}
			}
			return false;
		}
	}

	private class UpdateUIJob extends WorkbenchJob {

		volatile Runnable task;

		public UpdateUIJob() {
			super(TASK_NAME);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Runnable r = task;
			if(r != null && !monitor.isCanceled() && !isDisposed.get()) {
				r.run();
			}
			return Status.OK_STATUS;
		}

		void setTask(Runnable r){
			cancel();
			task = r;
			if(r != null){
				schedule();
			}
		}

		Runnable getTask() {
			return task;
		}
	}

	private IElementChangedListener	javaElementChangeListener	= new JavaElementChangeListener();
	private IWorkingSet				bndtoolsWorkingSet			= null;
	private IAdaptable[]			initialContents				= new IAdaptable[0];
	private Job						updateJob;
	private UpdateUIJob				updateInUIJob;
	private AtomicBoolean			isDisposed					= new AtomicBoolean();

	@Override
	public void add(IWorkingSet workingSet) {
		synchronized (this) {
			if (workingSet.getName()
				.equals(WORKING_SET_NAME)) {
				bndtoolsWorkingSet = workingSet;
				workingSet.setElements(restore(workingSet));
			}
		}
	}

	@Override
	public boolean remove(IWorkingSet workingSet) {
		synchronized (this) {
			if (WORKING_SET_NAME.equals(workingSet.getName())) {
				bndtoolsWorkingSet = null;
				updateJob.cancel();
				updateInUIJob.setTask(null);
				if (javaElementChangeListener != null)
					JavaCore.removeElementChangedListener(javaElementChangeListener);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean contains(IWorkingSet workingSet) {
		synchronized (this) {
			return Objects.equals(workingSet, bndtoolsWorkingSet);
		}
	}

	public BndtoolsJavaWorkingSetUpdater() {
		updateJob = new Job(TASK_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return updateElements(bndtoolsWorkingSet, monitor);
			}
		};
		updateInUIJob = new UpdateUIJob();
		updateJob.setSystem(true);
	}

	private IAdaptable[] restore(IWorkingSet workingSet) {
		String name = workingSet.getName();
		if (!WORKING_SET_NAME.equals(name)) {
			return new IAdaptable[0];
		}
		IAdaptable[] elements = null;
		if (initialContents.length == 0) {
			try {
				elements = collectData(new NullProgressMonitor());
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
			JavaCore.addElementChangedListener(javaElementChangeListener, ElementChangedEvent.POST_CHANGE);
		}
		return elements;
	}

	@Override
	public void dispose() {
		isDisposed.set(true);
		if (javaElementChangeListener != null)
			JavaCore.removeElementChangedListener(javaElementChangeListener);
		updateJob.cancel();
		updateInUIJob.setTask(null);
	}

	public void triggerUpdate() {
		synchronized (this) {
			if (isDisposed.get())
				return;
			updateJob.cancel();
			updateJob.schedule(1000L);
		}
	}

	private IStatus updateElements(IWorkingSet workingSet, IProgressMonitor monitor) {
		try {
			if (isDisposed.get() || monitor.isCanceled())
				return Status.CANCEL_STATUS;
			IAdaptable[] data = collectData(monitor);
			Runnable update = () -> updateWorkingSet(workingSet, data);
			if(Display.getCurrent() != null) {
				update.run();
			} else {
				updateInUIJob.setTask(new Runnable() {
					@Override
					public void run() {
						// check if the next task is already in queue
						if (this != updateInUIJob.getTask()) {
							update.run();
						}
					}
				});
			}
		} catch (Exception e) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	private static void updateWorkingSet(IWorkingSet workingSet, IAdaptable[] data) {
		if (WORKING_SET_NAME.equals(workingSet.getName()))
			workingSet.setElements(data);
	}

	private IAdaptable[] collectData(IProgressMonitor monitor) throws CoreException {
		IAdaptable[] data = new IAdaptable[0];
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IJavaModel model= JavaCore.create(root);
		List<IAdaptable> result = new ArrayList<>();
		for (IJavaProject javaProject : model.getJavaProjects()) {
			if (monitor.isCanceled() || isDisposed.get())
				return new IAdaptable[0];
			IProject project = javaProject.getProject();
			if (project.isOpen()) {
				Arrays.stream(javaProject
					.getPackageFragmentRoots())
					.filter(packageFragmentRoot -> {
						boolean isBndGeneratedJar = Optional.ofNullable(packageFragmentRoot.getResource())
							.filter(res -> Central.isBndProject(res.getProject()))
							.filter(IResource::isDerived)
							.map(IResource::getFullPath)
							.map(IPath::lastSegment)
							.filter(path -> path.endsWith(".jar"))
							.isPresent();

						return !isBndGeneratedJar;

						})
					.forEach(result::add);
					}
		}
		data = result.toArray(new IAdaptable[0]);
		if (initialContents.length == 0)
			initialContents = data;
		return data;
	}

}
