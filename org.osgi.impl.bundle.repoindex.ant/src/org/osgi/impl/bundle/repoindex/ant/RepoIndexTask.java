package org.osgi.impl.bundle.repoindex.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.osgi.framework.launch.Framework;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.util.tracker.ServiceTracker;

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;

public class RepoIndexTask extends Task {

	private final List<FileSet> fileSets = new LinkedList<FileSet>();
	private final Map<String, String> config = new HashMap<String, String>();
	
	private File repositoryFile = null;
	
	public void setName(String name) {
		config.put(ResourceIndexer.REPOSITORY_NAME, name);
	}
	
	public void setVerbose(boolean verbose) {
		config.put(ResourceIndexer.VERBOSE, Boolean.toString(verbose));
	}
	
	public void setPretty(boolean pretty) {
		config.put(ResourceIndexer.PRETTY, Boolean.toString(pretty));
	}
	
	public void setRootURL(String root) {
		config.put(ResourceIndexer.ROOT_URL, root);
	}

	public void setOut(String outFile) {
		this.repositoryFile = new File(outFile);
	}

	public void addFileset(FileSet fs) {
		fileSets.add(fs);
	}
	
	@Override
	public void execute() throws BuildException {
		printCopyright(System.err);
		
		if (repositoryFile == null)
			throw new BuildException("Output file not specified");
		
		try {
			// Configure PojoSR
			Map<String, Object> pojoSrConfig = new HashMap<String, Object>();
			pojoSrConfig.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, new ClasspathScanner());
			
			// Start PojoSR 'framework'
			Framework framework = new PojoServiceRegistryFactoryImpl().newFramework(pojoSrConfig);
			framework.init();
			framework.start();
			
			// Look for indexer and run index generation
			ServiceTracker tracker = new ServiceTracker(framework.getBundleContext(), ResourceIndexer.class.getName(), null);
			tracker.open();
			ResourceIndexer index = (ResourceIndexer) tracker.waitForService(1000);
			if (index == null)
				throw new IllegalStateException("Timed out waiting for ResourceIndexer service.");

			// Flatten the file sets into a single list
			Set<File> fileList = new LinkedHashSet<File>();
			for (FileSet fileSet : fileSets) {
				DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
				File basedir = ds.getBasedir();
				String[] files = ds.getIncludedFiles();
				for (int i = 0; i < files.length; i++)
					fileList.add(new File(basedir, files[i]));
			}

			// Run
			index.index(fileList, new FileOutputStream(repositoryFile), config);
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
	
	public static void printCopyright(PrintStream out) {
		out.println("Bindex2 | Resource Indexer v1.0");
		out.println("(c) 2012 OSGi, All Rights Reserved");
	}

}
