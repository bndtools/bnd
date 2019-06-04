package aQute.libg.tarjan;

import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tarjan<T> {

	public class Node {
		final T				name;
		final List<Node>	adjacent	= new ArrayList<>();
		int					low			= -1;
		int					index		= -1;

		public Node(T name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name + "{" + index + "," + low + "}";
		}
	}

	private int				index	= 0;
	private List<Node>		stack	= new ArrayList<>();
	private List<List<T>>	scc		= new ArrayList<>();
	private Node			root	= new Node(null);

	void tarjan(Node v) {
		v.index = index;
		v.low = index;
		index++;
		stack.add(0, v);
		for (Node n : v.adjacent) {
			if (n.index == -1) {
				// first time visit
				tarjan(n);
				v.low = min(v.low, n.low);
			} else if (stack.contains(n)) {
				v.low = min(v.low, n.index);
			}
		}

		if (v != root && v.low == v.index) {
			List<T> component = new ArrayList<>();
			Node n;
			do {
				n = stack.remove(0);
				component.add(n.name);
			} while (n != v);
			scc.add(component);
		}
	}

	List<List<T>> getResult(Map<T, ? extends Collection<T>> graph) {
		Map<T, Node> index = new HashMap<>();

		for (Map.Entry<T, ? extends Collection<T>> entry : graph.entrySet()) {
			Node node = getNode(index, entry.getKey());
			root.adjacent.add(node);
			for (T adj : entry.getValue())
				node.adjacent.add(getNode(index, adj));
		}
		tarjan(root);
		return scc;
	}

	private Node getNode(Map<T, Node> index, T key) {
		Node node = index.get(key);
		if (node == null) {
			node = new Node(key);
			index.put(key, node);
		}
		return node;
	}

	public static <T> Collection<? extends Collection<T>> tarjan(Map<T, ? extends Collection<T>> graph) {
		Tarjan<T> tarjan = new Tarjan<>();
		return tarjan.getResult(graph);
	}
}
