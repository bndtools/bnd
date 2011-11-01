
package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import aQute.bnd.main.*;
import aQute.libg.qtokens.*;
import aQute.libg.reporter.*;

public class WrapTask extends BaseTask implements Reporter {
	List<File>	jars		= new ArrayList<File>();
	File	output		= null;
	File	definitions = null;
	List<File>	classpath	= new ArrayList<File>();
	
	boolean	failok;
	boolean	exceptions;
	boolean	print;

	@SuppressWarnings("unchecked")
    public void execute() throws BuildException {
		boolean failed = false;
		
		try {
			if (jars == null)
				throw new BuildException("No files set");

			if (output == null)
				output = getProject().getBaseDir();

			if (definitions == null)
				definitions = getProject().getBaseDir();
			
			for (Iterator<File> f = jars.iterator(); f.hasNext();) {
				bnd bnd = new bnd();
				bnd.setPedantic(isPedantic());
				File file = f.next();
				String name = file.getName();
				name = name.replaceFirst("(\\.jar)?$", ".bnd");
				File bndFile = new File(definitions, name );
				bnd.doWrap(bndFile.exists()?bndFile:null, file, output,  classpath.toArray(new File[0]), 0, getProject().getProperties());
				failed |= report(bnd);
			}
		} catch (Exception e) {
			if (exceptions)
				e.printStackTrace();
			if (!failok)
				throw new BuildException("Failed to build jar file: " + e, e);
		}
		if ( failed && !failok)
			throw new BuildException("Failed to wrap jar file");
	}

	public void setJars(String files) {
		addAll(this.jars, files, ",");
	}

	void addAll(List<File> list, String files, String separator) {
		QuotedTokenizer qt = new QuotedTokenizer(files, separator);
		String entries[] = qt.getTokens();
		File project = getProject().getBaseDir();
		for (int i = 0; i < entries.length; i++) {
			File f = getFile(project, entries[i]);
			if (f.exists())
				list.add(f);
			else
				error("Can not find bnd file to process: "
						+ f.getAbsolutePath());
		}
	}

	public void setClasspath(String files) {
		addAll(classpath, files, File.pathSeparator+",");
	}

	boolean isFailok() {
		return failok;
	}

	public void setFailok(boolean failok) {
		this.failok = failok;
	}

	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}


	public void setOutput(File output) {
		this.output = output;
	}
	
	public void setDefinitions(File out) {
		definitions = out;
	}
	
	public void addConfiguredFileSet(FileSet list) {
		DirectoryScanner scanner = list.getDirectoryScanner(getProject());
		String files[] = scanner.getIncludedFiles();
		for (int i = 0; i < files.length; i++) {
			File f= getFile(scanner.getBasedir(), files[i]);
			this.jars.add(f);
		}
	}	
}
