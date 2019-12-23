package aQute.launchpad.junit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.osgi.framework.Bundle;

import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Probe;
import aQute.launchpad.internal.ProbeImpl;
import aQute.lib.io.IO;

public class LaunchpadRunner extends BlockJUnit4ClassRunner {
	static byte[]			testBundle;

	final LaunchpadBuilder	builder;

	public LaunchpadRunner(Class<?> klass) throws InitializationError {
		super(klass);
		try {
			Field field = klass.getDeclaredField("builder");
			if (!Modifier.isStatic(field.getModifiers())) {
				throw new InitializationError("");
			}
			field.setAccessible(true);
			Object value = field.get(null);
			if (value instanceof LaunchpadBuilder) {
				this.builder = (LaunchpadBuilder) value;
			} else {
				throw new IllegalArgumentException("The builder field must be a LaunchpadBuilder");
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new InitializationError("No field 'builder' in class " + klass);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Statement methodBlock(FrameworkMethod method) {
		try {
			if (testBundle == null) {
				testBundle = getTestBundle();
			}

			Launchpad launchpad = builder.notestbundle()
				.export("aQute.launchpad.junit")
				.export("aQute.launchpad")
				.export("org.junit.*")
				.export("org.junit")
				.create(method.getName(), getName());

			Bundle tb = launchpad.getFramework()
				.getBundleContext()
				.installBundle(method.getName() + "-" + getName(), new ByteArrayInputStream(testBundle));

			debug(tb);
			launchpad.setProxyBundle(tb);

			doProbeBundle(launchpad);

			if (builder.isDebug()) {
				launchpad.report();
			}

			Class<?> actualClassInBundle;
			try {
				actualClassInBundle = tb.loadClass(getTestClass().getJavaClass()
					.getName());
			} catch (ClassNotFoundException e) {
				int state = tb.getState();
				if (state == Bundle.INSTALLED) {
					try {
						tb.start();
					} catch (Exception ee) {
						return new Fail(ee);
					}
				}
				return new Fail(e);
			}

			FrameworkMethod actualFrameworkMethod = getFrameworkMethod(actualClassInBundle, method);

			Object actualInstance = actualClassInBundle.newInstance();

			TestClass actualClass = createTestClass(actualClassInBundle);

			Statement statement = methodInvoker(actualFrameworkMethod, actualInstance);
			statement = possiblyExpectingExceptions(actualFrameworkMethod, actualInstance, statement);
			statement = withPotentialTimeout(actualFrameworkMethod, actualInstance, statement);

			List<FrameworkMethod> befores = actualClass.getAnnotatedMethods(Before.class);
			statement = befores.isEmpty() ? statement : new RunBefores(statement, befores, actualInstance);

			List<FrameworkMethod> afters = actualClass.getAnnotatedMethods(After.class);
			statement = afters.isEmpty() ? statement : new RunAfters(statement, afters, actualInstance);

			Statement inner = statement;

			Statement s = new Statement() {
				@Override
				public void evaluate() throws Throwable {
					tb.start();
					launchpad.inject(actualInstance);
					try {
						inner.evaluate();
					} finally {
						launchpad.close();
					}
				}
			};

			return new Statement() {

				@Override
				public void evaluate() throws Throwable {
					s.evaluate();
				}
			};
		} catch (Throwable e) {
			e.printStackTrace();
			return new Fail(e);
		}
	}

	private FrameworkMethod getFrameworkMethod(Class<?> actualClassInBundle, FrameworkMethod originalFrameworkMethod)
		throws NoSuchMethodException, SecurityException {
		Method actualMethod = actualClassInBundle.getMethod(originalFrameworkMethod.getName());
		actualMethod.setAccessible(true);
		return new FrameworkMethod(actualMethod);
	}

	public void doProbeBundle(Launchpad launchpad) {
		try {
			URL resource = Launchpad.class.getResource("/probe.jar");
			Bundle probeBundle = launchpad.getFramework()
				.getBundleContext()
				.installBundle(resource.toString(), resource.openStream());

			@SuppressWarnings("unchecked")
			Class<ProbeImpl> c = (Class<ProbeImpl>) probeBundle.loadClass(ProbeImpl.class.getName());
			Probe probe = c.getConstructor()
				.newInstance();
			launchpad.setProbe(probe);
		} catch (Exception e) {
			// ignore
			e.printStackTrace();
		}
	}

	private void debug(Bundle tb) throws IOException {
		if (builder.isDebug()) {
			Enumeration<URL> entryPaths = tb.findEntries("/", "*", true);
			while (entryPaths.hasMoreElements())
				System.err.println(entryPaths.nextElement()
					.getPath());
			URL entry = tb.getEntry("META-INF/MANIFEST.MF");
			Manifest mf = new Manifest(entry.openStream());
			mf.write(System.err);
		}
	}

	public byte[] getTestBundle() {
		RunSpecification local = builder.getLocal();

		BuilderSpecification bs = new BuilderSpecification();
		bs.testBundle = "*";
		bs.includeresource.put(local.bin_test, Collections.emptyMap());
		return builder.build(IO.work.getPath(), bs);
	}
}
