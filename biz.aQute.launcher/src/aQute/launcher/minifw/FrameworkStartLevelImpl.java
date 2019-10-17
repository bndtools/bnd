package aQute.launcher.minifw;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.FrameworkStartLevel;

class FrameworkStartLevelImpl implements FrameworkStartLevel {
	private final Framework	framework;
	private volatile int	startLevel;
	private volatile int	initialBundleStartLevel;

	FrameworkStartLevelImpl(Framework framework) {
		this.framework = framework;
		this.startLevel = 0;
		this.initialBundleStartLevel = 0;
	}

	@Override
	public Bundle getBundle() {
		return framework;
	}

	@Override
	public int getStartLevel() {
		return startLevel;
	}

	@Override
	public void setStartLevel(int startlevel, FrameworkListener... listeners) {
		this.startLevel = startlevel;
		if (listeners != null) {
			FrameworkEvent event = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, getBundle(), null);
			for (FrameworkListener listener : listeners) {
				listener.frameworkEvent(event);
			}
		}
	}

	@Override
	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	@Override
	public void setInitialBundleStartLevel(int startlevel) {
		this.initialBundleStartLevel = startlevel;
	}
}
