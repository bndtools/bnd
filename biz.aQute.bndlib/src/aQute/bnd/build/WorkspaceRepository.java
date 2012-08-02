package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;

public class WorkspaceRepository implements RepositoryPlugin {
	private final Workspace	workspace;

	public WorkspaceRepository(Workspace workspace) {
		this.workspace = workspace;
	}

	private File[] get(String bsn, String range) throws Exception {
		Collection<Project> projects = workspace.getAllProjects();
		SortedMap<Version,File> foundVersion = new TreeMap<Version,File>();
		for (Project project : projects) {
			File[] build = project.build(false);
			if (build != null) {
				for (File file : build) {
					Jar jar = new Jar(file);
					if (bsn.equals(jar.getBsn())) {
						Version version = new Version(jar.getVersion());
						boolean exact = range.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\..*");
						if ("latest".equals(range) || matchVersion(range, version, exact)) {
							foundVersion.put(version, file);
						}
					}
				}
			}
		}

		File[] result = new File[foundVersion.size()];
		result = foundVersion.values().toArray(result);
		if (!"latest".equals(range)) {
			return result;
		}
		if (result.length > 0) {
			return new File[] {
				result[0]
			};
		}
		return new File[0];
	}

	public File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception {
		File[] files = get(bsn, range);

		if (files.length == 0) {
			return null;
		}

		if (strategy == Strategy.EXACT) {
			return files[0];
		} else if (strategy == Strategy.HIGHEST) {
			return files[files.length - 1];
		} else if (strategy == Strategy.LOWEST) {
			return files[0];
		}

		return null;
	}

	private boolean matchVersion(String range, Version version, boolean exact) {
		if (range == null || range.trim().length() == 0)
			return true;
		VersionRange vr = new VersionRange(range);

		boolean result;
		if (exact) {
			if (vr.isRange())
				result = false;
			else
				result = vr.getHigh().equals(version);
		} else {
			result = vr.includes(version);
		}
		return result;
	}

	public boolean canWrite() {
		return false;
	}

	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		return null;
	}

	public List<String> list(String regex) throws Exception {
		List<String> names = new ArrayList<String>();
		Collection<Project> projects = workspace.getAllProjects();
		for (Project project : projects) {
			File[] build = project.build(false);
			if (build != null) {
				for (File file : build) {
					Jar jar = new Jar(file);
					String bsn = jar.getBsn();
					if (regex != null) {
						Pattern pattern = Pattern.compile(regex);
						Matcher matcher = pattern.matcher(bsn);
						if (matcher.matches()) {
							if (!names.contains(bsn)) {
								names.add(bsn);
							}
						}
					} else {
						if (!names.contains(bsn)) {
							names.add(bsn);
						}
					}
				}
			}
		}

		return names;
	}

	public List<Version> versions(String bsn) throws Exception {
		List<Version> versions = new ArrayList<Version>();
		Collection<Project> projects = workspace.getAllProjects();
		for (Project project : projects) {
			File[] build = project.build(false);
			if (build != null) {
				for (File file : build) {
					Jar jar = new Jar(file);
					if (bsn.equals(jar.getBsn())) {
						versions.add(new Version(jar.getVersion()));
					}
				}
			}
		}

		return versions;
	}

	public String getName() {
		return "Workspace";
	}

	public String getLocation() {
		return "Workspace";
	}

}
