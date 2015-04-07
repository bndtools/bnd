package aQute.remote.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import javax.management.*;
import javax.management.openmbean.*;
import javax.management.remote.*;

import org.osgi.framework.dto.*;

public class JMXBundleDeployer {

	private final static String		OBJECTNAME		= "osgi.core";
	private static ClassLoader		toolsClassloader	= null;

	private MBeanServerConnection mBeanServerConnection;

	public JMXBundleDeployer() {
		this(getLocalConnectorAddress());
	}

	public JMXBundleDeployer(int port) {
		this("service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi");
	}

	public JMXBundleDeployer(String serviceURL) {
		try {
			final JMXServiceURL jmxServiceUrl = new JMXServiceURL(serviceURL);
			final JMXConnector jmxConnector = JMXConnectorFactory.connect(
				jmxServiceUrl, null);

			mBeanServerConnection = jmxConnector.getMBeanServerConnection();
		} catch (Exception e) {
			throw new IllegalArgumentException(
				"Unable to get JMX connection", e);
		}
	}

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
			mBeanServerConnection.invoke(framework, "stopBundle",
					new Object[] { bundleId }, new String[] { "long" });

			mBeanServerConnection.invoke(framework, "updateBundleFromURL",
					new Object[] { bundleId,
					bundle.toURI().toURL().toExternalForm() },
					new String[] { "long", String.class.getName() });

			mBeanServerConnection.invoke(framework, "refreshBundle",
					new Object[] { bundleId }, new String[] { "long" });
		} else {
			Object installed = mBeanServerConnection.invoke(
					framework,
					"installBundleFromURL",
					new Object[] { bundle.getAbsolutePath(),
							bundle.toURI().toURL().toExternalForm() },
					new String[] { String.class.getName(),
							String.class.getName() });

			bundleId = Long.parseLong(installed.toString());
		}

		mBeanServerConnection.invoke(framework, "startBundle",
			new Object[] { bundleId }, new String[] { "long" });

		return bundleId;
	}

	private ObjectName getBundleState() throws MalformedObjectNameException, IOException {

		return mBeanServerConnection.queryNames(new ObjectName(OBJECTNAME + ":type=bundleState,*"), null).iterator()
				.next();
	}

	private static ObjectName getFramework(MBeanServerConnection mBeanServerConnection)
			throws MalformedObjectNameException, IOException {

		final ObjectName objectName = new ObjectName(OBJECTNAME + ":type=framework,*");
		final Set<ObjectName> objectNames = mBeanServerConnection.queryNames(objectName, null);

		if (objectNames != null && objectNames.size() > 0) {
			return objectNames.iterator().next();
		}

		return null;
	}

	public BundleDTO[] listBundles() {
		final List<BundleDTO> retval = new ArrayList<BundleDTO>();

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
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return retval.toArray(new BundleDTO[0]);
	}

	private static BundleDTO newFromData(CompositeData cd) {
		final BundleDTO dto = new BundleDTO();
		dto.id = Long.parseLong(cd.get("Identifier").toString());
		dto.symbolicName = cd.get("SymbolicName").toString();

		return dto;
	}

	public void uninstall(String bsn) throws Exception {
		for (BundleDTO osgiBundle : listBundles()) {
			if (osgiBundle.symbolicName.equals(bsn)) {
				uninstall(osgiBundle.id);

				return;
			}
		}

		throw new IllegalStateException("Unable to uninstall " + bsn);
	}

	public void uninstall(long id) throws Exception {
		final ObjectName framework = getFramework(mBeanServerConnection);

		mBeanServerConnection.invoke(framework, "uninstallBundle", new Object[] {
			id
		}, new String[] {
			"long"
		});
	}

	@SuppressWarnings("unchecked")
	private static String getLocalConnectorAddress() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		try {
			ClassLoader tl = getToolsClassLoader(cl);

			if (tl != null) {
				Thread.currentThread().setContextClassLoader(tl);

				Class< ? > vmClass = tl.loadClass("com.sun.tools.attach.VirtualMachine");
				Method listMethod = vmClass.getMethod("list");
				List<Object> vmds = (List<Object>) listMethod.invoke(null);

				for (Object vmd : vmds) {
					try {
						Class< ? > vmdClass = tl.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
						Method idMethod = vmdClass.getMethod("id");
						String id = (String) idMethod.invoke(vmd);

						Method attachMethod = vmClass.getMethod("attach", String.class);
						// FIXME this can't be called twice, throws native
						// loadLibrary "attach" has already been loaded by
						// another classloader
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
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						finally {
							Method detachMethod = vmClass.getMethod("detach");
							detachMethod.invoke(vm);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		catch (Exception e) {
			//
		}
		finally {
			Thread.currentThread().setContextClassLoader(cl);
		}

		return null;
	}

	private static ClassLoader getToolsClassLoader(ClassLoader parent) throws IOException {
		if (toolsClassloader == null) {
			File toolsJar = findJdkJar("tools.jar");

			if (toolsJar != null && toolsJar.exists()) {
				URL toolsUrl = null;

				try {
					toolsUrl = toolsJar.toURI().toURL();
				}
				catch (MalformedURLException e) {}

				if (toolsClassloader == null) {
					URL[] urls = new URL[] {
						toolsUrl
					};
					toolsClassloader = new URLClassLoader(urls, parent);
				}
			}
		}

		return toolsClassloader;
	}

	private static File findJdkJar(String jar) throws IOException {
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
