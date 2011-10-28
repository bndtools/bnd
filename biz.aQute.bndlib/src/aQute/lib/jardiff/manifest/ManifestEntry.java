package aQute.lib.jardiff.manifest;

import java.util.*;

import aQute.lib.jardiff.*;

public class ManifestEntry implements Diff {

	private ManifestInfo manifestInfo;
	
	private String name;
	private Delta delta = Delta.UNCHANGED;
	
	private String newValue;
	private String oldValue;
	
	public ManifestEntry(ManifestInfo manifestInfo, String name) {
		this.manifestInfo = manifestInfo;
		this.name = name;
	}

	public Delta getDelta() {
		return delta;
	}
	
	public void setDelta(Delta delta) {
		this.delta = delta;
	}

	public String getName() {
		return name;
	}

	public Diff getContainer() {
		return manifestInfo;
	}

	public Collection<? extends Diff> getContained() {
		return Collections.emptyList();
	}

	public String explain() {
		return getDelta() + " " + getName();
	}

	public void setNewValue(String value) {
		this.newValue  = value;
	}

	public void setOldValue(String value) {
		this.oldValue  = value;
	}

	public String getNewValue() {
		return newValue;
	}

	public String getOldValue() {
		return oldValue;
	}
}
