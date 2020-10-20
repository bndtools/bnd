package aQute.bnd.ant;

import java.io.File;
import java.io.IOException;
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
					for (Object object : flattened.keySet()) {
						String key = (String) object;
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
