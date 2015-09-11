package org.osgi.impl.bundle.repoindex.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.service.indexer.*;
import org.osgi.service.indexer.impl.*;
import org.osgi.util.tracker.*;

import de.kalpatec.pojosr.framework.*;
import de.kalpatec.pojosr.framework.launch.*;

@SuppressWarnings("restriction")
public class RepoIndexTask extends Task {

	private final List<FileSet>			fileSets	= new LinkedList<FileSet>();
	private final Map<String,String>	config		= new HashMap<String,String>();

	private File	repositoryFile		= null;
	private boolean	knownBundles;
	private boolean	builtInknownBundles	= true;
	private String	additionalKnownBundles;

	public void setName(String name) {
		config.put(ResourceIndexer.REPOSITORY_NAME, name);
	}

	public void setVerbose(boolean verbose) {
		config.put(ResourceIndexer.VERBOSE, Boolean.toString(verbose));
	}

	public void setPretty(boolean pretty) {
		config.put(ResourceIndexer.PRETTY, Boolean.toString(pretty));
	}

	public void setCompressed(boolean compressed) {
		config.put(ResourceIndexer.COMPRESSED, Boolean.toString(compressed));
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

	public void setKnownBundles(boolean knownBundles) {
		this.knownBundles = knownBundles;
	}

	public void setBuiltInKnownBundles(boolean builtInknownBundles) {
		this.builtInknownBundles = builtInknownBundles;
	}

	public void setAdditionalKnownBundles(String additionalKnownBundles) {
		this.additionalKnownBundles = additionalKnownBundles;
	}

	@Override
	public void execute() throws BuildException {
		printCopyright(System.err);

		if (repositoryFile == null)
			throw new BuildException("Output file not specified");

		FileOutputStream fos = null;
		try {
			// Configure PojoSR
			Map<String,Object> pojoSrConfig = new HashMap<String,Object>();
			pojoSrConfig.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, new ClasspathScanner());

			// Start PojoSR 'framework'
			Framework framework = new PojoServiceRegistryFactoryImpl().newFramework(pojoSrConfig);
			framework.init();
			framework.start();

			if (knownBundles) {
				registerKnownBundles(framework.getBundleContext());
			}

			// Look for indexer and run index generation
			ServiceTracker<ResourceIndexer,ResourceIndexer> tracker = new ServiceTracker<ResourceIndexer,ResourceIndexer>(
					framework.getBundleContext(), ResourceIndexer.class, null);
			tracker.open();
			ResourceIndexer index = tracker.waitForService(1000);
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
			fos = new FileOutputStream(repositoryFile);
			index.index(fileList, fos, config);
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
		finally {
			if (fos != null) {
				try {
					fos.close();
				}
				catch (IOException e) {
					/* swallow */
				}
				fos = null;
			}
		}
	}

	private void registerKnownBundles(BundleContext bundleContext) {
		KnownBundleAnalyzer kba = builtInknownBundles ? new KnownBundleAnalyzer()
				: new KnownBundleAnalyzer(new Properties());

		if (additionalKnownBundles != null) {
			File extras = new File(additionalKnownBundles);
			if (extras.exists()) {
				Properties props = new Properties();
				try {
					props.load(new FileReader(extras));
					kba.setKnownBundlesExtra(props);
				}
				catch (IOException e) {
					throw new BuildException("Unable to load the additional known bundles " + additionalKnownBundles,
							e);
				}
			} else {
				throw new BuildException(
						"The additional known bundles file " + additionalKnownBundles + " does not exist.");
			}
		}

		bundleContext.registerService(ResourceAnalyzer.class, kba, null);
	}

	public static void printCopyright(PrintStream out) {
		out.println("Bindex2 | Resource Indexer v1.0");
		out.println("(c) 2012 OSGi, All Rights Reserved");
	}

}
