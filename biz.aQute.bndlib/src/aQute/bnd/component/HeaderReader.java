package aQute.bnd.component;

import static aQute.bnd.component.DSAnnotationReader.V1_0;
import static aQute.bnd.component.DSAnnotationReader.V1_1;
import static aQute.bnd.component.DSAnnotationReader.V1_2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.component.annotations.ConfigurationPolicy;
import aQute.bnd.component.annotations.ReferenceCardinality;
import aQute.bnd.component.annotations.ReferencePolicy;
import aQute.bnd.component.annotations.ReferencePolicyOption;
import aQute.bnd.component.annotations.ServiceScope;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError.ErrorType;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.lib.tag.Tag;

public class HeaderReader extends Processor {
	private final static Pattern		PROPERTY_PATTERN	= Pattern
		.compile("(([^=:@]+)([:@](Boolean|Byte|Char|Short|Integer|Long|Float|Double|String))?)\\s*=(.*)");
	private final static Set<String>	LIFECYCLE_METHODS	= new HashSet<>(
		Arrays.asList("activate", "deactivate", "modified"));

	private final Analyzer				analyzer;

	private final static String			ComponentContextTR	= "org.osgi.service.component.ComponentContext";
	private final static String			BundleContextTR		= "org.osgi.framework.BundleContext";
	private final static String			MapTR				= Map.class.getName();
	private final static String			IntTR				= int.class.getName();
	final static Set<String>			allowed				= new HashSet<>(
		Arrays.asList(ComponentContextTR, BundleContextTR, MapTR));
	final static Set<String>			allowedDeactivate	= new HashSet<>(
		Arrays.asList(ComponentContextTR, BundleContextTR, MapTR, IntTR));

	private final static String			ServiceReferenceTR	= "org.osgi.framework.ServiceReference";

