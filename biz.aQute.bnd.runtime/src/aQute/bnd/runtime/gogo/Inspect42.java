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
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class Inspect42 {
	public static final String	LEGACY_PACKAGE_NAMESPACE		= "package";
	public static final String	LEGACY_BUNDLE_NAMESPACE			= "bundle";
	public static final String	LEGACY_HOST_NAMESPACE			= "host";
	public static final String	NONSTANDARD_SERVICE_NAMESPACE	= "service";

	public static final String	CAPABILITY						= "capability";
	public static final String	REQUIREMENT						= "requirement";

	private static final String	EMPTY_MESSAGE					= "[EMPTY]";
	private static final String	UNUSED_MESSAGE					= "[UNUSED]";
	private static final String	UNRESOLVED_MESSAGE				= "[UNRESOLVED]";

	private final BundleContext	m_bc;

	public Inspect42(BundleContext bc) {
		m_bc = bc;
	}

	@Descriptor("inspects bundle capabilities and requirements")
	public void inspect(@Descriptor("('capability' | 'requirement')") String direction,
		@Descriptor("('package' | 'bundle' | 'host' | 'service')") String namespace,
		@Descriptor("target bundles") Bundle[] bundles) {
		inspect(m_bc, direction, namespace, bundles);
	}

	private static void inspect(BundleContext bc, String direction, String namespace, Bundle[] bundles) {
		// Verify arguments.
		if (isValidDirection(direction)) {
			bundles = ((bundles == null) || (bundles.length == 0)) ? bc.getBundles() : bundles;

			if (CAPABILITY.startsWith(direction)) {
				printNonstandardCapabilities(bc, Util.parseSubstring(namespace), bundles);
			} else {
				printNonstandardRequirements(bc, Util.parseSubstring(namespace), bundles);
			}
		} else {
			if (!isValidDirection(direction)) {
				System.out.println("Invalid argument: " + direction);
			}
		}
	}

	private static void printNonstandardCapabilities(BundleContext bc, List<String> namespace, Bundle[] bundles) {
		boolean separatorNeeded = false;
		for (Bundle b : bundles) {
			if (separatorNeeded) {
				System.out.println();
			}
			String title = b + " provides:";
			System.out.println(title);
			System.out.println(Util.getUnderlineString(title.length()));
			boolean matches = false;

			if (matchNamespace(namespace, LEGACY_BUNDLE_NAMESPACE)) {
				matches |= printRequiringBundles(bc, b);
			}
			if (matchNamespace(namespace, LEGACY_HOST_NAMESPACE)) {
				matches |= printHostedFragments(bc, b);
			}
			if (matchNamespace(namespace, LEGACY_PACKAGE_NAMESPACE)) {
				matches |= printExportedPackages(bc, b);
			}
			if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE)) {
				matches |= Inspect.printServiceCapabilities(b);
			}

			// If there were no capabilities for the specified namespace,
			// then say so.
			if (!matches) {
				System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
			}
			separatorNeeded = true;
		}
	}

	private static void printNonstandardRequirements(BundleContext bc, List<String> namespace, Bundle[] bundles) {
		boolean separatorNeeded = false;
		for (Bundle b : bundles) {
			if (separatorNeeded) {
				System.out.println();
			}
			String title = b + " requires:";
			System.out.println(title);
			System.out.println(Util.getUnderlineString(title.length()));
			boolean matches = false;
			if (matchNamespace(namespace, LEGACY_BUNDLE_NAMESPACE)) {
				matches |= printRequiredBundles(bc, b);
			}
			if (matchNamespace(namespace, LEGACY_HOST_NAMESPACE)) {
				matches |= printFragmentHosts(bc, b);
			}
			if (matchNamespace(namespace, LEGACY_PACKAGE_NAMESPACE)) {
				matches |= printImportedPackages(bc, b);
			}
			if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE)) {
				matches |= Inspect.printServiceRequirements(b);
			}

			// If there were no capabilities for the specified namespace,
			// then say so.
			if (!matches) {
				System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
			}
			separatorNeeded = true;
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean printExportedPackages(BundleContext bc, Bundle b) {
		boolean matches = false;

		// Keep track of service references.
		List<ServiceReference<?>> refs = new ArrayList<>();

		// Fragments cannot export packages.
		if (!isFragment(b)) {
			// Get package admin service.
			org.osgi.service.packageadmin.PackageAdmin pa = Util.getService(bc,
				org.osgi.service.packageadmin.PackageAdmin.class, refs);
			if (pa == null) {
				System.out.println("PackageAdmin service is unavailable.");
			} else {
				try {
					org.osgi.service.packageadmin.ExportedPackage[] exports = pa.getExportedPackages(b);
					if (exports != null) {
						for (org.osgi.service.packageadmin.ExportedPackage ep : exports) {
							matches = true;
							Bundle[] importers = ep.getImportingBundles();
							if ((importers != null) && (importers.length > 0)) {
								String msg = LEGACY_PACKAGE_NAMESPACE + "; " + ep.getName() + "; " + ep.getVersion()
									.toString() + " required by:";
								System.out.println(msg);
								for (Bundle importer : importers) {
									System.out.println("   " + importer);
								}
							} else {
								System.out
									.println(LEGACY_PACKAGE_NAMESPACE + "; " + ep.getName() + "; " + ep.getVersion()
										.toString() + " " + UNUSED_MESSAGE);
							}
						}
					}
				} catch (Exception ex) {
					System.err.println(ex.toString());
				}
			}
		}

		Util.ungetServices(bc, refs);

		return matches;
	}

	@SuppressWarnings("deprecation")
	private static boolean printImportedPackages(BundleContext bc, Bundle b) {
		boolean matches = false;

		// Keep track of service references.
		List<ServiceReference<?>> refs = new ArrayList<>();

		// Fragments cannot import packages.
		if (!isFragment(b)) {
			// Get package admin service.
			org.osgi.service.packageadmin.PackageAdmin pa = Util.getService(bc,
				org.osgi.service.packageadmin.PackageAdmin.class, refs);
			if (pa == null) {
				System.out.println("PackageAdmin service is unavailable.");
			} else {
				org.osgi.service.packageadmin.ExportedPackage[] exports = pa.getExportedPackages((Bundle) null);
				if (exports != null) {
					for (org.osgi.service.packageadmin.ExportedPackage ep : exports) {
						Bundle[] importers = ep.getImportingBundles();
						if (importers != null) {
							for (Bundle importer : importers) {
								if (importer == b) {
									matches = true;
									System.out
										.println(LEGACY_PACKAGE_NAMESPACE + "; " + ep.getName() + " resolved by:");
									System.out.println("   " + ep.getName() + "; " + ep.getVersion()
										.toString() + " from " + ep.getExportingBundle());
								}
							}
						}
					}
				}
			}
		}

		Util.ungetServices(bc, refs);

		return matches;
	}

	@SuppressWarnings("deprecation")
	public static boolean printRequiringBundles(BundleContext bc, Bundle b) {
		boolean matches = false;

		// Keep track of service references.
		List<ServiceReference<?>> refs = new ArrayList<>();

		// Fragments cannot be required.
		if (!isFragment(b)) {
			// Get package admin service.
			org.osgi.service.packageadmin.PackageAdmin pa = Util.getService(bc,
				org.osgi.service.packageadmin.PackageAdmin.class, refs);
			if (pa == null) {
				System.out.println("PackageAdmin service is unavailable.");
			} else {
				try {
					org.osgi.service.packageadmin.RequiredBundle[] rbs = pa.getRequiredBundles(b.getSymbolicName());
					if (rbs != null) {
						for (org.osgi.service.packageadmin.RequiredBundle rb : rbs) {
							if (rb.getBundle() == b) {
								Bundle[] requires = rb.getRequiringBundles();
								if ((requires != null) && (requires.length > 0)) {
									matches = true;
									System.out.println(
										LEGACY_BUNDLE_NAMESPACE + "; " + b.getSymbolicName() + "; " + b.getVersion()
											.toString() + " required by:");
									for (Bundle requirer : requires) {
										System.out.println("   " + requirer);
									}
								}
							}
						}
					}

					if (!matches) {
						matches = true;
						System.out.println(LEGACY_BUNDLE_NAMESPACE + "; " + b.getSymbolicName() + "; " + b.getVersion()
							.toString() + " " + UNUSED_MESSAGE);
					}

				} catch (Exception ex) {
					System.err.println(ex.toString());
				}
			}
		}

		Util.ungetServices(bc, refs);

		return matches;
	}

	@SuppressWarnings("deprecation")
	private static boolean printRequiredBundles(BundleContext bc, Bundle b) {
		boolean matches = false;

		// Keep track of service references.
		List<ServiceReference<?>> refs = new ArrayList<>();

		// Fragments cannot require bundles.
		if (!isFragment(b)) {
			// Get package admin service.
			org.osgi.service.packageadmin.PackageAdmin pa = Util.getService(bc,
				org.osgi.service.packageadmin.PackageAdmin.class, refs);
			if (pa == null) {
				System.out.println("PackageAdmin service is unavailable.");
			} else {
				org.osgi.service.packageadmin.RequiredBundle[] rbs = pa.getRequiredBundles(null);
				if (rbs != null) {
					for (org.osgi.service.packageadmin.RequiredBundle rb : rbs) {
						Bundle[] requirers = rb.getRequiringBundles();
						if (requirers != null) {
							for (Bundle requirer : requirers) {
								if (requirer == b) {
									matches = true;
									System.out.println(
										LEGACY_BUNDLE_NAMESPACE + "; " + rb.getSymbolicName() + " resolved by:");
									System.out.println("   " + rb.getBundle());
								}
							}
						}
					}
				}
			}
		}

		Util.ungetServices(bc, refs);

		return matches;
	}

	@SuppressWarnings("deprecation")
	public static boolean printHostedFragments(BundleContext bc, Bundle b) {
		boolean matches = false;

		// Keep track of service references.
		List<ServiceReference<?>> refs = new ArrayList<>();

		// Get package admin service.
		org.osgi.service.packageadmin.PackageAdmin pa = Util.getService(bc,
			org.osgi.service.packageadmin.PackageAdmin.class, refs);
		if (pa == null) {
			System.out.println("PackageAdmin service is unavailable.");
		} else {
			try {
				if (!isFragment(b)) {
					matches = true;
					Bundle[] fragments = pa.getFragments(b);
					if ((fragments != null) && (fragments.length > 0)) {
						System.out.println(LEGACY_HOST_NAMESPACE + "; " + b.getSymbolicName() + "; " + b.getVersion()
							.toString() + " required by:");
						for (Bundle fragment : fragments) {
							System.out.println("   " + fragment);
						}
					} else {
						System.out.println(LEGACY_HOST_NAMESPACE + "; " + b.getSymbolicName() + "; " + b.getVersion()
							.toString() + " " + UNUSED_MESSAGE);
					}
				}
			} catch (Exception ex) {
				System.err.println(ex.toString());
			}

			Util.ungetServices(bc, refs);
		}

		return matches;
	}

	@SuppressWarnings("deprecation")
	public static boolean printFragmentHosts(BundleContext bc, Bundle b) {
		boolean matches = false;

		// Keep track of service references.
		List<ServiceReference<?>> refs = new ArrayList<>();

		// Get package admin service.
		org.osgi.service.packageadmin.PackageAdmin pa = Util.getService(bc,
			org.osgi.service.packageadmin.PackageAdmin.class, refs);
		if (pa == null) {
			System.out.println("PackageAdmin service is unavailable.");
		} else {
			try {
				if (isFragment(b)) {
					matches = true;

					Bundle[] hosts = pa.getHosts(b);
					if ((hosts != null) && (hosts.length > 0)) {
						System.out.println(LEGACY_HOST_NAMESPACE + "; " + b.getHeaders()
							.get(Constants.FRAGMENT_HOST) + " resolved by:");
						for (Bundle host : hosts) {
							System.out.println("   " + host);
						}
					} else {
						System.out.println(LEGACY_HOST_NAMESPACE + "; " + b.getHeaders()
							.get(Constants.FRAGMENT_HOST) + " " + UNRESOLVED_MESSAGE);
					}
				}
			} catch (Exception ex) {
				System.err.println(ex.toString());
			}

			Util.ungetServices(bc, refs);
		}

		return matches;
	}

	private static boolean matchNamespace(List<String> namespace, String actual) {
		return Util.compareSubstring(namespace, actual);
	}

	private static boolean isValidDirection(String direction) {
		return (CAPABILITY.startsWith(direction) || REQUIREMENT.startsWith(direction));
	}

	private static boolean isFragment(Bundle bundle) {
		return bundle.getHeaders()
			.get(Constants.FRAGMENT_HOST) != null;
	}
}
