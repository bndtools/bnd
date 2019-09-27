package aQute.lib.formatter;

import static aQute.lib.formatter.Formatters.format;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FormattersTest {

	@Test
	public void testAllConversionsToRightType() {
		// a|A|b|B|c|d|e|E|f|g|G|h|H|n|o|s|S|x|X
		assertThat(format("%a", "1")).isEqualTo("0x1.0p0");
		assertThat(format("%A", "1")).isEqualTo("0X1.0P0");
		assertThat(format("%b", "1")).isEqualTo("true");
		assertThat(format("%B", "1")).isEqualTo("TRUE");
		assertThat(format("%c", "1")).isEqualTo("1");
		assertThat(format("%d", "1")).isEqualTo("1");
		assertThat(format("%e", "1")).isEqualTo("1.000000e+00");
		assertThat(format("%E", "1")).isEqualTo("1.000000E+00");
		assertThat(format("%f", "1")).isEqualTo("1.000000");
		assertThat(format("%g", "1")).isEqualTo("1.00000");
		assertThat(format("%G", "1")).isEqualTo("1.00000");
		assertThat(format("%h", "1")).isEqualTo("31");
		assertThat(format("%H", "1")).isEqualTo("31");
		assertThat(format("%n")).matches("\r?\n");
		assertThat(format("%o", "1")).isEqualTo("1");
		assertThat(format("%s", "1")).isEqualTo("1");
		assertThat(format("%S", "1")).isEqualTo("1");
		assertThat(format("%x", "1")).isEqualTo("1");
		assertThat(format("%X", "1")).isEqualTo("1");
	}

	@Test
	public void testIndex() {
		assertThat(format("%s", "1")).isEqualTo("1");
		assertThat(format("%2$d %s %d", "1", "2")).isEqualTo("2 1 2");
		assertThat(format("%2$d %<d", "1", "2")).isEqualTo("2 2");
		assertThat(format("%d %<d", "1", "2")).isEqualTo("1 1");
		assertThat(format("%1$d %2$d %<s", "1", "2")).isEqualTo("1 2 2");
		assertThat(format("%1$s %<s %s", "1", "2")).isEqualTo("1 1 1");
		assertThat(format("%s %<s %s", "1", "2")).isEqualTo("1 1 2");
		assertThat(format("%% %s %% %<s %% %s", "1", "2")).isEqualTo("% 1 % 1 % 2");
		assertThat(format("%n %s %n %<s %n %s", "1", "2")).matches("\r?\n 1 \r?\n 1 \r?\n 2");
		assertThat(format("%2$d %<s %<s %s", "1", "2")).isEqualTo("2 2 2 1");
	}

	@Test
	public void testDate() {
		assertThat(format("%tY %<tm %<td %<tZ", "1970-01-01")).isEqualTo("1970 01 01 UTC");
		assertThat(format("%TY %<tm %<Td %<TZ", "1970-01-01")).isEqualTo("1970 01 01 UTC");
	}
}
