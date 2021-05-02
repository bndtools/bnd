package aQute.bnd.build;

import static aQute.bnd.result.Result.err;
import static aQute.bnd.result.Result.ok;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.osgi.framework.VersionRange;

import aQute.bnd.help.instructions.ProjectInstructions.GeneratorSpec;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.result.Result;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;
import aQute.lib.redirect.Redirect;
import aQute.lib.specinterface.SpecInterface;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter.SetLocation;

public class ProjectGenerate implements AutoCloseable {

	final Project project;

	ProjectGenerate(Project project) {
		this.project = project;
	}

	public Result<Set<File>, String> generate(boolean force) {
		project.clear();
		if (force)
			clean();


		Set<File> files = new TreeSet<>();
		for (Entry<String, GeneratorSpec> e : project.instructions.generate()
			.entrySet()) {

			Result<Set<File>, String> step = step(e.getKey(), e.getValue());
			if (step.isErr()) {
				String qsrc = e.getKey();
				SetLocation error = project.error("%s : %s;%s", step.error()
					.get(), e.getKey(),
					e.getValue()
						._attrs());
				error.context(qsrc);
				error.header(Constants.GENERATE);
				return step.asError();
			}
			files.addAll(step.unwrap());
		}
		return Result.ok(files);
	}

	private Result<Set<File>, String> step(String sourceWithDuplicate, GeneratorSpec st) {
		try {

			String source = Strings.trim(Processor.removeDuplicateMarker(sourceWithDuplicate));
			if (source.isEmpty() || source.equals(Constants.EMPTY_HEADER))
				return Result.ok(Collections.emptySet());

			String output = st.output();
			if (output == null)
				return err("No mandatory 'output' files/directories specified");

			if (!output.endsWith("/"))
				output += "/";

			Set<File> sourceFiles = new FileSet(project.getBase(), source).getFiles();
			if (sourceFiles.isEmpty())
				return err("No source files/directories specified");

			File out = project.getFile(output);
			if (out.isDirectory()) {
				for (File f : out.listFiles()) {
					IO.delete(f);
				}
			} else {
				out.mkdirs();
			}

			if (st.system()
				.isPresent())
				project.system(st.system()
					.get(), null);

			if (st.generate()
				.isPresent()) {

				boolean ignoreErrors = false;
				String plugin = st.generate()
					.get();
				if (plugin.startsWith("-")) {
					ignoreErrors = true;
					plugin = plugin.substring(1);
				}

				String result = doGenerate(plugin, st);

				if (result != null)
					return err(ignoreErrors ? "-" + result : result);
			}

			Set<File> affected = new FileSet(project.getBase(), st.output()).getFiles();
			if (affected.isEmpty()) {
				return err(
					"ran command or generate but no files were changed; Is `output` correct wrt to the command?");
			}
			return ok(affected);
		} catch (Exception e) {
			return err(Exceptions.causes(e));
		}
	}

	private String doGenerate(String commandline, GeneratorSpec st) {
		try {
			String result = null;

			List<String> blocks = Strings.splitQuoted(commandline, ";");
			for (String block : blocks) {
				if (block.isEmpty())
					continue;

				List<String> arguments = Strings.splitQuoted(block, " \t");
				if (arguments.isEmpty()) {
					return block + " : no command name";
				}

				File fstdin = null;
				File fstdout = null;
				File fstderr = null;

				String pluginName = arguments.remove(0);

				for (Iterator<String> it = arguments.iterator(); it.hasNext();) {
					String arg = it.next();

					if (arg.startsWith("<")) {
						if (fstdin != null)
							return "only 1 redirection of stdin supported";

						fstdin = getFile(arg.substring(1), false);
						if (fstdin == null || !fstdin.isFile())
							return "cannot find redirected input file " + fstdin;
						it.remove();
					} else if (arg.startsWith(">")) {
						if (fstdout != null)
							return "only 1 redirection of stdout supported";
						fstdout = getFile(arg.substring(1), true);
						it.remove();
					} else if (arg.startsWith("1>")) {
						if (fstdout != null)
							return "only 1 redirection of stdout supported";
						fstdout = getFile(arg.substring(2), true);
						it.remove();
					} else if (arg.startsWith("2>")) {
						if (fstderr != null)
							return "only 1 redirection of stderr supported";
						fstderr = getFile(arg.substring(2), true);
						it.remove();
					}
				}

				InputStream stdin = fstdin == null ? null : IO.stream(fstdin);
				OutputStream stdout = fstdout == null ? null : IO.outputStream(fstdout);
				OutputStream stderr = fstderr == null ? null : IO.outputStream(fstderr);
				try {
					if (pluginName.indexOf('.') >= 0) {
						if (pluginName.startsWith("."))
							pluginName = pluginName.substring(1);

						VersionRange range = st.version()
							.map(VersionRange::valueOf)
							.orElse(null);

						result = doGenerateMain(pluginName, range, st._attrs(), arguments, stdin, stdout, stderr);
					} else {
						result = doGeneratePlugin(pluginName, st._attrs(), arguments, stdin, stdout, stderr);
					}
					if (result != null)
						return block + " : " + result;
				} finally {
					IO.closeAll(stdout, stderr, stdin);
				}
			}
			return result;
		} catch (Exception e) {
			return Exceptions.causes(e);
		}
	}

