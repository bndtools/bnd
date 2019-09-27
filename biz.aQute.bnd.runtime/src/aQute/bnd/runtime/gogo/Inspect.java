/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package aQute.bnd.runtime.gogo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import aQute.lib.dtoformatter.DTOFormatter;

public class Inspect {
	public static final String	NONSTANDARD_SERVICE_NAMESPACE	= "service";

	public static final String	CAPABILITY						= "capability";
	public static final String	REQUIREMENT						= "requirement";

	private static final String	EMPTY_MESSAGE					= "[EMPTY]";
	private static final String	UNUSED_MESSAGE					= "[UNUSED]";
	private static final String	UNRESOLVED_MESSAGE				= "[UNRESOLVED]";

	private final BundleContext	m_bc;

	public Inspect(BundleContext bc, DTOFormatter formatter) {
		m_bc = bc;
	}

	@Descriptor("inspects bundle capabilities and requirements")
	public void inspect(@Descriptor("('capability' | 'requirement')") String direction,
		@Descriptor("(<namespace> | 'service')") String namespace, @Descriptor("target bundles") Bundle[] bundles) {
		inspect(m_bc, direction, namespace, bundles);
	}

	private static void inspect(BundleContext bc, String direction, String namespace, Bundle[] bundles) {
		// Verify arguments.
		if (isValidDirection(direction)) {
			bundles = ((bundles == null) || (bundles.length == 0)) ? bc.getBundles() : bundles;

			if (CAPABILITY.startsWith(direction)) {
				printCapabilities(bc, Util.parseSubstring(namespace), bundles);
			} else {
				printRequirements(bc, Util.parseSubstring(namespace), bundles);
			}
		} else {
			if (!isValidDirection(direction)) {
				System.out.println("Invalid argument: " + direction);
			}
		}
	}