	public HeaderReader(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Tag createComponentTag(String name, String impl, Map<String, String> info) throws Exception {
		final ComponentDef cd = new ComponentDef(analyzer, null, V1_0);
		cd.name = name;
		if (info.get(COMPONENT_ENABLED) != null)
			cd.enabled = Boolean.valueOf(info.get(COMPONENT_ENABLED));
		cd.factory = info.get(COMPONENT_FACTORY);
		if (info.get(COMPONENT_IMMEDIATE) != null)
			cd.immediate = Boolean.valueOf(info.get(COMPONENT_IMMEDIATE));
		if (info.get(COMPONENT_CONFIGURATION_POLICY) != null)
			cd.configurationPolicy = ConfigurationPolicy.valueOf(info.get(COMPONENT_CONFIGURATION_POLICY)
				.toUpperCase());
		cd.activate = checkIdentifier(COMPONENT_ACTIVATE, info.get(COMPONENT_ACTIVATE));
		cd.deactivate = checkIdentifier(COMPONENT_DEACTIVATE, info.get(COMPONENT_DEACTIVATE));
		cd.modified = checkIdentifier(COMPONENT_MODIFIED, info.get(COMPONENT_MODIFIED));

		cd.implementation = analyzer.getTypeRefFromFQN(impl == null ? name : impl);

		String provides = info.get(COMPONENT_PROVIDE);
		if (info.get(COMPONENT_SERVICEFACTORY) != null) {
			if (provides != null)
				cd.scope = Boolean.valueOf(info.get(COMPONENT_SERVICEFACTORY)) ? ServiceScope.BUNDLE
					: ServiceScope.SINGLETON;
			else
				warning("The servicefactory:=true directive is set but no service is provided, ignoring it");
		}

		if (cd.scope == ServiceScope.BUNDLE && cd.immediate != null && cd.immediate) {
			// TODO can become error() if it is up to me
			warning(
				"For a Service Component, the immediate option and the servicefactory option are mutually exclusive for %s(%s)",
				name, impl);
		}

		// analyze the class for suitable methods.
		final Map<String, MethodDef> lifecycleMethods = new HashMap<>();
		final Map<String, MethodDef> bindmethods = new HashMap<>();
		TypeRef typeRef = analyzer.getTypeRefFromFQN(impl);
		Clazz clazz = analyzer.findClass(typeRef);
		boolean privateAllowed = true;
		boolean defaultAllowed = true;
		String topPackage = typeRef.getPackageRef()
			.getFQN();
		while (clazz != null) {
			final boolean pa = privateAllowed;
			final boolean da = defaultAllowed;
			final Map<String, MethodDef> classLifecyclemethods = new HashMap<>();
			final Map<String, MethodDef> classBindmethods = new HashMap<>();

			clazz.parseClassFileWithCollector(new ClassDataCollector() {

				@Override
				public void method(MethodDef md) {
					Set<String> allowedParams = allowed;
					String lifecycleName = null;

					boolean isLifecycle = (cd.activate == null ? "activate" : cd.activate).equals(md.getName())
						|| md.getName()
							.equals(cd.modified);
					if (!isLifecycle && (cd.deactivate == null ? "deactivate" : cd.deactivate).equals(md.getName())) {
						isLifecycle = true;
						allowedParams = allowedDeactivate;
					}
					if (isLifecycle && !lifecycleMethods.containsKey(md.getName())
						&& (md.isPublic() || md.isProtected() || (md.isPrivate() && pa) || (!md.isPrivate()) && da)
						&& isBetter(md, classLifecyclemethods.get(md.getName()), allowedParams)) {
						classLifecyclemethods.put(md.getName(), md);
					}
					if (!bindmethods.containsKey(md.getName())
						&& (md.isPublic() || md.isProtected() || (md.isPrivate() && pa) || (!md.isPrivate()) && da)
						&& isBetterBind(md, classBindmethods.get(md.getName()))) {
						classBindmethods.put(md.getName(), md);
					}
				}

				private boolean isBetter(MethodDef test, MethodDef existing, Set<String> allowedParams) {
					int testRating = rateLifecycle(test, allowedParams);
					if (existing == null)
						return testRating < 6;// ignore invalid methods
					if (testRating < rateLifecycle(existing, allowedParams))
						return true;

					return false;
				}

				private boolean isBetterBind(MethodDef test, MethodDef existing) {
					int testRating = rateBind(test);
					if (existing == null)
						return testRating < 6;// ignore invalid methods
					if (testRating < rateBind(existing))
						return true;

					return false;
				}

			});
			lifecycleMethods.putAll(classLifecyclemethods);
			bindmethods.putAll(classBindmethods);
			typeRef = clazz.getSuper();
			if (typeRef == null)
				break;
			clazz = analyzer.findClass(typeRef);
			privateAllowed = false;
			defaultAllowed = defaultAllowed && topPackage.equals(typeRef.getPackageRef()
				.getFQN());
		}

		if (cd.activate != null && !lifecycleMethods.containsKey(cd.activate)) {
			error("in component %s, activate method %s specified but not found", cd.implementation.getFQN(),
				cd.activate);
			cd.activate = null;
		}
		if (cd.deactivate != null && !lifecycleMethods.containsKey(cd.deactivate)) {
			error("in component %s, deactivate method %s specified but not found", cd.implementation.getFQN(),
				cd.deactivate);
			cd.activate = null;
		}
		if (cd.modified != null && !lifecycleMethods.containsKey(cd.modified)) {
			error("in component %s, modified method %s specified but not found", cd.implementation.getFQN(),
				cd.modified);
			cd.activate = null;
		}

		provide(cd, provides, impl);
		properties(cd, info, name);
		reference(info, impl, cd, bindmethods);
		// compute namespace after references, an updated method means ds 1.2.
		getNamespace(info, cd, lifecycleMethods);
		cd.prepare(analyzer);
		return cd.getTag();

	}

	private String checkIdentifier(String name, String value) {
		if (value != null) {
			if (!Verifier.isIdentifier(value)) {
				error("Component attribute %s has value %s but is not a Java identifier", name, value);
				return null;
			}
		}
		return value;
	}

	/**
	 * Check if we need to use the v1.1 namespace (or later).
	 *
	 * @param info
	 * @param cd TODO
	 * @param descriptors TODO
	 */
	private void getNamespace(Map<String, String> info, ComponentDef cd, Map<String, MethodDef> descriptors) {
		String namespace = info.get(COMPONENT_NAMESPACE);
		if (namespace != null) {
			cd.xmlns = namespace;
		}
		String version = info.get(COMPONENT_VERSION);
		if (version != null) {
			try {
				Version v = new Version(version);
				cd.updateVersion(v, "set XML namespace version: " + version);
			} catch (Exception e) {
				error("version: specified on component header but not a valid version: %s", version);
				return;
			}
		}
		for (String key : info.keySet()) {
			if (SET_COMPONENT_DIRECTIVES_1_2.contains(key)) {
				cd.updateVersion(V1_2, "uses 1.2 component directives like " + SET_COMPONENT_DIRECTIVES_1_2);
				return;
			}
		}
		for (ReferenceDef rd : cd.references.values()) {
			if (rd.updated != null) {
				cd.updateVersion(V1_2, "updated");
				return;
			}
		}
		// among other things this picks up any specified lifecycle methods
		for (String key : info.keySet()) {
			if (SET_COMPONENT_DIRECTIVES_1_1.contains(key)) {
				cd.updateVersion(V1_1, "1.1 component directives like " + SET_COMPONENT_DIRECTIVES_1_1);
				return;
			}
		}
		for (String lifecycle : LIFECYCLE_METHODS) {
			// lifecycle methods were not specified.... check for non 1.0
			// signatures.
			MethodDef test = descriptors.get(lifecycle);
			if (descriptors.containsKey(lifecycle) && (!(test.isPublic() || test.isProtected())
				|| rateLifecycle(test, "deactivate".equals(lifecycle) ? allowedDeactivate : allowed) > 1)) {
				cd.updateVersion(V1_1, "base lifecyle methods " + LIFECYCLE_METHODS);
				return;
			}
		}
	}

	/**
	 * Print the Service-Component properties element
	 *
	 * @param cd
	 * @param info
	 */
	void properties(ComponentDef cd, Map<String, String> info, String name) {
		Collection<String> properties = split(info.get(COMPONENT_PROPERTIES));
		for (String p : properties) {
			Matcher m = PROPERTY_PATTERN.matcher(p);

			if (m.matches()) {
				String key = m.group(2);
				String type = m.group(4);
				if (type == null)
					type = "String"; // default
				String value = m.group(5);
				String parts[] = value.split("\\s*(\\||\\n)\\s*");
				if (parts.length == 1 && value.endsWith("|")) {
					String v = parts[0];
					parts = new String[2];
					parts[0] = v;
					parts[1] = PropertyDef.MARKER;
				}
				cd.property.addProperty(key, type, parts);
			} else
				throw new IllegalArgumentException("Malformed property '" + p + "' on component: " + name);
		}
	}

	/**
	 * @param cd
	 * @param provides
	 */
	void provide(ComponentDef cd, String provides, String impl) {
		if (provides != null) {
			StringTokenizer st = new StringTokenizer(provides, ",");
			List<TypeRef> provide = new ArrayList<>();
			while (st.hasMoreTokens()) {
				String interfaceName = st.nextToken();
				TypeRef ref = analyzer.getTypeRefFromFQN(interfaceName);
				provide.add(ref);
				analyzer.nonClassReferTo(ref);

				// TODO verifies the impl. class extends or implements the
				// interface
			}
			cd.service = provide.toArray(new TypeRef[0]);
		}
	}

	public final static Pattern REFERENCE = Pattern.compile("([^(]+)(\\(.+\\))?");

	/**
	 * rates the methods according to the scale in 112.5.8 (compendium 4.3, ds
	 * 1.2), also returning "6" for invalid methods We don't look at return
	 * values yet due to proposal to all them for setting service properties.
	 *
	 * @param test methodDef to examine for suitability as a DS lifecycle method
	 * @param allowedParams TODO
	 * @return rating; 6 if invalid, lower is better
	 */
	int rateLifecycle(MethodDef test, Set<String> allowedParams) {
		TypeRef[] prototype = test.getDescriptor()
			.getPrototype();
		if (prototype.length == 1 && ComponentContextTR.equals(prototype[0].getFQN()))
			return 1;
		if (prototype.length == 1 && BundleContextTR.equals(prototype[0].getFQN()))
			return 2;
		if (prototype.length == 1 && MapTR.equals(prototype[0].getFQN()))
			return 3;
		if (prototype.length > 1) {
			for (TypeRef tr : prototype) {
				if (!allowedParams.contains(tr.getFQN()))
					return 6;
			}
			return 5;
		}
		if (prototype.length == 0)
			return 5;

		return 6;
	}

	/**
	 * see 112.3.2. We can't distinguish the bind type, so we just accept
	 * anything.
	 *
	 * @param test
	 */
	int rateBind(MethodDef test) {
		TypeRef[] prototype = test.getDescriptor()
			.getPrototype();
		if (prototype.length == 1 && ServiceReferenceTR.equals(prototype[0].getFQN()))
			return 1;
		if (prototype.length == 1)
			return 2;
		if (prototype.length == 2 && MapTR.equals(prototype[1].getFQN()))
			return 3;
		return 6;
	}

	/**
	 * @param info
	 * @param impl TODO
	 * @param descriptors TODO
	 * @param pw
	 * @throws Exception
	 */
	void reference(Map<String, String> info, String impl, ComponentDef cd, Map<String, MethodDef> descriptors)
		throws Exception {
		Collection<String> dynamic = new ArrayList<>(split(info.get(COMPONENT_DYNAMIC)));
		Collection<String> optional = new ArrayList<>(split(info.get(COMPONENT_OPTIONAL)));
		Collection<String> multiple = new ArrayList<>(split(info.get(COMPONENT_MULTIPLE)));
		Collection<String> greedy = new ArrayList<>(split(info.get(COMPONENT_GREEDY)));

		for (Map.Entry<String, String> entry : info.entrySet()) {

			// Skip directives
			String referenceName = entry.getKey();
			if (referenceName.endsWith(":")) {
				if (!SET_COMPONENT_DIRECTIVES.contains(referenceName))
					error("Unrecognized directive in " + Constants.SERVICE_COMPONENT + " header: %s", referenceName);
				continue;
			}

			// Parse the bind/unbind methods from the name
			// if set. They are separated by '/'
			String bind = null;
			String unbind = null;
			String updated = null;

			boolean bindCalculated = true;
			boolean unbindCalculated = true;
			boolean updatedCalculated = true;

			if (referenceName.indexOf('/') >= 0) {
				String parts[] = referenceName.split("/");
				referenceName = parts[0];
				if (parts[1].length() > 0) {
					bind = parts[1];
					bindCalculated = false;
				} else {
					bind = calculateBind(referenceName);
				}
				bind = parts[1].length() == 0 ? calculateBind(referenceName) : parts[1];
				if (parts.length > 2 && parts[2].length() > 0) {
					unbind = parts[2];
					unbindCalculated = false;
				} else {
					if (bind.startsWith("add"))
						unbind = bind.replaceAll("add(.+)", "remove$1");
					else
						unbind = "un" + bind;
				}
				if (parts.length > 3) {
					updated = parts[3];
					updatedCalculated = false;
				}
			} else if (Character.isLowerCase(referenceName.charAt(0))) {
				bind = calculateBind(referenceName);
				unbind = "un" + bind;
				updated = "updated" + Character.toUpperCase(referenceName.charAt(0)) + referenceName.substring(1);
			}

			String interfaceName = entry.getValue();
			if (interfaceName == null || interfaceName.length() == 0) {
				error("Invalid Interface Name for references in Service Component: %s=%s", referenceName,
					interfaceName);
				continue;
			}

			// If we have descriptors, we have analyzed the component.
			// So why not check the methods
			if (descriptors.size() > 0) {
				// Verify that the bind method exists
				if (!descriptors.containsKey(bind))
					if (bindCalculated)
						bind = null;
					else
						error("In component %s, the bind method %s for %s not defined", cd.effectiveName(), bind,
							referenceName);

				// Check if the unbind method exists
				if (!descriptors.containsKey(unbind)) {
					if (unbindCalculated)
						// remove it
						unbind = null;
					else
						error("In component %s, the unbind method %s for %s not defined", cd.effectiveName(), unbind,
							referenceName);
				}
				if (!descriptors.containsKey(updated)) {
					if (updatedCalculated)
						// remove it
						updated = null;
					else
						error("In component %s, the updated method %s for %s is not defined", cd.effectiveName(),
							updated, referenceName);
				}
			}
			// Check the cardinality by looking at the last
			// character of the value
			char c = interfaceName.charAt(interfaceName.length() - 1);
			if ("?+*~".indexOf(c) >= 0) {
				if (c == '?' || c == '*' || c == '~')
					optional.add(referenceName);
				if (c == '+' || c == '*')
					multiple.add(referenceName);
				if (c == '+' || c == '*' || c == '?')
					dynamic.add(referenceName);
				interfaceName = interfaceName.substring(0, interfaceName.length() - 1);
			}

			// Parse the target from the interface name
			// The target is a filter.
			String target = null;
			Matcher m = REFERENCE.matcher(interfaceName);
			if (m.matches()) {
				interfaceName = m.group(1);
				target = m.group(2);
			}
			TypeRef ref = analyzer.getTypeRefFromFQN(interfaceName);
			analyzer.nonClassReferTo(ref);
			ReferenceDef rd = new ReferenceDef(null);
			rd.name = referenceName;
			rd.service = interfaceName;

			if (optional.contains(referenceName)) {
				if (multiple.contains(referenceName)) {
					rd.cardinality = ReferenceCardinality.MULTIPLE;
				} else {
					rd.cardinality = ReferenceCardinality.OPTIONAL;
				}
			} else {
				if (multiple.contains(referenceName)) {
					rd.cardinality = ReferenceCardinality.AT_LEAST_ONE;
				} else {
					rd.cardinality = ReferenceCardinality.MANDATORY;
				}
			}
			if (bind != null) {
				rd.bind = bind;
				if (unbind != null) {
					rd.unbind = unbind;
				}
				if (updated != null) {
					rd.updated = updated;
				}
			}

			if (dynamic.contains(referenceName)) {
				rd.policy = ReferencePolicy.DYNAMIC;
				if (rd.unbind == null)
					error("In component %s, reference %s is dynamic but has no unbind method.", cd.effectiveName(),
						rd.name)
							.details(new DeclarativeServicesAnnotationError(cd.implementation.getFQN(), null, null,
								ErrorType.DYNAMIC_REFERENCE_WITHOUT_UNBIND));
			}

			if (greedy.contains(referenceName)) {
				rd.policyOption = ReferencePolicyOption.GREEDY;
			}

			if (target != null) {
				rd.target = target;
			}
			cd.references.put(referenceName, rd);
		}
	}

	private String calculateBind(String referenceName) {
		return "set" + Character.toUpperCase(referenceName.charAt(0)) + referenceName.substring(1);
	}

}
