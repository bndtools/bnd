package bndtools.core.test.utils;

import static bndtools.core.test.utils.TaskUtils.log;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.osgi.test.common.service.ServiceConfiguration;
import org.osgi.test.junit5.service.ServiceExtension;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;

/**
 * Jupiter extension for setting up a test workspace. This extension does the
 * following:
 * <ol>
 * <li>Waits for the workbench initialization to complete</li>
 * <li>Clears the current workspace</li>
 * <li>Imports the template workspace (if specified by {@code @Workbench}) to
 * the current workspace</li>
 * </ol>
 *
 * @see WorkbenchTest
 */
public class WorkbenchExtension implements BeforeAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		TaskUtils.setAutobuild(false);

		// Wait for the Bnd Workspace service to be available before proceeding.
		ServiceConfiguration<Workspace> sc = ServiceExtension.getServiceConfiguration(Workspace.class, "",
			new String[] {}, 1, 10000, context);

		WorkbenchTest test = context.getRequiredTestClass()
			.getAnnotation(WorkbenchTest.class);

		String resourcePath = (test != null && !"".equals(test.value())) ? test.value()
			: classToPath(context.getRequiredTestClass());

		String workspacesRoot = System.getProperty("bndtools.core.test.dir");
		if (workspacesRoot == null) {
			throw new IllegalArgumentException("bndtools.core.test.dir not set");
		}
		Path srcRoot = Paths.get(workspacesRoot)
			.resolve("resources/workspaces");
		Path ourRoot = srcRoot.resolve(resourcePath);
		log("Using template workspace: " + ourRoot);

		WorkspaceImporter importer = new WorkspaceImporter(ourRoot);

		Stream.of(context.getRequiredTestClass()
			.getDeclaredFields())
			.filter(f -> Modifier.isStatic(f.getModifiers()))
			.filter(f -> f.getType()
				.equals(WorkspaceImporter.class))
			.forEach(field -> {
				try {
					field.setAccessible(true);
					field.set(null, importer);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});

		List<Path> sourceProjects = Files.walk(ourRoot, 1)
			.filter(x -> !x.equals(ourRoot))
			.collect(Collectors.toList());

		log("importing: " + sourceProjects.size() + " projects");
		WorkspaceImporter.importAllProjects(sourceProjects.stream());
		log("done importing");
	}

	private static String classToPath(Class<?> requiredTestClass) {
		return requiredTestClass.getPackage()
			.getName()
			.replace("bndtools.core.test.", "")
			.replace('.', '/');
	}
}
