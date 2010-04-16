package aQute.bnd.junit;



public class OSGiJUnitLaunchShortcut extends
		org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut {
	public OSGiJUnitLaunchShortcut() {
		//System.out.println("Constructore Launch Shortcut");
	}
	
	protected String getLaunchConfigurationTypeId() {
		return "aQute.bnd.junit.launchconfig";
	}

}
