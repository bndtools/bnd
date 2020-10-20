package aQute.lib.hierarchy;

import java.util.Optional;
import java.util.stream.Stream;

public interface FolderNode extends NamedNode, Iterable<NamedNode> {
	NamedNode[] children();

	default Stream<String> names() {
		return stream().map(NamedNode::name);
	}

	default Stream<NamedNode> stream() {
		return Stream.of(children());
	}

	int size();

	Optional<NamedNode> get(String name);

}
