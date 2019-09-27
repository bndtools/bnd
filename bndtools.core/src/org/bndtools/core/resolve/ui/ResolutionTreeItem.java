package org.bndtools.core.resolve.ui;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Wire;

public class ResolutionTreeItem {

	private final Capability	capability;
	private final List<Wire>	wires	= new LinkedList<>();

	public ResolutionTreeItem(Capability capability) {
		this.capability = capability;
	}

	public Capability getCapability() {
		return capability;
	}

	public void addWire(Wire wire) {
		wires.add(wire);
	}

	public List<Wire> getWires() {
		return Collections.unmodifiableList(wires);
	}

}
