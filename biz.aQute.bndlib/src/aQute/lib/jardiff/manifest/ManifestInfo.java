package aQute.lib.jardiff.manifest;

import java.util.*;
import java.util.jar.*;

import aQute.lib.jardiff.*;

public class ManifestInfo implements ManifestDiff {

	private Diff container;
	private String name;
	private Manifest newManifest;
	private Manifest oldManifest;
	
	private List<Diff> diffs = new ArrayList<Diff>();
	
	private Delta delta = Delta.UNCHANGED;
	
	public ManifestInfo(Diff container, String name, Manifest newManifest, Manifest oldManifest) {
		this.container = container;
		this.name = name;
		this.newManifest = newManifest;
		this.oldManifest = oldManifest;
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
		return container;
	}

	public Collection<? extends Diff> getContained() {
		return diffs;
	}

	public void addDiff(Diff diff) {
		diffs.add(diff);
	}
	
	public String explain() {
		return getDelta() + "";
	}
	
	public Manifest getNewManifest() {
		return newManifest;
	}

	public Manifest getOldManifest() {
		return oldManifest;
	}

}
