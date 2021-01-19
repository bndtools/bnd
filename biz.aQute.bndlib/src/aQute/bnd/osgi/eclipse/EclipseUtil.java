package aQute.bnd.osgi.eclipse;

import java.io.File;
import java.net.URI;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

public class EclipseUtil {

	/**
	 * Get the .classpath file for a project
	 *
	 * <pre>
	 * <?xml version="1.0" encoding="UTF-8"?>
	<classpath>
	<classpathentry kind="src" output="bin" path="src"/>
				<classpathentry kind="src" path="gen-sources">
				<attributes>
					<attribute name="ignore_optional_problems" value="true"/>
				</attributes>
			</classpathentry>
	<classpathentry kind="con" path=
	"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"/>
	<classpathentry kind="con" path="aQute.bnd.classpath.container"/>
		<classpathentry kind="src" output="bin_test" path="test">
		<attributes>
			<attribute name="test" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="output" path="bin"/>
	</classpath>
	 * </pre>
	 *
	 * @param model
	 * @throws Exception
	 */
	public static String getClasspath(Project model) throws Exception {
		Tag classpath = new Tag("classpath");

		String target = model.getProperty(Constants.JAVAC_TARGET, "1.8");
		Tag vm = new Tag(classpath, "classpathentry");
		vm.addAttribute("kind", "con");
		vm.addAttribute("path",
			"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-"
				+ target);

		Tag bnd = new Tag(classpath, "classpathentry");
		bnd.addAttribute("kind", "con");
		bnd.addAttribute("path", "aQute.bnd.classpath.container");

		for (File s : model.getSourcePath()) {
			Tag srctag = new Tag(classpath, "classpathentry");
			srctag.addAttribute("kind", "src");
			srctag.addAttribute("output", relative(model, model.getOutput()));
			srctag.addAttribute("path", relative(model, s));

			if (s.getName()
				.contains("gen")) {
				Tag attributes = new Tag(srctag, "attributes");
				Tag attribute = new Tag(attributes, "attribute");
				attribute.addAttribute("name", "ignore_optional_problems");
				attribute.addAttribute("value", "true");
			}
			IO.mkdirs(s);
		}

		Tag tsttag = new Tag(classpath, "classpathentry");
		tsttag.addAttribute("kind", "src");
		tsttag.addAttribute("output", relative(model, model.getTestOutput()));
		tsttag.addAttribute("path", relative(model, model.getTestSrc()));
		Tag attributes = new Tag(tsttag, "attributes");
		Tag attribute = new Tag(attributes, "attribute");
		attribute.addAttribute("name", "test");
		attribute.addAttribute("value", "true");
		IO.mkdirs(model.getTestSrc());

		return classpath.toString();
	}

	/**
	 * Create a classpath for the project, overwriting an existing classpath
	 *
	 * @param model the project
	 */
	public static void createClasspath(Project model) throws Exception {
		String classpath = getClasspath(model);
		File classpathFile = model.getFile(".classpath");
		String a = IO.collect(classpathFile);
		String b = classpath.toString();
		if (!a.equals(b)) {
			IO.store(classpath.toString(), classpathFile);
		}
	}

	private static String relative(Project model, File output) {
		URI target = output.toURI();
		URI base = model.getBase()
			.toURI();
		return base.relativize(target)
			.toString();
	}
}
