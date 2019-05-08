package aQute.lib.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.Test;

public class ByteBufferDataTest {

	private String readUTF(String actual) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(actual);
		DataInput bbin = ByteBufferDataInput.wrap(baos.toByteArray());
		return bbin.readUTF();
	}

	private String writeUTF(String actual) throws Exception {
		ByteBufferDataOutput bbout = new ByteBufferDataOutput();
		bbout.writeUTF(actual);
		ByteArrayInputStream bais = new ByteArrayInputStream(bbout.toByteArray());
		DataInput din = new DataInputStream(bais);
		return din.readUTF();
	}

	@Test
	public void testReadUTF8Empty() throws Exception {
		String expected = "";
		String actual = readUTF(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testReadUTF8Simple() throws Exception {
		String expected = "A simple String value!";
		String actual = readUTF(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testReadUTFMulti() throws Exception {
		String expected = "A multi <\u0801\uFFF0\u0000> String value!";
		String actual = readUTF(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testWriteUTF8Empty() throws Exception {
		String expected = "";
		String actual = writeUTF(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testWriteUTF8Simple() throws Exception {
		String expected = "A simple String value!";
		String actual = writeUTF(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testWriteUTFMulti() throws Exception {
		String expected = "A multi <\u0801\uFFF0\u0000> String value!";
		String actual = writeUTF(expected);
		assertThat(actual).isEqualTo(expected);
	}

}
