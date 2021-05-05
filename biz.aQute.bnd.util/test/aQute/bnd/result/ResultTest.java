package aQute.bnd.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ResultTest {

	@Test
	void of_non_null_both() throws Exception {
		Result<String> result = Result.of("value", "error");
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void of_non_null_value() throws Exception {
		Result<String> result = Result.of("value", null);
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void of_null_value() throws Exception {
		Result<String> result = Result.of(null, "error");
		assertThat(result.isOk()).isFalse();
		assertThat(result.isErr()).isTrue();
		assertThat(result.value()).isEmpty();
		assertThatCode(() -> result.unwrap()).isInstanceOf(ResultException.class);
		assertThatCode(() -> result.unwrap("unwrap")).isInstanceOf(ResultException.class)
			.hasMessage("unwrap");
		assertThat(result.orElse("foo")).isEqualTo("foo");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("foo");
		assertThatCode(() -> result.orElseThrow(Exception::new)).hasMessage("error");
		assertThat(result.error()).hasValue("error");
		assertThat(result.asError()).isSameAs(result);
	}

	@Test
	void of_null_both() throws Exception {
		assertThatNullPointerException().isThrownBy(() -> Result.of(null, null));
	}

	@Test
	void ok() throws Exception {
		Result<String> result = Result.ok("value");
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void ok_null() throws Exception {
		Result<Void> result = Result.ok(null);
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).isEmpty();
		assertThat(result.unwrap()).isNull();
		assertThat(result.unwrap("unwrap")).isNull();
		assertThat(result.orElse(null)).isNull();
		assertThat(result.orElseGet(() -> null)).isNull();
		assertThat(result.orElseThrow(Exception::new)).isNull();
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void err() throws Exception {
		Result<String> result = Result.err("error");
		assertThat(result.isOk()).isFalse();
		assertThat(result.isErr()).isTrue();
		assertThat(result.value()).isEmpty();
		assertThatCode(() -> result.unwrap()).isInstanceOf(ResultException.class);
		assertThatCode(() -> result.unwrap("unwrap")).isInstanceOf(ResultException.class)
			.hasMessage("unwrap");
		assertThat(result.orElse("foo")).isEqualTo("foo");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("foo");
		assertThatCode(() -> result.orElseThrow(Exception::new)).hasMessage("error");
		assertThat(result.error()).hasValue("error");
		assertThat(result.asError()).isSameAs(result);
	}

	@Test
	void err_format() throws Exception {
		Result<String> result = Result.err("%s", "error");
		assertThat(result.isOk()).isFalse();
		assertThat(result.isErr()).isTrue();
		assertThat(result.value()).isEmpty();
		assertThatCode(() -> result.unwrap()).isInstanceOf(ResultException.class);
		assertThatCode(() -> result.unwrap("unwrap")).isInstanceOf(ResultException.class)
			.hasMessage("unwrap");
		assertThat(result.orElse("foo")).isEqualTo("foo");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("foo");
		assertThatCode(() -> result.orElseThrow(Exception::new)).hasMessage("error");
		assertThat(result.error()).hasValue("error");
		assertThat(result.asError()).isSameAs(result);
	}

	@Test
	void err_null() throws Exception {
		assertThatNullPointerException().isThrownBy(() -> Result.err(null));
	}

	@Test
	void map() throws Exception {
		Result<Integer> original = Result.ok(1);
		Result<CharSequence> result = original.map(v -> "value");
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void map_null_function() throws Exception {
		Result<Integer> original = Result.ok(1);
		assertThatNullPointerException().isThrownBy(() -> original.map(null));
	}

	@Test
	void map_null_value() throws Exception {
		Result<Integer> original = Result.ok(1);
		Result<String> result = original.map(v -> null);
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).isEmpty();
		assertThat(result.unwrap()).isNull();
		assertThat(result.unwrap("unwrap")).isNull();
		assertThat(result.orElse("foo")).isNull();
		assertThat(result.orElseGet(() -> "foo")).isNull();
		assertThat(result.orElseThrow(Exception::new)).isNull();
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void maperr() throws Exception {
		Result<String> original = Result.err("original");
		Result<String> result = original.mapErr((Object e) -> "error");
		assertThat(result.isOk()).isFalse();
		assertThat(result.isErr()).isTrue();
		assertThat(result.value()).isEmpty();
		assertThatCode(() -> result.unwrap()).isInstanceOf(ResultException.class);
		assertThatCode(() -> result.unwrap("unwrap")).isInstanceOf(ResultException.class)
			.hasMessage("unwrap");
		assertThat(result.orElse("foo")).isEqualTo("foo");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("foo");
		assertThatCode(() -> result.orElseThrow(Exception::new)).hasMessage("error");
		assertThat(result.error()).hasValue("error");
		assertThat(result.asError()).isSameAs(result);
	}

	@Test
	void maperr_null_function() throws Exception {
		Result<String> original = Result.err("original");
		assertThatNullPointerException().isThrownBy(() -> original.mapErr(null));
	}

	@Test
	void maperr_null_value() throws Exception {
		Result<String> original = Result.err("original");
		assertThatNullPointerException().isThrownBy(() -> original.mapErr(e -> null));
	}

	@Test
	void flatmap() throws Exception {
		Result<Integer> original = Result.ok(1);
		Result<CharSequence> result = original.flatMap(v -> Result.ok("value"));
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void flatmap_err() throws Exception {
		Result<Integer> original = Result.ok(1);
		Result<CharSequence> result = original.flatMap(v -> Result.err("error"));
		assertThat(result.isOk()).isFalse();
		assertThat(result.isErr()).isTrue();
		assertThat(result.value()).isEmpty();
		assertThatCode(() -> result.unwrap()).isInstanceOf(ResultException.class);
		assertThatCode(() -> result.unwrap("unwrap")).isInstanceOf(ResultException.class)
			.hasMessage("unwrap");
		assertThat(result.orElse("foo")).isEqualTo("foo");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("foo");
		assertThatCode(() -> result.orElseThrow(Exception::new)).hasMessage("error");
		assertThat(result.error()).hasValue("error");
		assertThat(result.asError()).isSameAs(result);
	}

	@Test
	void flatmap_null_function() throws Exception {
		Result<Integer> original = Result.ok(1);
		assertThatNullPointerException().isThrownBy(() -> original.flatMap(null));
	}

	@Test
	void flatmap_null_value() throws Exception {
		Result<Integer> original = Result.ok(1);
		assertThatNullPointerException().isThrownBy(() -> original.flatMap(v -> null));
	}

	@Test
	void recover() throws Exception {
		Result<CharSequence> original = Result.err("error");
		Result<CharSequence> result = original.recover((CharSequence e) -> "value");
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void recover_null_function() throws Exception {
		Result<String> original = Result.err("error");
		assertThatNullPointerException().isThrownBy(() -> original.recover(null));
	}

	@Test
	void recover_null_value() throws Exception {
		Result<String> original = Result.err("error");
		Result<String> result = original.recover(e -> null);
		assertThat(result.isOk()).isFalse();
		assertThat(result.isErr()).isTrue();
		assertThat(result.value()).isEmpty();
		assertThatCode(() -> result.unwrap()).isInstanceOf(ResultException.class);
		assertThatCode(() -> result.unwrap("unwrap")).isInstanceOf(ResultException.class)
			.hasMessage("unwrap");
		assertThat(result.orElse("foo")).isEqualTo("foo");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("foo");
		assertThatCode(() -> result.orElseThrow(Exception::new)).hasMessage("error");
		assertThat(result.error()).hasValue("error");
		assertThat(result.asError()).isSameAs(result);
	}

	@Test
	void recover_with() throws Exception {
		Result<CharSequence> original = Result.err("error");
		Result<CharSequence> result = original.recoverWith((CharSequence e) -> Result.ok("value"));
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).hasValue("value");
		assertThat(result.unwrap()).isEqualTo("value");
		assertThat(result.unwrap("unwrap")).isEqualTo("value");
		assertThat(result.orElse("foo")).isEqualTo("value");
		assertThat(result.orElseGet(() -> "foo")).isEqualTo("value");
		assertThat(result.orElseThrow(Exception::new)).isEqualTo("value");
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void recover_with_ok_null() throws Exception {
		Result<CharSequence> original = Result.err("error");
		Result<CharSequence> result = original.recoverWith((CharSequence e) -> Result.ok(null));
		assertThat(result.isOk()).isTrue();
		assertThat(result.isErr()).isFalse();
		assertThat(result.value()).isEmpty();
		assertThat(result.unwrap()).isNull();
		assertThat(result.unwrap("unwrap")).isNull();
		assertThat(result.orElse("foo")).isNull();
		assertThat(result.orElseGet(() -> "foo")).isNull();
		assertThat(result.orElseThrow(Exception::new)).isNull();
		assertThat(result.error()).isEmpty();
		assertThatCode(() -> result.asError()).isInstanceOf(ResultException.class);
	}

	@Test
	void recover_with_null_function() throws Exception {
		Result<String> original = Result.err("error");
		assertThatNullPointerException().isThrownBy(() -> original.recoverWith(null));
	}

	@Test
	void recover_with_null_value() throws Exception {
		Result<String> original = Result.err("error");
		assertThatNullPointerException().isThrownBy(() -> original.recoverWith(e -> null));
	}

	@Test
	void accept_ok() throws Exception {
		Result<String> result = Result.ok("value");
		AtomicReference<CharSequence> valueHolder = new AtomicReference<>("original");
		result.accept(valueHolder::set, e -> fail("Result is not an Err"));
		assertThat(valueHolder).hasValue("value");
	}

	@Test
	void accept_ok_null() throws Exception {
		Result<String> result = Result.ok("value");
		assertThatNullPointerException().isThrownBy(() -> result.accept(null, e -> fail("ok is null")));
		assertThatNullPointerException().isThrownBy(() -> result.accept(e -> fail("err is null"), null));
		assertThatNullPointerException().isThrownBy(() -> result.accept(null, null));
	}

	@Test
	void accept_err() throws Exception {
		Result<String> result = Result.err("error");
		AtomicReference<CharSequence> errorHolder = new AtomicReference<>("original");
		result.accept(v -> fail("Result is not an Ok"), errorHolder::set);
		assertThat(errorHolder).hasValue("error");
	}

	@Test
	void accept_err_null() throws Exception {
		Result<String> result = Result.err("error");
		assertThatNullPointerException().isThrownBy(() -> result.accept(null, e -> fail("ok is null")));
		assertThatNullPointerException().isThrownBy(() -> result.accept(e -> fail("err is null"), null));
		assertThatNullPointerException().isThrownBy(() -> result.accept(null, null));
	}
}