	public static void printCapabilities(BundleContext bc, List<String> namespace, Bundle[] bundles) {
		boolean separatorNeeded = false;
		for (Bundle b : bundles) {
			if (separatorNeeded) {
				System.out.println();
			}

			// Print out any matching generic capabilities.
			BundleWiring wiring = b.adapt(BundleWiring.class);
			if (wiring != null) {
				String title = b + " provides:";
				System.out.println(title);
				System.out.println(Util.getUnderlineString(title.length()));

				// Print generic capabilities for matching namespaces.
				boolean matches = printMatchingCapabilities(wiring, namespace);

				// Handle service capabilities separately, since they aren't
				// part
				// of the generic model in OSGi.
				if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE)) {
					matches |= printServiceCapabilities(b);
				}

				// If there were no capabilities for the specified namespace,
				// then say so.
				if (!matches) {
					System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
				}
			} else {
				System.out.println("Bundle " + b.getBundleId() + " is not resolved.");
			}
			separatorNeeded = true;
		}
	}

	private static boolean printMatchingCapabilities(BundleWiring wiring, List<String> namespace) {
		List<BundleWire> wires = wiring.getProvidedWires(null);
		Map<BundleCapability, List<BundleWire>> aggregateCaps = aggregateCapabilities(namespace, wires);
		List<BundleCapability> allCaps = wiring.getCapabilities(null);
		boolean matches = false;
		for (BundleCapability cap : allCaps) {
			if (matchNamespace(namespace, cap.getNamespace())) {
				matches = true;
				List<BundleWire> dependents = aggregateCaps.get(cap);
				Object keyAttr = cap.getAttributes()
					.get(cap.getNamespace());
				if (dependents != null) {
					String msg;
					if (keyAttr != null) {
						msg = cap.getNamespace() + "; " + keyAttr + " " + getVersionFromCapability(cap);
					} else {
						msg = cap.toString();
					}
					msg = msg + " required by:";
					System.out.println(msg);
					for (BundleWire wire : dependents) {
						System.out.println("   " + wire.getRequirerWiring()
							.getBundle());
					}
				} else if (keyAttr != null) {
					System.out.println(cap.getNamespace() + "; " + cap.getAttributes()
						.get(cap.getNamespace()) + " " + getVersionFromCapability(cap) + " " + UNUSED_MESSAGE);
				} else {
					System.out.println(cap + " " + UNUSED_MESSAGE);
				}
			}
		}
		return matches;
	}

	private static Map<BundleCapability, List<BundleWire>> aggregateCapabilities(List<String> namespace,
		List<BundleWire> wires) {
		// Aggregate matching capabilities.
		Map<BundleCapability, List<BundleWire>> map = new HashMap<>();
		for (BundleWire wire : wires) {
			if (matchNamespace(namespace, wire.getCapability()
				.getNamespace())) {
				List<BundleWire> dependents = map.get(wire.getCapability());
				if (dependents == null) {
					dependents = new ArrayList<>();
					map.put(wire.getCapability(), dependents);
				}
				dependents.add(wire);
			}
		}
		return map;
	}

	static boolean printServiceCapabilities(Bundle b) {
		boolean matches = false;

		try {
			ServiceReference<?>[] refs = b.getRegisteredServices();

			if ((refs != null) && (refs.length > 0)) {
				matches = true;
				// Print properties for each service.
				for (ServiceReference<?> ref : refs) {
					// Print object class with "namespace".
					System.out.println(NONSTANDARD_SERVICE_NAMESPACE + "; "
						+ Util.getValueString(ref.getProperty("objectClass")) + " with properties:");
					// Print service properties.
					String[] keys = ref.getPropertyKeys();
					for (String key : keys) {
						if (!key.equalsIgnoreCase(Constants.OBJECTCLASS)) {
							Object v = ref.getProperty(key);
							System.out.println("   " + key + " = " + Util.getValueString(v));
						}
					}
					Bundle[] users = ref.getUsingBundles();
					if ((users != null) && (users.length > 0)) {
						System.out.println("   Used by:");
						for (Bundle user : users) {
							System.out.println("      " + user);
						}
					}
				}
			}
		} catch (Exception ex) {
			System.err.println(ex.toString());
		}

		return matches;
	}

	public static void printRequirements(BundleContext bc, List<String> namespace, Bundle[] bundles) {
		boolean separatorNeeded = false;
		for (Bundle b : bundles) {
			if (separatorNeeded) {
				System.out.println();
			}

			// Print out any matching generic requirements.
			BundleWiring wiring = b.adapt(BundleWiring.class);
			if (wiring != null) {
				String title = b + " requires:";
				System.out.println(title);
				System.out.println(Util.getUnderlineString(title.length()));
				boolean matches = printMatchingRequirements(wiring, namespace);

				// Handle service requirements separately, since they aren't
				// part
				// of the generic model in OSGi.
				if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE)) {
					matches |= printServiceRequirements(b);
				}

				// If there were no requirements for the specified namespace,
				// then say so.
				if (!matches) {
					System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
				}
			} else {
				System.out.println("Bundle " + b.getBundleId() + " is not resolved.");
			}

			separatorNeeded = true;
		}
	}

	private static boolean printMatchingRequirements(BundleWiring wiring, List<String> namespace) {
		List<BundleWire> wires = wiring.getRequiredWires(null);
		Map<BundleRequirement, List<BundleWire>> aggregateReqs = aggregateRequirements(namespace, wires);
		List<BundleRequirement> allReqs = wiring.getRequirements(null);
		boolean matches = false;
		for (BundleRequirement req : allReqs) {
			if (matchNamespace(namespace, req.getNamespace())) {
				matches = true;
				List<BundleWire> providers = aggregateReqs.get(req);
				if (providers != null) {
					System.out.println(req.getNamespace() + "; " + req.getDirectives()
						.get(Constants.FILTER_DIRECTIVE) + " resolved by:");
					for (BundleWire wire : providers) {
						String msg;
						Object keyAttr = wire.getCapability()
							.getAttributes()
							.get(wire.getCapability()
								.getNamespace());
						if (keyAttr != null) {
							msg = wire.getCapability()
								.getNamespace() + "; " + keyAttr + " " + getVersionFromCapability(wire.getCapability());
						} else {
							msg = wire.getCapability()
								.toString();
						}
						msg = "   " + msg + " from " + wire.getProviderWiring()
							.getBundle();
						System.out.println(msg);
					}
				} else {
					System.out.println(req.getNamespace() + "; " + req.getDirectives()
						.get(Constants.FILTER_DIRECTIVE) + " " + UNRESOLVED_MESSAGE);
				}
			}
		}
		return matches;
	}

	private static Map<BundleRequirement, List<BundleWire>> aggregateRequirements(List<String> namespace,
		List<BundleWire> wires) {
		// Aggregate matching capabilities.
		Map<BundleRequirement, List<BundleWire>> map = new HashMap<>();
		for (BundleWire wire : wires) {
			if (matchNamespace(namespace, wire.getRequirement()
				.getNamespace())) {
				List<BundleWire> providers = map.get(wire.getRequirement());
				if (providers == null) {
					providers = new ArrayList<>();
					map.put(wire.getRequirement(), providers);
				}
				providers.add(wire);
			}
		}
		return map;
	}

	static boolean printServiceRequirements(Bundle b) {
		boolean matches = false;

		try {
			ServiceReference<?>[] refs = b.getServicesInUse();

			if ((refs != null) && (refs.length > 0)) {
				matches = true;
				// Print properties for each service.
				for (ServiceReference<?> ref : refs) {
					// Print object class with "namespace".
					System.out.println(NONSTANDARD_SERVICE_NAMESPACE + "; "
						+ Util.getValueString(ref.getProperty("objectClass")) + " provided by:");
					System.out.println("   " + ref.getBundle());
				}
			}
		} catch (Exception ex) {
			System.err.println(ex.toString());
		}

		return matches;
	}

	private static String getVersionFromCapability(BundleCapability c) {
		Object o = c.getAttributes()
			.get(Constants.VERSION_ATTRIBUTE);
		if (o == null) {
			o = c.getAttributes()
				.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
		}
		return (o == null) ? "" : o.toString();
	}

	private static boolean matchNamespace(List<String> namespace, String actual) {
		return Util.compareSubstring(namespace, actual);
	}

	private static boolean isValidDirection(String direction) {
		return (CAPABILITY.startsWith(direction) || REQUIREMENT.startsWith(direction));
	}

}
