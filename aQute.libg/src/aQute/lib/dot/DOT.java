package aQute.lib.dot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.lib.collections.MultiMap;

/**
 * Simple utility to create a graph in the dot language
 *
 * @param <Vertex> the type of vertex
 */
public class DOT<Vertex> {

	interface Attribute {
		void render(Formatter f);
	}

	class Edge {
		Vertex	from;
		Vertex	to;
	}

	final Map<Vertex, ? extends Collection<Vertex>>	graph;
	final Map<Vertex, String>						names				= new HashMap<>();
	final MultiMap<Vertex, Attribute>				vertexAttributes	= new MultiMap<>();
	final MultiMap<Edge, Attribute>					edgeAttributes		= new MultiMap<>();
	final List<Attribute>							graphAttributes		= new ArrayList<>();
	final String									id;

	public DOT(String id, Map<Vertex, ? extends Collection<Vertex>> graph) {
		this.id = id;
		this.graph = graph;
	}

	public String render() {
		try (Formatter f = new Formatter()) {
			f.format("digraph \"%s\" {\n", id);
			for (Attribute a : graphAttributes) {
				a.render(f);
			}
			graph.entrySet()
				.forEach(e -> {
					Vertex from = e.getKey();
					e.getValue()
						.forEach(to -> vertex(f, from, to));
				});
			f.format("}\n");
			return f.toString();
		}

	}

	public DOT<Vertex> ranksep(double ranksep) {
		graphAttributes.add((f) -> f.format("  ranksep %d;\n", ranksep));
		return this;
	}

	public DOT<Vertex> nodesep(double nodesep) {
		graphAttributes.add((f) -> f.format("  nodesep %d;\n", nodesep));
		return this;
	}

	private void vertex(Formatter f, Vertex from, Vertex to) {
		String fName = getName(from);
		String tName = getName(to);
		f.format("  \"%s\" -> \"%s\"\n", fName, tName);

	}

	public DOT<Vertex> name(Vertex vertex, String string) {
		names.put(vertex, string);
		return this;
	}

	private String getName(Vertex vertex) {
		return names.computeIfAbsent(vertex, Vertex::toString);
	}

	public DOT<Vertex> prune() {
		for (Entry<Vertex, ? extends Collection<Vertex>> it : graph.entrySet()) {
			HashSet<Vertex> set = new HashSet<>(it.getValue());
			it.getValue()
				.clear();
			it.getValue()
				.addAll(set);
		}

		return this;
	}

}
