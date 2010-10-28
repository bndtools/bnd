package test;

import java.io.*;
import java.util.concurrent.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.lib.osgi.*;

public class LauncherTest extends TestCase {

	public void testSimple() throws Exception{
		Project project = Workspace.getProject(Processor.getFile(new File("").getAbsoluteFile().getParentFile(), "demo"));
		project.clear();
		
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "exit");
		assertEquals(42,l.launch());
	}
	
	public void testTester() throws Exception {
		Project project = Workspace.getProject(Processor.getFile(new File("").getAbsoluteFile().getParentFile(), "demo"));
		project.clear();
		project.build();
		
		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
				
		assertEquals(2,pt.test());
	}
	
	
	
	
	
	public void testTimeoutActivator() throws Exception {
		Project project = Workspace.getProject(Processor.getFile(new File("").getAbsoluteFile().getParentFile(), "demo"));
		project.clear();
		
		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(100, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		assertEquals(ProjectLauncher.TIMEDOUT,l.launch());
		
	}
	
	public void testTimeout() throws Exception {
		Project project = Workspace.getProject(Processor.getFile(new File("").getAbsoluteFile().getParentFile(), "demo"));
		project.clear();
		
		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(100, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		l.getRunProperties().put("test.cmd", "timeout");
		assertEquals(ProjectLauncher.TIMEDOUT,l.launch());
	}
	
	public void testMainThread() throws Exception {
		Project project = Workspace.getProject(Processor.getFile(new File("").getAbsoluteFile().getParentFile(), "demo"));
		project.clear();
		
		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(10000, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		l.getRunProperties().put("test.cmd", "main.thread");
		assertEquals(ProjectLauncher.OK,l.launch());
	}
}
