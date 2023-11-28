package biz.aQute.bnd.proxy.generator;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;
import aQute.lib.io.IO;

/**
 * A -generate command to generate a Facade class. The class can create an
 * instance of an object that optionally extends a class and implements 0 or
 * more interfaces. Instance can bind to a controlling object when created.
 * There is a public interface Delegate created that contains all the
 * overridable methods in the extended class (if any) and which extends all the
 * given interfaces. I.e. it declares all methods that need to be delegated. A
 * second class is created that extends the given class (if any) and implements
 * the interfaces. All overrideable methods are delegating to a bind object.
 * <p>
 * A constructor is provided to let the binding happen.
 * <p>
 * It will create a class that extends a parameterizable base class
 * (-e/--extends). The package for the output class is specified with
 * -p/--package_.
 * <p>
 * The arguments specify a target class. Each class gets parameterized by a
 * definition which consists of a number of ':' separated names.
 * <p>
 * If only a single name is specified, it is assumed to be a the class to build
 * a front for. The output name will be the simple name of the that class,
 * appended with 'Facade' and it will be placed in the -p specified package.
 * <p>
 * If more than one name is specified, the first name is the output class. It
 * can be a simple name, it will then be placed then in the given package as
 * specified with -p.
 * <p>
 * The second name may be an interface or a class name. If the name is a class
 * name, the Facade will extend it, otherwise it will implement the interface.
 * Subsequent names must be valid interface names and will be implemented.
 * <p>
 * The class will hold a Delegate interface. This interface is supposed to be
 * implemented by the delegatee, e.g. a service. It specifies _all_ methods in
 * the primaty class/interface as well as the additional interfaces. Methods
 * from a primary class will be declared in this interface. The other methods
 * are inherited.
 * <p>
 * The generated class will provide a createFacade method that returns an
 * instance of the Facade class. This facade class extends the primary class (if
 * present) and implements all interfaces.
 * <p>
 */
@ExternalPlugin(name = "facadegen", objectClass = Generator.class)
public class FacadeGenerator extends Processor implements Generator<FacadeSourceGenOptions> {

	@Override
	public Optional<String> generate(BuildContext context, FacadeSourceGenOptions options) throws Exception {
		try {
			work(context, options);
		} catch (Exception e) {
			exception(e, "failed to generate facade %s", e);
		}

		if (isOk())
			return Optional.empty();
		StringBuilder sb = new StringBuilder();
		report(sb);
		return Optional.of(sb.toString());
	}

	private void work(BuildContext context, FacadeSourceGenOptions options) throws Exception, IOException {

		File output = options.output()
			.orElse(context.getFile("gen-src"));
		output.mkdirs();

		if (!output.isDirectory()) {
			error("no output directory %s", output);

		}

		String extend = options.extend();
		if (extend == null) {
			error("no -e/--extend specified");
			return;
		}
		String pack = options.package_();
		if (pack == null) {
			error("no -p/--package_ specified");
			return;
		}
		System.out.println("generating " + options._arguments());

		Project project = context.getProject();
		try (ProjectBuilder pb = project.getBuilder(null)) {
			for (String arg : options._arguments()) {
				Spec spec = new Spec(pb, arg, pack, extend);
				String source = spec.source();
				String path = spec.facade.getSourcePath();
				File file = IO.getFile(output, path);
				file.getParentFile()
					.mkdirs();
				IO.store(source, file);
				System.out.println(file);
				System.out.println(source);
			}
			getInfo(pb);
		}
	}

}
