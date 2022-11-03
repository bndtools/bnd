package bndtools.explorer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Display;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import bndtools.central.Central;

class Model {
	Glob					glob;
	IProject				selectedProject;
	Object					selection;
	String					prompt;
	String					message	= "initializing workspace";
	int						severity;
	String					filterText;
	final AtomicBoolean		filterDirty	= new AtomicBoolean(false);
	final List<Runnable>	updates	= new ArrayList<>();
	final AtomicBoolean		dirty	= new AtomicBoolean(false);
	final Set<IProject>		pinned	= new HashSet<>();

	void setSelectedProject(IProject project) {
		if (project != selectedProject) {
			this.selectedProject = project;
			update();
		}
	}

	void closeProject(IProject selectedProject) {
		if (this.selectedProject == selectedProject)
			setSelectedProject(null);
	}

	void setFilterText(String value) {
		if (Objects.equals(this.filterText, value))
			return;
		this.filterText = value;
		if (value == null)
			glob = null;
		else
			glob = new Glob(value);
		filterDirty.set(true);
		update();
	}

	void setPrompt(String prompt) {
		if (Objects.equals(prompt, this.prompt))
			return;

		this.prompt = prompt;
		updateMessage();
	}

	void setMessage(String message) {
		if (Objects.equals(this.message, message))
			return;
		this.message = message;
		update();
	}

	boolean isPinned(IProject project) {
		return pinned.contains(project);
	}

	void updateMessage() {
		Central.onAnyWorkspace(ws -> setMessage(getPrompt(ws)));
	}

	void setSeverity(int severity) {
		if (this.severity != severity) {
			this.severity = severity;
			filterDirty.set(true);
			update();
		}
	}

	private String getPrompt(Workspace ws) {
		try {
			if (prompt == null || prompt.isEmpty())
				prompt = "<b>${basename;${workspace}}</b> ${def;Bundle-Version} <a href='prefs'>[?]</a>";
			else if ("-".equals(prompt))
				return "";

			String s = ws.getReplacer()
				.process(prompt);
			s = Strings.removeQuotes(s);
			return s;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void onUpdate(Runnable runnable) {
		updates.add(runnable);
	}

	void update() {
		if (dirty.getAndSet(true))
			return;

		Display.getDefault()
			.asyncExec(this::update0);
	}

	/*
	 * This runs async on the display thread.
	 */
	private void update0() {
		if (dirty.getAndSet(false)) {
			updates.forEach(Runnable::run);
		}
	}

	void doPin() {
		if (!(selection instanceof IProject))
			return;

		IProject p = (IProject) selection;
		if (pinned.contains(p)) {
			pinned.remove(p);
		} else {
			pinned.add(p);
		}
		filterDirty.set(true);
		update();
	}

	void setActualSelection(Object selection) {
		this.selection = selection;
		update();
	}

}
