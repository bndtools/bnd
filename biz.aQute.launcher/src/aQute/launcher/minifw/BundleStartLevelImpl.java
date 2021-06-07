package aQute.launcher.minifw;

import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

class BundleStartLevelImpl implements BundleStartLevel {
	private final Bundle	bundle;
	private volatile int	startLevel;

	BundleStartLevelImpl(Bundle bundle, FrameworkStartLevel frameworkStartLevel) {
		this.bundle = bundle;
		this.startLevel = frameworkStartLevel.getInitialBundleStartLevel();
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public int getStartLevel() {
		return startLevel;
	}

	@Override
	public void setStartLevel(int startlevel) {
		this.startLevel = startlevel;
	}

	@Override
	public boolean isPersistentlyStarted() {
		return false;
	}

	@Override
	public boolean isActivationPolicyUsed() {
		return false;
	}
}
