package aQute.bnd.build;

import java.io.*;
import java.util.*;

public class Run extends Project {

	public Run(Workspace workspace, File projectDir, File propertiesFile ) throws Exception {
		super(workspace, projectDir, propertiesFile);
	}

	public void report(Map<String,Object> table) throws Exception {
		super.report(table, false);
	}

	public String toString() {
		return getPropertiesFile().getName();
	}
}
