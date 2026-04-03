package bndtools.wizards.repo;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import bndtools.utils.HierarchicalLabel;
import bndtools.utils.LabelParser;

/**
 * TODO
 */
@ExtendWith(SoftAssertionsExtension.class)
public class HierarchicalLabelTest {

	@InjectSoftAssertions
	SoftAssertions softly;

	@Test
	public void testHierarchicalLabel() {

		HierarchicalLabel<String> one = new HierarchicalLabel<>("Foo", l -> l.getLeaf());
		softly.assertThat(one.getFirst())
			.isEqualTo("Foo");
		softly.assertThat(one.getLeaf())
			.isEqualTo("Foo");
		softly.assertThat(one.getNumLLevels())
			.isEqualTo(1);
		softly.assertThat(one.getByPosition(0))
			.isEqualTo("Foo");

		HierarchicalLabel<String> two = new HierarchicalLabel<>("Foo :: Bar", l -> l.getLeaf());
		softly.assertThat(two.getFirst())
			.isEqualTo("Foo");
		softly.assertThat(two.getLeaf())
			.isEqualTo("Bar");
		softly.assertThat(two.getNumLLevels())
			.isEqualTo(2);
		softly.assertThat(two.getByPosition(0))
			.isEqualTo("Foo");
		softly.assertThat(two.getByPosition(1))
			.isEqualTo("Bar");
		softly.assertThatException()
			.isThrownBy(() -> two.getByPosition(2))
			.withMessageContaining("Position out of bounds.");

		HierarchicalLabel<String> disabled = new HierarchicalLabel<>("-!Foo :: Bar{This is a description}",
			l -> l.getLeaf());
		softly.assertThat(disabled.getLeaf())
			.isEqualTo("Bar");
		softly.assertThat(disabled.getDescription())
			.isEqualTo("This is a description");
		softly.assertThat(disabled.isEnabled())
			.isFalse();
		softly.assertThat(disabled.isChecked())
			.isTrue();

	}

	@Test
	public void testLabelParser() {
		LabelParser a = new LabelParser("-!MyLabel{This is a description}");
		softly.assertThat(a.getLabel())
			.isEqualTo("MyLabel");
		softly.assertThat(a.getDescription())
			.isEqualTo("This is a description");
		softly.assertThat(a.isEnabled())
			.isFalse();
		softly.assertThat(a.isChecked())
			.isTrue();

		LabelParser b = new LabelParser("MyLabel{This is a description}");
		softly.assertThat(b.getLabel())
			.isEqualTo("MyLabel");
		softly.assertThat(b.getDescription())
			.isEqualTo("This is a description");
		softly.assertThat(b.isEnabled())
			.isTrue();
		softly.assertThat(b.isChecked())
			.isFalse();

		LabelParser c = new LabelParser("!MyLabel");
		softly.assertThat(c.getLabel())
			.isEqualTo("MyLabel");
		softly.assertThat(c.getDescription())
			.isNull();
		softly.assertThat(c.isEnabled())
			.isTrue();
		softly.assertThat(c.isChecked())
			.isTrue();
	}


}
