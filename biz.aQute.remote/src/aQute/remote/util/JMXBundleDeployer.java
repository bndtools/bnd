package aQute.remote.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.osgi.framework.Bundle;
import org.osgi.framework.dto.BundleDTO;

/**
 * This class will try to connect to a remote OSGi framework using JMX and will
 * deploy a bundle for you, by deploy, that means install the bundle if it
 * doesn't existing in the remote runtime or update the bundle if it already
 * exists. For the actual JMX connection it will use a port if you tell it to,
 * or if not, it will try to use the JDK's attach API and search for the OSGi
 * framework JMX beans. For the JDK attach API, beware, assumptions about the
 * Oracle JDK directory layout have been made.
 */
public class JMXBundleDeployer {

	private final static String		OBJECTNAME	= "osgi.core";

	private MBeanServerConnection	mBeanServerConnection;

	public JMXBundleDeployer() {
		this(getLocalConnectorAddress());
	}

	public JMXBundleDeployer(int port) {
		this("service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi");
	}

	public JMXBundleDeployer(String serviceURL) {
		try {
			final JMXServiceURL jmxServiceUrl = new JMXServiceURL(serviceURL);
			final JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceUrl, null);

			mBeanServerConnection = jmxConnector.getMBeanServerConnection();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to get JMX connection", e);
		}
	}

	/**
	 * Gets the current list of installed bsns, compares it to the bsn provided.
	 * If bsn doesn't exist, then install it. If it does exist then update it.
	 *
	 * @param bsn Bundle-SymbolicName of bundle you are wanting to deploy
	 * @param bundle the bundle
	 * @return the id of the updated or installed bundle
	 * @throws Exception
	 */
	public long deploy(String bsn, File bundle) throws Exception {
		final ObjectName framework = getFramework(mBeanServerConnection);

		long bundleId = -1;

		for (BundleDTO osgiBundle : listBundles()) {
			if (osgiBundle.symbolicName.equals(bsn)) {
				bundleId = osgiBundle.id;
				break;
			}
		}

		// TODO serve bundle url over http so it works for non file:// urls

		if (bundleId > -1) {
			mBeanServerConnection.invoke(framework, "stopBundle", new Object[] {
				bundleId
			}, new String[] {
				"long"
			});

			mBeanServerConnection.invoke(framework, "updateBundleFromURL", new Object[] {
				bundleId, bundle.toURI()
					.toURL()
					.toExternalForm()
			}, new String[] {
				"long", String.class.getName()
			});

			mBeanServerConnection.invoke(framework, "refreshBundle", new Object[] {
				bundleId
			}, new String[] {
				"long"
			});
		} else {
			Object installed = mBeanServerConnection.invoke(framework, "installBundleFromURL", new Object[] {
				bundle.getAbsolutePath(), bundle.toURI()
					.toURL()
					.toExternalForm()
			}, new String[] {
				String.class.getName(), String.class.getName()
			});

			bundleId = Long.parseLong(installed.toString());
		}

		mBeanServerConnection.invoke(framework, "startBundle", new Object[] {
			bundleId
		}, new String[] {
			"long"
		});

		return bundleId;
	}

	private ObjectName getBundleState() throws MalformedObjectNameException, IOException {

		return mBeanServerConnection.queryNames(new ObjectName(OBJECTNAME + ":type=bundleState,*"), null)
			.iterator()
			.next();
	}

	private static ObjectName getFramework(MBeanServerConnection mBeanServerConnection)
		throws MalformedObjectNameException, IOException {

		final ObjectName objectName = new ObjectName(OBJECTNAME + ":type=framework,*");
		final Set<ObjectName> objectNames = mBeanServerConnection.queryNames(objectName, null);

		if (objectNames != null && objectNames.size() > 0) {
			return objectNames.iterator()
				.next();
		}

		return null;
	}

	/**
	 * Calls osgi.core bundleState MBean listBundles operation
	 *
	 * @return array of bundles in framework
	 */
	public BundleDTO[] listBundles() {
		final List<BundleDTO> retval = new ArrayList<>();

		try {
			final ObjectName bundleState = getBundleState();

			final Object[] params = new Object[] {
				new String[] {
					"Identifier", "SymbolicName", "State", "Version",
				}
			};

			final String[] signature = new String[] {
				String[].class.getName()
			};

			final TabularData data = (TabularData) mBeanServerConnection.invoke(bundleState, "listBundles", params,
				signature);

			for (Object value : data.values()) {
				final CompositeData cd = (CompositeData) value;

				try {
					retval.add(newFromData(cd));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return retval.toArray(new BundleDTO[0]);
	}

	private static BundleDTO newFromData(CompositeData cd) {
		final BundleDTO dto = new BundleDTO();
		dto.id = Long.parseLong(cd.get("Identifier")
			.toString());
		dto.symbolicName = cd.get("SymbolicName")
			.toString();

		String state = cd.get("State")
			.toString();

		if ("UNINSTALLED".equals(state)) {
			dto.state = Bundle.UNINSTALLED;
		} else if ("INSTALLED".equals(state)) {
			dto.state = Bundle.INSTALLED;
		} else if ("RESOLVED".equals(state)) {
			dto.state = Bundle.RESOLVED;
		} else if ("STARTING".equals(state)) {
			dto.state = Bundle.STARTING;
		} else if ("STOPPING".equals(state)) {
			dto.state = Bundle.STOPPING;
		} else if ("ACTIVE".equals(state)) {
			dto.state = Bundle.ACTIVE;
		}

		dto.version = cd.get("Version")
			.toString();

		return dto;
	}

	/**
	 * Uninstall a bundle by passing in its Bundle-SymbolicName. If bundle
	 * doesn't exist, this is a NOP.
	 *
	 * @param bsn bundle symbolic name
	 * @throws Exception
	 */
	public void uninstall(String bsn) throws Exception {
		for (BundleDTO osgiBundle : listBundles()) {
			if (osgiBundle.symbolicName.equals(bsn)) {
				uninstall(osgiBundle.id);

				return;
			}
		}

		throw new IllegalStateException("Unable to uninstall " + bsn);
	}

	/**
	 * Calls through directly to the OSGi frameworks MBean uninstallBundle
	 * operation
	 *
	 * @param id id of bundle to uninstall
	 * @throws Exception
	 */
	public void uninstall(long id) throws Exception {
		final ObjectName framework = getFramework(mBeanServerConnection);

		Object[] objects = new Object[] {
			id
		};

		String[] params = new String[] {
			"long"
		};

		mBeanServerConnection.invoke(framework, "uninstallBundle", objects, params);
	}

	/**
	 * Uses Oracle JDK's Attach API to try to search VMs on this machine looking
	 * for the osgi.core MBeans. This will stop searching for VMs once the
	 * MBeans are found. Beware if you have multiple JVMs with osgi.core MBeans
	 * published.
	 */
	@SuppressWarnings("unchecked")
	static String getLocalConnectorAddress() {
		ClassLoader cl = Thread.currentThread()
			.getContextClassLoader();
		ClassLoader toolsClassloader = null;

		try {
			toolsClassloader = getToolsClassLoader(cl);

			if (toolsClassloader != null) {
				Thread.currentThread()
					.setContextClassLoader(toolsClassloader);

				Class<?> vmClass = toolsClassloader.loadClass("com.sun.tools.attach.VirtualMachine");

				Method listMethod = vmClass.getMethod("list");
				List<Object> vmds = (List<Object>) listMethod.invoke(null);

				for (Object vmd : vmds) {
					try {
						Class<?> vmdClass = toolsClassloader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
						Method idMethod = vmdClass.getMethod("id");
						String id = (String) idMethod.invoke(vmd);

						Method attachMethod = vmClass.getMethod("attach", String.class);
						Object vm = attachMethod.invoke(null, id);

						try {
							Method getAgentPropertiesMethod = vmClass.getMethod("getAgentProperties");
							Properties agentProperties = (Properties) getAgentPropertiesMethod.invoke(vm);

							String localConnectorAddress = agentProperties
								.getProperty("com.sun.management.jmxremote.localConnectorAddress");

							if (localConnectorAddress == null) {
								File agentJar = findJdkJar("management-agent.jar");

								if (agentJar != null) {
									Method loadAgent = vmClass.getMethod("loadAgent", String.class);
									loadAgent.invoke(vm, agentJar.getCanonicalPath());

									agentProperties = (Properties) getAgentPropertiesMethod.invoke(vm);

									localConnectorAddress = agentProperties
										.getProperty("com.sun.management.jmxremote.localConnectorAddress");
								}
							}

							if (localConnectorAddress != null) {
								final JMXServiceURL jmxServiceUrl = new JMXServiceURL(localConnectorAddress);
								final JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceUrl, null);

								final MBeanServerConnection mBeanServerConnection = jmxConnector
									.getMBeanServerConnection();

								if (mBeanServerConnection != null) {
									final ObjectName framework = getFramework(mBeanServerConnection);

									if (framework != null) {
										return localConnectorAddress;
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							Method detachMethod = vmClass.getMethod("detach");
							detachMethod.invoke(vm);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread()
				.setContextClassLoader(cl);
			// try to get custom classloader to unload native libs
			try {
				if (toolsClassloader != null) {
					Field nl = ClassLoader.class.getDeclaredField("nativeLibraries");
					nl.setAccessible(true);
					Vector<?> nativeLibs = (Vector<?>) nl.get(toolsClassloader);
					for (Object nativeLib : nativeLibs) {
						Field nameField = nativeLib.getClass()
							.getDeclaredField("name");
						nameField.setAccessible(true);
						String name = (String) nameField.get(nativeLib);

						if (new File(name).getName()
							.contains("attach")) {
							Method f = nativeLib.getClass()
								.getDeclaredMethod("finalize");
							f.setAccessible(true);
							f.invoke(nativeLib);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private static ClassLoader getToolsClassLoader(ClassLoader parent) throws IOException {
		File toolsJar = findJdkJar("tools.jar");

		if (toolsJar != null && toolsJar.exists()) {
			URL toolsUrl = null;

			try {
				toolsUrl = toolsJar.toURI()
					.toURL();
			} catch (MalformedURLException e) {
				//
			}

			URL[] urls = new URL[] {
				toolsUrl
			};

			return new URLClassLoader(urls, parent);
		}

		return null;
	}

	static File findJdkJar(String jar) throws IOException {
		File retval = null;

		final String jarPath = File.separator + "lib" + File.separator + jar;
		final String javaHome = System.getProperty("java.home");
		File jarFile = new File(javaHome + jarPath);

		if (jarFile.exists()) {
			retval = jarFile;
		} else {
			jarFile = new File(javaHome + "/.." + jarPath);

			if (jarFile.exists()) {
				retval = jarFile.getCanonicalFile();
			}
		}

		return retval;
	}
}
