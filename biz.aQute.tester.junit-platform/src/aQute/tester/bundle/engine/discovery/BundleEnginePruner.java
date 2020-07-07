package aQute.tester.bundle.engine.discovery;

import static aQute.lib.strings.Strings.splitAsStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId.Segment;

import aQute.tester.bundle.engine.BundleEngineDescriptor;

public class BundleEnginePruner {

	public enum PruneType {
		NEVER,
		ONLYCHILD,
		ALWAYS
	}

	final Map<String, PruneType> directives;

	final static Set<String>		ALLOWED_NODE_TYPES;

	static {
		Set<String> allowed = new HashSet<>();
		allowed.add("bundle");
		allowed.add("fragment");
		allowed.add("sub-engine");
		ALLOWED_NODE_TYPES = Collections.unmodifiableSet(allowed);
	}

	public BundleEnginePruner(String prune) {
		directives = new HashMap<>();
		splitAsStream(prune).forEach(pair -> {
			int index = pair.indexOf('=');
			if (index < 0) {
				throw new IllegalArgumentException("Malformed prune directive: missing '='");
			}
			String nodeType = pair.substring(0, index);
			if (!ALLOWED_NODE_TYPES.contains(nodeType)) {
				throw new IllegalArgumentException("Malformed prune directive: unknown node type '" + nodeType
					+ "'; expecting bundle, fragment or sub-engine");
			}
			String pruneType = pair.substring(index + 1);
			try {
				directives.put(nodeType, PruneType.valueOf(pruneType.toUpperCase()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Malformed prune directive: unknown prune type '" + pruneType
					+ "'; expecting never, onlychild or always");
			}
		});
	}

	PruneType getDirective(String nodeType) {
		return directives.getOrDefault(nodeType, PruneType.NEVER);
	}

	static Segment getLastSegment(TestDescriptor descriptor) {
		List<Segment> uidSegments = descriptor.getUniqueId()
			.getSegments();
		return uidSegments.get(uidSegments.size() - 1);
	}

	static String getType(TestDescriptor descriptor) {
		return getLastSegment(descriptor).getType();
	}

	boolean shouldPrune(TestDescriptor descriptor) {
		String type = getType(descriptor);
		if (currentBundleBeingPruned && type.equals("fragment")) {
			return true;
		}
		PruneType directive = getDirective(type);
		return directive == PruneType.ALWAYS || (directive == PruneType.ONLYCHILD && descriptor.getParent()
			.get()
			.getChildren()
			.size() == 1);
	}

	public void prune(BundleEngineDescriptor rootDescriptor) {
		new ArrayList<>(rootDescriptor.getChildren()).forEach(child -> pruneAndReparentChildren(child, rootDescriptor));
	}

	Predicate<TestDescriptor> withSameIdAs(TestDescriptor other) {
		Segment lastSegment = getLastSegment(other);
		String type = lastSegment.getType();
		String value = lastSegment.getValue();
		return descriptor -> {
			Segment last = getLastSegment(descriptor);
			return last.getType()
				.equals(type)
				&& last.getValue()
					.equals(value);
		};
	}

	// Optionally prunes the given descriptor, and reparents it (or its children
	// if it is being pruned) to the given parent.
	void pruneAndReparentChildren(TestDescriptor descriptor, TestDescriptor newParent) {
		String type = getType(descriptor);

		TestDescriptor currentParent = descriptor.getParent()
			.get();

		if (shouldPrune(descriptor)) {
			if (type.equals("bundle")) {
				currentBundleBeingPruned = true;
			}
			currentParent.removeChild(descriptor);
			// Have to take a copy otherwise we get a
			// ConcurrentModificationException
			// while iterating, and the iterator doesn't support remove()
			List<TestDescriptor> children = new ArrayList<>(descriptor.getChildren());
			for (TestDescriptor child : children) {
				pruneAndReparentChildren(child, newParent);
			}
			if (type.equals("bundle")) {
				currentBundleBeingPruned = false;
			}
		} else {
			if (newParent != currentParent) {
				currentParent.removeChild(descriptor);
				Optional<? extends TestDescriptor> existing = newParent.getChildren()
					.stream()
					.filter(withSameIdAs(descriptor))
					.findAny();
				if (existing.isPresent()) {
					newParent = existing.get();
				} else {
					newParent.addChild(descriptor);
					newParent = descriptor;
				}
			} else {
				newParent = descriptor;
			}
			switch (type) {
				case "bundle" :
				case "fragment" :
				case "sub-engine" :
					// Copy to prevent ConcurrentModificationException
					List<TestDescriptor> children = new ArrayList<>(descriptor.getChildren());
					for (TestDescriptor child : children) {
						pruneAndReparentChildren(child, newParent);
					}
					break;
				default :
					// Terminate the tree descent if we've already gone past the
					// depth of sub-engine
			}
		}
	}

	boolean currentBundleBeingPruned = false;
}
