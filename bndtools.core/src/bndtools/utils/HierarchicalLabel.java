package bndtools.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

/**
 * This class uses a list labels to internally store the different hierarchical
 * labels. It provides methods to get the number of layers, retrieve a label by
 * position, retrieve the first and last labels to e.g. build a menu with
 * submenues.
 *
 * <pre>
 * public static void main(String[] args) {
 * 	HierarchicalLabel label = new HierarchicalLabel("layer1/layer2/layer3/layer4", (l) -> new Action(l.getLast()));
 *
 * 	System.out.println("Number of layers: " + label.getNumLevels());
 * 	System.out.println("Label at position 2: " + label.getByPosition(2));
 * 	System.out.println("First label: " + label.getFirst());
 * 	System.out.println("Last label: " + label.getLast());
 *
 * }
 * </pre>
 */
public class HierarchicalLabel {

	private static final String					DELIMITER_FOR_HIERARCHY	= " :: ";

	/**
	 * <p>
	 * Regex for parsing the compoundLabelExpression in the constructor:
	 * </p>
	 * <ol>
	 * <li>
	 * <p>
	 * <strong>Group 1 <code>(-)?</code></strong>: Represents an optional minus
	 * sign <code>-</code>.
	 * </p>
	 * <ul>
	 * <li>This is used to determine if something is <code>enabled</code>. If
	 * this group is present, then <code>enabled</code> is set to
	 * <code>false</code>.</li>
	 * </ul>
	 * </li>
	 * <li>
	 * <p>
	 * <strong>Group 2 <code>(!)?</code></strong>: Represents an optional
	 * exclamation mark <code>!</code>.
	 * </p>
	 * <ul>
	 * <li>This is used to determine if something is <code>checked</code>. If
	 * this group is present, then <code>checked</code> is set to
	 * <code>true</code>.</li>
	 * </ul>
	 * </li>
	 * <li>
	 * <p>
	 * <strong>Group 3 <code>([^{}]+)</code></strong>: Matches one or more
	 * characters that are not curly braces <code>{</code> or <code>}</code>.
	 * </p>
	 * <ul>
	 * <li>This captures the actual <code>labeltext</code>.</li>
	 * </ul>
	 * </li>
	 * <li>
	 * <p>
	 * <strong>Group 4 <code>(?:\\{([^}]+)\\})?</code></strong>: This is an
	 * optional group that captures text inside curly braces.
	 * </p>
	 * <ul>
	 * <li><code>(?: ... )</code> is a non-capturing group.</li>
	 * <li><code>\\{([^}]+)\\}</code> captures characters between <code>{</code>
	 * and <code>}</code>.</li>
	 * <li>This captures the <code>description</code>.</li>
	 * </ul>
	 * </li> See Constructor for Example Strings
	 * </ol>
	 * </p>
	 */
	final static Pattern						LABEL_PATTERN			= Pattern
		.compile("(-)?(!)?([^{}]+)(?:\\{([^}]+)\\})?");

	private final List<String>					labels;
	private Function<HierarchicalLabel, Action>	leafActionCallback;

	boolean										enabled					= true;
	boolean										checked					= false;
	String										description				= null;

