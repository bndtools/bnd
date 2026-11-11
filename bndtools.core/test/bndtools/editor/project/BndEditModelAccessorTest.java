package bndtools.editor.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

class BndEditModelAccessorTest {
	@ParameterizedTest
	@CsvSource({
		"-runfw, org.eclipse.osgi, org.apache.felix.framework",
		"-runee, JavaSE-17, JavaSE-21"
	})
	void inheritedScalarUsesExactKey(String key, String inherited, String local,
		@InjectTemporaryDirectory File tmp) throws Exception {
		File included = new File(tmp, "shared.bndrun");
		IO.store(key + ": " + inherited + "\n" + key + ".extra: ignored\n", included);
		File run = new File(tmp, "app.bndrun");
		IO.store("-include: shared.bndrun\n" + key + ".local: ignored\n", run);
		try (Processor owner = new Processor()) {
			owner.setProperties(run);
			BndEditModel model = new BndEditModel(null, owner);
			assertThat(BndEditModelAccessor.getInheritedScalarValue(model, key)).isEqualTo(inherited);
			assertThat(BndEditModelAccessor.getScalarPropertyProvenance(model, key))
				.contains(included.getAbsolutePath());
			assertThat(model.getDocumentProperties()).doesNotContainKey(key);
			assertThat((Object) model.getTypedProperty(key)).isNull();

			model.loadFrom("-include: shared.bndrun\n" + key + ": " + local + "\n");
			assertThat(BndEditModelAccessor.getInheritedScalarValue(model, key)).isNull();

			model.loadFrom("-include: shared.bndrun\n" + key + ":\n");
			assertThat(BndEditModelAccessor.getInheritedScalarValue(model, key)).isNull();
		}
	}

	@ParameterizedTest
	@CsvSource({ "-runfw", "-runee" })
	void suffixedKeysDoNotSupplyScalarValue(String key, @InjectTemporaryDirectory File tmp) throws Exception {
		File included = new File(tmp, "shared.bndrun");
		IO.store(key + ".extra: ignored\n", included);
		File run = new File(tmp, "app.bndrun");
		IO.store("-include: shared.bndrun\n", run);
		try (Processor owner = new Processor()) {
			owner.setProperties(run);
			BndEditModel model = new BndEditModel(null, owner);
			assertThat(BndEditModelAccessor.getInheritedScalarValue(model, key)).isNull();
			assertThat(BndEditModelAccessor.getScalarPropertyProvenance(model, key)).isEmpty();
		}
	}
}
