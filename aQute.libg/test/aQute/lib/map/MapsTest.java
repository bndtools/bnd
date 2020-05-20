package aQute.lib.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class MapsTest {

	@Test
	public void zero() {
		Map<String, String> map = Maps.mapOf();
		assertThat(map).hasSize(0)
			.isEmpty();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void one() {
		Map<String, String> map = Maps.mapOf("k1", "v1");
		assertThat(map).hasSize(1)
			.containsEntry("k1", "v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void two() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2");
		assertThat(map).hasSize(2)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void three() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3");
		assertThat(map).hasSize(3)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void four() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4");
		assertThat(map).hasSize(4)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void five() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5");
		assertThat(map).hasSize(5)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void six() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6");
		assertThat(map).hasSize(6)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void seven() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7");
		assertThat(map).hasSize(7)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void eight() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7", "k8", "v8");
		assertThat(map).hasSize(8)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7")
			.containsEntry("k8", "v8");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void nine() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7", "k8", "v8", "k9", "v9");
		assertThat(map).hasSize(9)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7")
			.containsEntry("k8", "v8")
			.containsEntry("k9", "v9");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void ten() {
		Map<String, String> map = Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7", "k8", "v8", "k9", "v9", "k10", "v10");
		assertThat(map).hasSize(10)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7")
			.containsEntry("k8", "v8")
			.containsEntry("k9", "v9")
			.containsEntry("k10", "v10");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void duplicate_key() {
		assertThatIllegalArgumentException().isThrownBy(() -> Maps.mapOf("k1", "v1", "k1", "v2"));
		assertThatIllegalArgumentException().isThrownBy(() -> Maps.mapOf("k1", "v1", "k2", "v2", "k2", "v3"));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Maps.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k3", "v4"));
	}

	@Test
	public void null_arguments() {
		assertThatNullPointerException().isThrownBy(() -> Maps.mapOf("k1", "v1", null, "v2"));
		assertThatNullPointerException().isThrownBy(() -> Maps.mapOf("k1", "v1", "k2", null));
	}

}
