package bndtools.model.repo;

import java.util.List;
import java.util.Objects;

import org.bndtools.utils.resources.ResourceUtils;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.resource.Requirement;
import org.osgi.resource.Namespace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.xml.XML;
import aQute.p2.provider.Feature;

/**
 * Represents an indexed Eclipse feature with its member hierarchy. The display
 * metadata is reconstructed from the index without downloading the feature JAR;
 * capability and requirement analysis uses the original resource.
 */
public class RepositoryFeatureResource extends RepositoryFeature {

	private final Resource resource;
	private final String id;
	private final String version;
	private final RepositoryPlugin repoPlugin;

	public RepositoryFeatureResource(RepositoryPlugin repoPlugin, Resource resource) {
		super(repoPlugin, feature(resource));
		this.repoPlugin = repoPlugin;
		this.resource = resource;
		this.id = ResourceUtils.getIdentity(resource);
		
		Version osgiVersion = ResourceUtils.getVersion(resource);
		if (osgiVersion == null)
			osgiVersion = Version.emptyVersion;
		this.version = osgiVersion.toString();
	}

	public Resource getResource() {
		return resource;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public static Requirement getMemberRequirement(RepositoryFeature feature, String id, String relation) {
		Resource resource = feature.getResource();
		if (resource == null)
			return null;
		for (Requirement requirement : resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE)) {
			if (relation.equals(requirement.getAttributes().get(Feature.RELATION_ATTRIBUTE))
				&& id.equals(identity(new FilterParser().parse(requirement))))
				return requirement;
		}
		return null;
	}

	public String getLabel() {
		// Try to get label from identity capability attributes
		for (Capability cap : resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)) {
			Object label = cap.getAttributes().get("label");
			if (label != null) {
				return label.toString();
			}
		}
		return id;
	}

	public String getProviderName() {
		// Try to get provider-name from identity capability attributes
		for (Capability cap : resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)) {
			Object providerName = cap.getAttributes().get("provider-name");
			if (providerName != null) {
				return providerName.toString();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == Resource.class) {
			return (T) resource;
		}
		if (adapter == RepositoryPlugin.class) {
			return (T) repoPlugin;
		}
		return super.getAdapter(adapter);
	}

	private static Feature feature(Resource resource) {
		try {
			Document document = XML.newDocumentBuilderFactory().newDocumentBuilder().newDocument();
			Element root = document.createElement("feature");
			document.appendChild(root);
			resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes()
				.forEach((name, value) -> {
					if (IdentityNamespace.IDENTITY_NAMESPACE.equals(name))
						root.setAttribute("id", value.toString());
					else if (!IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE.equals(name))
						root.setAttribute(name, value.toString());
				});
			Element requires = document.createElement("requires");
			root.appendChild(requires);
			for (Requirement requirement : resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE)) {
				Object relation = requirement.getAttributes().get(Feature.RELATION_ATTRIBUTE);
				String id = Objects.toString(requirement.getAttributes().get("id"), null);
				if (id == null)
					id = identity(new FilterParser().parse(requirement));
				if (id == null)
					continue;
				Element member;
				if (Feature.RELATION_PLUGIN.equals(relation)) {
					member = document.createElement("plugin");
					member.setAttribute("id", id);
					root.appendChild(member);
				} else if (Feature.RELATION_INCLUDE.equals(relation)) {
					member = document.createElement("includes");
					member.setAttribute("id", id);
					root.appendChild(member);
				} else {
					member = document.createElement("import");
					String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					member.setAttribute(filter != null && filter.contains("(type=" + Feature.TYPE + ")")
						? "feature" : "plugin", id);
					requires.appendChild(member);
				}
				for (String name : List.of("version", "os", "ws", "arch", "fragment", "match")) {
					Object value = requirement.getAttributes().get(name);
					if (value != null)
						member.setAttribute(name, value.toString());
				}
				if (!member.hasAttribute("version")) {
					String version = version(new FilterParser().parse(requirement));
					if (version != null)
						member.setAttribute("version", version);
				}
				if (Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives()
					.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)))
					member.setAttribute("optional", "true");
			}
			Feature feature = new Feature(document);
			feature.parse();
			return feature;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static String identity(FilterParser.Expression expression) {
		if (expression instanceof FilterParser.IdentityExpression identity)
			return identity.getSymbolicName();
		if (expression instanceof FilterParser.SubExpression group) {
			for (FilterParser.Expression child : group.getExpressions()) {
				String id = identity(child);
				if (id != null)
					return id;
			}
		}
		return null;
	}

	private static String version(FilterParser.Expression expression) {
		if (expression instanceof FilterParser.WithRangeExpression ranged && ranged.getRangeExpression() != null)
			return ranged.getRangeExpression().getRangeString();
		if (expression instanceof FilterParser.RangeExpression range)
			return range.getRangeString();
		if (expression instanceof FilterParser.SimpleExpression simple && "version".equals(simple.getKey()))
			return simple.getValue();
		if (expression instanceof FilterParser.SubExpression group) {
			for (FilterParser.Expression child : group.getExpressions()) {
				String version = version(child);
				if (version != null)
					return version;
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(resource, repoPlugin);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryFeatureResource other = (RepositoryFeatureResource) obj;
		return Objects.equals(resource, other.resource) && Objects.equals(repoPlugin, other.repoPlugin);
	}

	@Override
	public String toString() {
		return "RepositoryFeatureResource [repo=" + repoPlugin.getName() + ", id=" + id + ", version=" + version + "]";
	}
}
