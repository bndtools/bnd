package aQute.bnd.ant;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import aQute.bnd.osgi.Processor;
import aQute.lib.utf8properties.UTF8Properties;

public class ExpandPropertiesTask extends BaseTask {
	File propertyFile;

	@Override
	@SuppressWarnings("unchecked")
	public void execute() throws BuildException {
		try {
			if (propertyFile.exists()) {
				Properties properties = new UTF8Properties();
				properties.putAll(getProject().getProperties());

				try (Processor processor = new Processor(properties)) {
					processor.setProperties(propertyFile);

					Project project = getProject();
					Properties flattened = processor.getFlattenedProperties();
					for (Iterator<Object> i = flattened.keySet()
						.iterator(); i.hasNext();) {
						String key = (String) i.next();
						if (project.getProperty(key) == null) {
							project.setProperty(key, flattened.getProperty(key));
						}
					}
				}
			}
			report();
		} catch (IOException e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	public void setPropertyFile(File file) {
		this.propertyFile = file;
	}
}