	private File getFile(String path, boolean mkdirs) {
		File f = project.getFile(path);
		if (mkdirs)
			f.getParentFile()
				.mkdirs();
		return f;
	}

	private String doGeneratePlugin(String pluginName, Map<String, String> attrs, List<String> arguments,
		InputStream stdin, OutputStream stdout, OutputStream stderr) {
		BuildContext bc = new BuildContext(project, attrs, arguments, stdin, stdout, stderr);

		Result<Boolean, String> call = project.getWorkspace()
			.getExternalPlugins()
			.call(pluginName, Generator.class, p -> {

				Class<?> type = SpecInterface.getParameterizedInterfaceType(p.getClass(), Generator.class);
				if (type == null) {
					return err("Cannot find the options type in %s", p.getClass());
				}
				SpecInterface<?> spec = SpecInterface.getOptions(type, arguments, bc.getBase());
				if (spec.isFailure()) {
					return err(spec.failure());
				}

				Redirect redirect = new Redirect(stdin, stdout, stderr).captureStdout()
					.captureStderr();

				@SuppressWarnings("unchecked")
				Optional<String> error = redirect.apply(() -> p.generate(bc, spec.instance()));

				if (error.isPresent())
					return err(error.get() + " : " + redirect.getContent());

				return ok(true);
			});
		return call.error()
			.orElse(null);
	}

	private String doGenerateMain(String mainClass, VersionRange range, Map<String, String> attrs,
		List<String> arguments, InputStream stdin, OutputStream stdout, OutputStream stderr) {
		Result<Integer, String> call = project.getWorkspace()
			.getExternalPlugins()
			.call(mainClass, range, project, attrs, arguments, stdin, stdout, stderr);
		if (call.isErr())
			return call.error()
				.get();

		if (call.unwrap() != 0) {
			return "process returned with non-zero exit code: " + call.unwrap();
		}
		return null;
	}

	public Result<Set<File>, String> getInputs() {

		Set<String> inputs = project.instructions.generate()
			.keySet();

		if (inputs.isEmpty())
			return Result.ok(Collections.emptySet());

		Set<File> files = new HashSet<>();
		String errors = "";
		for (String input : inputs) {
			Set<File> inputFiles = new FileSet(project.getBase(), input).getFiles();
			if (inputFiles.isEmpty()) {
				project.error("-generate: no content for %s", input);

				errors = errors.concat(input)
					.concat(" has no matching files\n");
			} else {
				files.addAll(inputFiles);
			}
		}
		if (errors.isEmpty())
			return Result.ok(files);
		else
			return Result.err(errors);
	}

	public Set<File> getOutputDirs() {
		return project.instructions.generate()
			.values()
			.stream()
			.map(GeneratorSpec::output)
			.filter(Objects::nonNull)
			.map(project::getFile)
			.collect(Collectors.toSet());
	}

	public boolean needsBuild() {
		for (Entry<String, GeneratorSpec> e : project.instructions.generate()
			.entrySet()) {

			GeneratorSpec spec = e.getValue();

			String source = Strings.trim(Processor.removeDuplicateMarker(e.getKey()));
			if (source.isEmpty() || source.equals(Constants.EMPTY_HEADER))
				continue;

			String output = spec.output();
			if (output == null)
				return true; // error handling

			if (!output.endsWith("/"))
				output += "/";

			Set<File> sourceFiles = new FileSet(project.getBase(), source).getFiles();
			if (sourceFiles.isEmpty())
				return true; // error handling

			sourceFiles.addAll(project.getSelfAndAncestors());

			long latestModifiedSource = sourceFiles.stream()
				.mapToLong(File::lastModified)
				.max()
				.getAsLong();

			Set<File> outputFiles = new FileSet(project.getBase(), output).getFiles();
			if (outputFiles.isEmpty())
				return true;

			long latestModifiedTarget = outputFiles.stream()
				.filter(File::isFile)
				.mapToLong(File::lastModified)
				.min()
				.orElse(0);

			boolean staleFiles = latestModifiedSource > latestModifiedTarget;
			if (staleFiles)
				return true;
		}
		return false;
	}

	public void clean() {
		getOutputDirs().stream()
			.forEach(IO::delete);
	}

	@Override
	public void close() {}

}