	/**
	 * @param compoundLabel a string consisting of multiple parts which is parse
	 *            by a regex into 4 parts. E.g.
	 *            <h3 id="example-strings-">Example Strings:</h3>
	 *            <ol>
	 *            <li>
	 *            <p>
	 *            <strong>&quot;-!MyLabel{This is a description}&quot;</strong>
	 *            </p>
	 *            <ul>
	 *            <li>Group 1: <code>-</code> =&gt; <code>enabled</code> is
	 *            <code>false</code>.</li>
	 *            <li>Group 2: <code>!</code> =&gt; <code>checked</code> is
	 *            <code>true</code>.</li>
	 *            <li>Group 3: <code>MyLabel</code> =&gt; Label text is
	 *            &quot;MyLabel&quot;.</li>
	 *            <li>Group 4: <code>This is a description</code> =&gt;
	 *            Description is &quot;This is a description&quot;.</li>
	 *            </ul>
	 *            </li>
	 *            <li>
	 *            <p>
	 *            <strong>&quot;!MyLabel&quot;</strong>
	 *            </p>
	 *            <ul>
	 *            <li>Group 1: Not present =&gt; <code>enabled</code> is not
	 *            affected (remains whatever value it previously had).</li>
	 *            <li>Group 2: <code>!</code> =&gt; <code>checked</code> is
	 *            <code>true</code>.</li>
	 *            <li>Group 3: <code>MyLabel</code> =&gt; Label text is
	 *            &quot;MyLabel&quot;.</li>
	 *            <li>Group 4: Not present =&gt; Description is
	 *            <code>null</code>.</li>
	 *            </ul>
	 *            </li>
	 *            <li>
	 *            <p>
	 *            <strong>&quot;MyLabel{Description here}&quot;</strong>
	 *            </p>
	 *            <ul>
	 *            <li>Group 1: Not present =&gt; <code>enabled</code> is not
	 *            affected.</li>
	 *            <li>Group 2: Not present =&gt; <code>checked</code> is not
	 *            affected.</li>
	 *            <li>Group 3: <code>MyLabel</code> =&gt; Label text is
	 *            &quot;MyLabel&quot;.</li>
	 *            <li>Group 4: <code>Description here</code> =&gt; Description
	 *            is &quot;Description here&quot;.</li>
	 *            </ul>
	 *            </li>
	 *            </ol>
	 *            <p>
	 *            These example strings illustrate how the regex pattern breaks
	 *            down and captures each part of the input string.
	 *            </p>
	 * @param leafNodeAction a callback to create an Action from the outside
	 */
	public HierarchicalLabel(String compoundLabel, Function<HierarchicalLabel, Action> leafNodeAction) {
		this.leafActionCallback = leafNodeAction;
		if (compoundLabel == null || compoundLabel.trim()
			.isEmpty()) {
			throw new IllegalArgumentException("Label string cannot be null or empty.");
		}

		String label = compoundLabel;
		Matcher m = LABEL_PATTERN.matcher(compoundLabel);
		if (m.matches()) {
			if (m.group(1) != null)
				enabled = false;

			if (m.group(2) != null)
				checked = true;

			label = m.group(3);

			description = m.group(4);
		}

		this.labels = new ArrayList<>(Arrays.asList(label.split(DELIMITER_FOR_HIERARCHY)));

	}

	public Action getLeafAction() {
		return leafActionCallback.apply(this);
	}

	public int getNumLLevels() {
		return labels.size();
	}

	public String getByPosition(int position) {
		if (position < 0 || position >= labels.size()) {
			throw new IllegalArgumentException("Position out of bounds.");
		}
		return labels.get(position);
	}

	public String getFirst() {
		return labels.get(0);
	}

	public String getLast() {
		return labels.get(labels.size() - 1);
	}

	public List<String> getLabels() {
		return labels;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isChecked() {
		return checked;
	}

	@Override
	public String toString() {
		return String.join("/", labels);
	}

	public MenuManager createMenuItems() {
		return createSubMenu(0);
	}

	private MenuManager createSubMenu(int position) {
		if (position >= labels.size()) {
			return null;
		}

		MenuManager current = new MenuManager(labels.get(position), null);
		if (position == labels.size() - 1) {
			current.add(leafActionCallback.apply(this));
		} else {
			MenuManager childMenu = createSubMenu(position + 1);
			if (childMenu != null) {
				current.add(childMenu);
			}
		}
		return current;
	}

	public static class HierarchicalMenu {

		private final List<HierarchicalLabel> labels = new ArrayList<>();

		public HierarchicalMenu() {

		}

		public HierarchicalMenu add(HierarchicalLabel label) {
			labels.add(label);
			return this;
		}

		public void build(IMenuManager root) {
			createMenu(labels, root);
		}

	}

	public static void createMenu(List<HierarchicalLabel> labels, IMenuManager rootMenu) {

		Map<String, IMenuManager> menus = new LinkedHashMap<>();

		for (HierarchicalLabel hl : labels) {
			IMenuManager current = rootMenu;
			List<String> hlLabels = hl.getLabels();
			for (int i = 0; i < hlLabels.size(); i++) {
				String currentLabel = hlLabels.get(i);
				if (i == hlLabels.size() - 1) {
					// currentMenu = getOrCreateSubMenu(menuMap, currentMenu,
					// currentLabel);
					current.add(hl.getLeafAction());
				} else {
					current = getOrCreateSubMenu(menus, current, currentLabel);
				}
			}
		}

	}

	private static IMenuManager getOrCreateSubMenu(Map<String, IMenuManager> menus, IMenuManager parent,
		String label) {
		String key = parent.toString() + " -> " + label;

		if (!menus.containsKey(key)) {
			IMenuManager newMenu = new MenuManager(label, null);
			parent.add(newMenu);
			menus.put(key, newMenu);
		}

		return menus.get(key);
	}
}
