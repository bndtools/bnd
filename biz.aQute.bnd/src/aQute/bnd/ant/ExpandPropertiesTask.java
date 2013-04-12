package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.osgi.*;

public class ExpandPropertiesTask extends BaseTask {
	File	propertyFile;

	@Override
	@SuppressWarnings("cast")
	public void execute() throws BuildException {
		try {
			if (propertyFile.exists()) {
				Properties properties = new Properties();
				properties.putAll((Map< ? , ? >) getProject().getProperties());

				Processor processor = new Processor(properties);
				try {
					processor.setProperties(propertyFile);

					Project project = getProject();
					Properties flattened = processor.getFlattenedProperties();
					for (Iterator<Object> i = flattened.keySet().iterator(); i.hasNext();) {
						String key = (String) i.next();
						if (project.getProperty(key) == null) {
							project.setProperty(key, flattened.getProperty(key));
						}
					}
				}
				finally {
					processor.close();
				}
			}
			report();
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	public void setPropertyFile(File file) {
		this.propertyFile = file;
	}
}
