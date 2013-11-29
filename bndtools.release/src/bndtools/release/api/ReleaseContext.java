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
package bndtools.release.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.release.api.IReleaseParticipant.Scope;

public class ReleaseContext {

	private Project project;
	private List<Baseline> diffs;
	private RepositoryPlugin releaseRepo;
	private IProgressMonitor progressMonitor;

	private List<Jar> releasedJars;
	private Map<String, Object> properties;
	private ErrorHandler errorHandler;
	private Scope currentScope;
	private ReleaseOption releaseOption;

	public ReleaseContext(Project project, List<Baseline> diffs, RepositoryPlugin releaseRepo, ReleaseOption releaseOption) {
		this.project = project;
		this.diffs = diffs;
		this.releaseRepo = releaseRepo;
		this.releaseOption = releaseOption;

		this.releasedJars = new ArrayList<Jar>();
		this.properties = new HashMap<String, Object>();
		this.errorHandler = new ErrorHandler();
	}

	@Deprecated
    public ReleaseContext(Project project, List<Baseline> diffs, RepositoryPlugin releaseRepo, boolean updateOnly) {
        this(project, diffs, releaseRepo, updateOnly ? ReleaseOption.UPDATE : ReleaseOption.UPDATE_RELEASE);
    }

	public Project getProject() {
		return project;
	}

	public List<Baseline> getBaselines() {
		return diffs;
	}

	public RepositoryPlugin getReleaseRepository() {
		return releaseRepo;
	}

	public IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public void setProgressMonitor(IProgressMonitor progressMonitor) {
		this.progressMonitor = progressMonitor;
	}
	public void addReleasedJar(Jar jar) {
		releasedJars.add(jar);
	}

	public List<Jar> getReleasedJars() {
		return releasedJars;
	}

	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}

	public Object getProperty(String name) {
		return properties.get(name);
	}

	public Map<String, Object> getPropertyMap() {
		return properties;
	}

	public Scope getCurrentScope() {
		return currentScope;
	}

	public void setCurrentScope(Scope currentScope) {
		this.currentScope = currentScope;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public class ErrorHandler {

		private List<Error> errors = new LinkedList<Error>();

		protected ErrorHandler() {
			super();
		}

		public void error(String message) {
			Error error = new Error();
			error.scope = getCurrentScope();
			error.message = message;
			errors.add(error);
		}

		public Error error(String symbName, String version, String message) {
			Error error = new Error();
			error.scope = getCurrentScope();
			error.message = message;
			error.symbName = symbName;
			error.version = version;
			errors.add(error);
			return error;
		}

		public Error error(String symbName, String version, String message, String[][] list, String ... headers) {
			Error error = error(symbName, version, message);
			error.list = list;
			error.headers = headers;
			return error;
		}

		public List<Error> getErrors() {
			return errors;
		}
	}

	/**
	 * @deprecated Use getReleaseOption instead
	 * @return true if the release option is UPDATE
	 */
	@Deprecated
	public boolean isUpdateOnly() {
		return releaseOption == ReleaseOption.UPDATE;
	}

    /**
     * @deprecated Use setReleaseOption instead
     * @param updateOnly true to indicate 'update only mode'
     */
    @Deprecated
	public void setUpdateOnly(boolean updateOnly) {
		this.releaseOption = updateOnly ? ReleaseOption.UPDATE : ReleaseOption.UPDATE_RELEASE;
	}

	public void setReleaseOption(ReleaseOption releaseOption) {
	    this.releaseOption = releaseOption;
	}

	public ReleaseOption getReleaseOption() {
	    return releaseOption;
	}

	public static class Error {

		protected Scope scope;
		protected String message;
		protected String symbName;
		protected String version;
		protected String[][] list;
		protected String[] headers;

		public Scope getScope() {
			return scope;
		}
		public String getMessage() {
			return message;
		}
		public String getSymbName() {
			return symbName;
		}
		public String getVersion() {
			return version;
		}
		public String[][] getList() {
			return list;
		}
		public String[] getHeaders() {
			return headers;
		}
	}
}
