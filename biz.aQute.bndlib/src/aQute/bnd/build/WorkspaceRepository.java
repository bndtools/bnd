package aQute.bnd.build;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Matcher;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.SortedList;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;

public class WorkspaceRepository implements RepositoryPlugin, Actionable {
	private final Workspace workspace;

	public WorkspaceRepository(Workspace workspace) {
		this.workspace = workspace;
	}

	private File[] get(String bsn, String range) throws Exception {
		Collection<Project> projects = workspace.getAllProjects();
		SortedMap<Version, File> foundVersion = new TreeMap<>();
		for (Project project : projects) {
			Map<String, Version> versions = project.getVersions();
			if (!versions.containsKey(bsn)) {
				continue;
			}
			Version version = versions.get(bsn);
			boolean exact = range.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\..*");
			if (Constants.VERSION_ATTR_LATEST.equals(range) || matchVersion(range, version, exact)) {
				File file = project.getOutputFile(bsn, version.toString());
				if (!file.exists()) {
					try (Builder builder = project.getSubBuilder(bsn); Jar jar = builder.build()) {
						if (jar == null) {
							project.getInfo(builder);
							continue;
						}
						file = project.saveBuild(jar);
					}
				}
				foundVersion.put(version, file);
				break;
			}
		}

		File[] result = foundVersion.values()
			.toArray(new File[0]);
		if (!Constants.VERSION_ATTR_LATEST.equals(range)) {
			return result;
		}
		if (result.length > 0) {
			return new File[] {
				result[0]
			};
		}
		return new File[0];
	}

	private File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
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
		if (range == null || range.trim()
			.length() == 0)
			return true;
		VersionRange vr = new VersionRange(range);

		boolean result;
		if (exact) {
			if (vr.isRange())
				result = false;
			else
				result = vr.getHigh()
					.equals(version);
		} else {
			result = vr.includes(version);
		}
		return result;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Read only repository");
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		List<String> names = new ArrayList<>();
		Collection<Project> projects = workspace.getAllProjects();
		for (Project project : projects) {
			for (String bsn : project.getBsns()) {
				if (pattern != null) {
					Glob glob = new Glob(pattern);
					Matcher matcher = glob.matcher(bsn);
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

		return names;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		List<Version> versions = new ArrayList<>();
		Collection<Project> projects = workspace.getAllProjects();
		for (Project project : projects) {
			Map<String, Version> projectVersions = project.getVersions();
			if (!projectVersions.containsKey(bsn)) {
				continue;
			}
			versions.add(projectVersions.get(bsn));
			break;
		}
		if (versions.isEmpty())
			return SortedList.empty();

		return new SortedList<>(versions);
	}

	@Override
	public String getName() {
		return "Workspace " + workspace.getBase()
			.getName();
	}

	@Override
	public String getLocation() {
		return IO.absolutePath(workspace.getBase());
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		File file = get(bsn, version.toString(), Strategy.EXACT, properties);
		if (file == null)
			return null;
		for (DownloadListener l : listeners) {
			try {
				l.success(file);
			} catch (Exception e) {
				workspace.exception(e, "Workspace repo listener callback for %s", file);
			}
		}
		return file;
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
