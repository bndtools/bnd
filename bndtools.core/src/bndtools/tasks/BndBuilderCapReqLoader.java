package bndtools.tasks;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Manifest;

import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.SyntheticBuilder;
import aQute.lib.exceptions.Exceptions;
import bndtools.model.resolution.RequirementWrapper;

public abstract class BndBuilderCapReqLoader implements CapReqLoader {

	protected final File							file;
	private Map<String, List<Capability>>			loadCapabilities;
	private Map<String, List<RequirementWrapper>>	loadRequirements;

	public BndBuilderCapReqLoader(File file) {
		this.file = file;
	}

	@Override
	public String getShortLabel() {
		return file.getName();
	}

	@Override
	public String getLongLabel() {
		return file.getName() + " - " + file.getParentFile()
			.getAbsolutePath();
	}

	protected abstract Builder getBuilder() throws Exception;

	private void load() throws Exception {
		if ((loadCapabilities != null) && (loadRequirements != null)) {
			return;
		}

		Builder builder = getBuilder();
		if (builder == null) {
			loadCapabilities = Collections.emptyMap();
			loadRequirements = Collections.emptyMap();
			return;
		}

		Jar jar = builder.getJar();
		if (jar == null) {
			loadCapabilities = Collections.emptyMap();
			loadRequirements = Collections.emptyMap();
			return;
		}

		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			loadCapabilities = Collections.emptyMap();
			loadRequirements = Collections.emptyMap();
			return;
		}

		SyntheticBuilder rb = new SyntheticBuilder();
		rb.addManifest(Domain.domain(manifest));
		Resource resource = rb.build();

		List<Capability> capabilities = resource.getCapabilities(null);
		loadCapabilities = capabilities.stream()
			.collect(groupingBy(Capability::getNamespace, toList()));

		List<Requirement> requirements = resource.getRequirements(null);
		loadRequirements = requirements.stream()
			.collect(groupingBy(Requirement::getNamespace, mapping(this::toRequirementWrapper, toList())));
	}

	@Override
	public Map<String, List<Capability>> loadCapabilities() throws Exception {
		load();
		return loadCapabilities;
	}

	@Override
	public Map<String, List<RequirementWrapper>> loadRequirements() throws Exception {
		load();
		return loadRequirements;
	}

	private RequirementWrapper toRequirementWrapper(Requirement req) {
		RequirementWrapper rw = new RequirementWrapper();
		rw.requirement = req;
		if (req.getNamespace()
			.equals(PackageNamespace.PACKAGE_NAMESPACE)) {
			String pkgName = (String) req.getAttributes()
				.get(PackageNamespace.PACKAGE_NAMESPACE);
			try {
				rw.requirers = findImportingClasses(pkgName);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}
		return rw;
	}

	private List<Clazz> findImportingClasses(String pkgName) throws Exception {
		List<Clazz> classes = new LinkedList<>();
		Collection<Clazz> importers = getBuilder().getClasses("", "IMPORTING", pkgName);

		// Remove *this* package
		for (Clazz clazz : importers) {
			String fqn = clazz.getFQN();
			int dot = fqn.lastIndexOf('.');
			if (dot >= 0) {
				String pkg = fqn.substring(0, dot);
				if (!pkgName.equals(pkg))
					classes.add(clazz);
			}
		}
		return classes;
	}

	public File getFile() {
		return file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BndBuilderCapReqLoader other = (BndBuilderCapReqLoader) obj;
		return Objects.equals(file, other.file);
	}

}
