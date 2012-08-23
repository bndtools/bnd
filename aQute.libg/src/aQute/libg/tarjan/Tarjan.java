package aQute.libg.tarjan;

import static java.lang.Math.*;

import java.util.*;

public class Tarjan<T> {

	public class Node {
		final T				name;
		final List<Node>	adjacent	= new ArrayList<Node>();
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

	private int			index	= 0;
	private List<Node>	stack	= new ArrayList<Node>();
	private Set<Set<T>>	scc		= new HashSet<Set<T>>();
	private Node		root	= new Node(null);

	// public ArrayList<ArrayList<Node>> tarjan(Node v, AdjacencyList list){
	// v.index = index;
	// v.lowlink = index;
	// index++;
	// stack.add(0, v);
	// for(Edge e : list.getAdjacent(v)){
	// Node n = e.to;
	// if(n.index == -1){
	// tarjan(n, list);
	// v.lowlink = Math.min(v.lowlink, n.lowlink);
	// }else if(stack.contains(n)){
	// v.lowlink = Math.min(v.lowlink, n.index);
	// }
	// }
	// if(v.lowlink == v.index){
	// Node n;
	// ArrayList<Node> component = new ArrayList<Node>();
	// do{
	// n = stack.remove(0);
	// component.add(n);
	// }while(n != v);
	// SCC.add(component);
	// }
	// return SCC;
	// }

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
			Set<T> component = new HashSet<T>();
			Node n;
			do {
				n = stack.remove(0);
				component.add(n.name);
			} while (n != v);
			scc.add(component);
		}
	}

	Set<Set<T>> getResult(Map<T, ? extends Collection<T>> graph) {
		Map<T,Node> index = new HashMap<T,Node>();

		for (Map.Entry<T, ? extends Collection<T>> entry : graph.entrySet()) {
			Node node = getNode(index, entry.getKey());
			root.adjacent.add(node);
			for (T adj : entry.getValue())
				node.adjacent.add(getNode(index, adj));
		}
		tarjan(root);
		return scc;
	}

	private Node getNode(Map<T,Node> index, T key) {
		Node node = index.get(key);
		if (node == null) {
			node = new Node(key);
			index.put(key, node);
		}
		return node;
	}

	public static <T> Collection< ? extends Collection<T>> tarjan(Map<T, ? extends Collection<T>> graph) {
		Tarjan<T> tarjan = new Tarjan<T>();
		return tarjan.getResult(graph);
	}
}
