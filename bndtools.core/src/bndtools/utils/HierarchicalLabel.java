package bndtools.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

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
public class HierarchicalLabel<T> {

	private static final String					DELIMITER_FOR_HIERARCHY	= " :: ";

	private final List<String>					labels;
	private Function<HierarchicalLabel<T>, T>	leafActionCallback;

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
	public HierarchicalLabel(String compoundLabel, Function<HierarchicalLabel<T>, T> leafNodeAction) {
		this.leafActionCallback = leafNodeAction;
		if (compoundLabel == null || compoundLabel.trim()
			.isEmpty()) {
			throw new IllegalArgumentException("Label string cannot be null or empty.");
		}

		LabelParser parsedEntry = new LabelParser(compoundLabel);
		this.enabled = parsedEntry.isEnabled();
		this.checked = parsedEntry.isChecked();
		this.description = parsedEntry.getDescription();
		String label = parsedEntry.getLabel();

		this.labels = new ArrayList<>(Arrays.asList(label.split(DELIMITER_FOR_HIERARCHY)));

	}

	public T getLeafAction() {
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

	/**
	 * @return the last entry (most right)
	 */
	public String getLeaf() {
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
		return String.join(DELIMITER_FOR_HIERARCHY, labels);
	}

}
