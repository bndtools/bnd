package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.resource.SupportingResource;

public final class BndResolutionService {
	private BndResolutionService() {}

	public static Map<String, Object> analyze(BndWorkspaceManager workspaceManager, List<Object> args) throws Exception {
		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException("Missing resolution analysis request.");
		}
		Object argument = args.get(0);
		if (argument instanceof JsonElement json && json.isJsonObject()) {
			argument = new Gson().fromJson(json, Map.class);
		}
		if (!(argument instanceof Map<?, ?> request)) {
			throw new IllegalArgumentException("Missing resolution analysis request.");
		}
		Object rawUris = request.get("uris");
		if (!(rawUris instanceof Collection<?> uris) || uris.isEmpty()) {
			throw new IllegalArgumentException("Select one or more .bnd or JAR files.");
		}
		if (uris.size() > 100) {
			throw new IllegalArgumentException("Resolution analysis is limited to 100 resources.");
		}

		List<Map<String, Object>> requirements = new ArrayList<>();
		List<Map<String, Object>> capabilities = new ArrayList<>();
		List<String> resources = new ArrayList<>();
		for (Object value : uris) {
			if (!(value instanceof String uri)) {
				throw new IllegalArgumentException("Resolution resource URIs must be strings.");
			}
			File file = new File(URI.create(uri));
			if (!file.isFile()) {
				throw new IllegalArgumentException("Resolution resource does not exist: " + file);
			}
			String name = file.getName();
			resources.add(file.getAbsolutePath());
			if (name.endsWith(".jar")) {
				try (Jar jar = new Jar(file)) {
					appendResources(name, jar, requirements, capabilities);
				}
			} else if (name.endsWith(".bnd")) {
				appendBndFile(workspaceManager, file, requirements, capabilities);
			} else {
				throw new IllegalArgumentException("Resolution analysis supports .bnd and .jar files: " + file);
			}
		}

		markResolved(requirements, capabilities);
		return Map.of("resources", resources, "requirements", requirements, "capabilities", capabilities);
	}

	private static void appendBndFile(BndWorkspaceManager workspaceManager, File file,
		List<Map<String, Object>> requirements, List<Map<String, Object>> capabilities) throws Exception {
		Project project = workspaceManager.getProjectForFile(file);
		if (project == null) {
			throw new IllegalArgumentException("Not in a bnd project: " + file);
		}
		if (file.getName().equals(Project.BNDFILE)) {
			ProjectBuilder projectBuilder = project.getBuilder(null);
			try {
				List<Builder> builders = projectBuilder.getSubBuilders();
				if (!builders.isEmpty()) {
					Builder builder = builders.get(0);
					builder.build();
					Jar jar = builder.getJar();
					if (jar != null) {
						appendResources(file.getName(), jar, requirements, capabilities);
					}
				}
			} finally {
				projectBuilder.close();
			}
		} else {
			Builder builder = project.getSubBuilder(file);
			if (builder == null) {
				throw new IllegalArgumentException("Could not create bnd builder for: " + file);
			}
			try {
				builder.build();
				Jar jar = builder.getJar();
				if (jar != null) {
					appendResources(file.getName(), jar, requirements, capabilities);
				}
			} finally {
				builder.close();
			}
		}
	}

	private static void appendResources(String source, Jar jar, List<Map<String, Object>> requirements,
		List<Map<String, Object>> capabilities) throws Exception {
		ResourceBuilder builder = new ResourceBuilder();
		builder.addJar(jar);
		SupportingResource supporting = builder.build();
		for (Resource resource : supporting.all()) {
			for (Requirement requirement : resource.getRequirements(null)) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("source", source);
				row.put("namespace", requirement.getNamespace());
				row.put("attributes", jsonMap(requirement.getAttributes()));
				row.put("directives", jsonMap(requirement.getDirectives()));
				row.put("optional", "optional".equals(requirement.getDirectives().get("resolution")));
				row.put("resolved", false);
				requirements.add(row);
			}
			for (Capability capability : resource.getCapabilities(null)) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("source", source);
				row.put("namespace", capability.getNamespace());
				row.put("attributes", jsonMap(capability.getAttributes()));
				row.put("directives", jsonMap(capability.getDirectives()));
				capabilities.add(row);
			}
		}
	}

	private static void markResolved(List<Map<String, Object>> requirements, List<Map<String, Object>> capabilities) {
		for (Map<String, Object> requirement : requirements) {
			@SuppressWarnings("unchecked")
			Map<String, Object> directives = (Map<String, Object>) requirement.get("directives");
			Object filter = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			if (!(filter instanceof String)) {
				continue;
			}
			try {
				Requirement osgiRequirement = new SimpleRequirement(requirement);
				Predicate<Capability> matcher = ResourceUtils.filterMatcher(osgiRequirement);
				for (Map<String, Object> capability : capabilities) {
					if (!requirement.get("namespace").equals(capability.get("namespace"))) {
						continue;
					}
					if (matcher.test(new SimpleCapability(capability))) {
						requirement.put("resolved", true);
						break;
					}
				}
			} catch (Exception e) {
				// Keep malformed or unsupported filters visible as unresolved.
			}
		}
	}

	private static Map<String, Object> jsonMap(Map<String, ?> values) {
		Map<String, Object> result = new LinkedHashMap<>();
		values.forEach((key, value) -> result.put(key, jsonValue(value)));
		return result;
	}

	private static Object jsonValue(Object value) {
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof Collection<?> values) {
			return values.stream().map(BndResolutionService::jsonValue).toList();
		}
		return value.toString();
	}

	private static final class SimpleRequirement implements Requirement {
		private final Map<String, Object> row;

		SimpleRequirement(Map<String, Object> row) { this.row = row; }
		@Override public String getNamespace() { return (String) row.get("namespace"); }
		@SuppressWarnings("unchecked")
		@Override public Map<String, String> getDirectives() { return (Map<String, String>) row.get("directives"); }
		@SuppressWarnings("unchecked")
		@Override public Map<String, Object> getAttributes() { return (Map<String, Object>) row.get("attributes"); }
		@Override public Resource getResource() { return null; }
	}

	private static final class SimpleCapability implements Capability {
		private final Map<String, Object> row;

		SimpleCapability(Map<String, Object> row) { this.row = row; }
		@Override public String getNamespace() { return (String) row.get("namespace"); }
		@SuppressWarnings("unchecked")
		@Override public Map<String, String> getDirectives() { return (Map<String, String>) row.get("directives"); }
		@SuppressWarnings("unchecked")
		@Override public Map<String, Object> getAttributes() { return (Map<String, Object>) row.get("attributes"); }
		@Override public Resource getResource() { return null; }
	}
}
