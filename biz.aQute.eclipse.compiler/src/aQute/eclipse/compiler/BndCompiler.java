package aQute.eclipse.compiler;

import java.io.*;
import java.util.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.build.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;

@Component public class BndCompiler implements aQute.bnd.service.Compiler {

	@Override public boolean compile(final Project project, Collection<File> sources,
			Collection<Container> buildpath, File bin) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("-classpath");
		String del = " ";
		for (Container c : buildpath) {
			sb.append(del);
			sb.append(c.getFile().getAbsolutePath());
			del = File.pathSeparator;
		}

		sb.append(" -sourcepath");
		del = " ";

		for (File f : sources) {
			sb.append(del);
			sb.append(f.getAbsolutePath());
			del = File.pathSeparator;
		}

		sb.append(" -source ");
		sb.append(project.getProperty(Constants.COMPILER_SOURCE, "1.6"));
		sb.append(" -target ");
		sb.append(project.getProperty(Constants.COMPILER_TARGET, "1.6"));

		sb.append(" -d ");
		sb.append(project.getOutput().getAbsolutePath());
		sb.append(" -g ");

		for (File dir : project.getSourcePath()) {
			List<File> l = Create.list();
			expand(l, dir, bin);
			if ( l.size() == 0 ) {
				project.trace("All files are up to date");
			}
			for (File source : l) {
				sb.append(" ");
				String path = source.getAbsolutePath();
				sb.append(path);
			}
		}
		StringWriter errors = new StringWriter();
		StringWriter warnings = new StringWriter();
		project.trace("Compile cmd: %s", sb);

//		CompilationProgress cp = new CompilationProgress() {
//
//			@Override public void worked(int workIncrement, int remainingWork) {
//				project.trace("compiler worked: %d/%d", workIncrement,
//						remainingWork);
//			}
//
//			@Override public void setTaskName(String name) {
//				project.trace("compiler task: %s", name);
//			}
//
//			@Override public boolean isCanceled() {
//				return false;
//			}
//
//			@Override public void done() {
//				project.trace("compiler done");
//			}
//
//			@Override public void begin(int remainingWork) {
//				project.trace("compiler begin");
//			}
//		};
//		BatchCompiler.compile(sb.toString(), new PrintWriter(errors),
//				new PrintWriter(warnings), cp);
		project.getErrors().addAll(Processor.split(errors.toString(), "\n"));
		project.getWarnings().addAll(Processor.split(warnings.toString(), "\n"));
		return true;
	}

	private void expand(List<File> l, File sources, File binaries) {
		for (File sourceSub : sources.listFiles()) {
			if (sourceSub.isDirectory()) {
				File binarySub = new File(binaries, sourceSub.getName());
				expand(l, sourceSub, binarySub);
			} else if (sourceSub.isFile()) {
				if (sourceSub.getName().endsWith(".java") ) {
					String name = sourceSub.getName();
					File binarySub = new File(binaries, name.substring(0, name
							.length() - 5)
							+ ".class");
					System.out.println("source " + sourceSub + " " + sourceSub.lastModified() + " " + binarySub + " " + binarySub.lastModified());
					if (sourceSub.lastModified() > binarySub.lastModified()) 
					l.add(sourceSub);
				}
			}
		}
	}

}
