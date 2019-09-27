package org.bndtools.builder.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.Test;

public class BndContainerTest {

	@Test
	public void testBndContainerSerialization() throws Exception {
		ClasspathContainerSerializationHelper<BndContainer> serializationHelper = new ClasspathContainerSerializationHelper<>();

		BndContainer c = new BndContainer.Builder().updateLastModified(System.currentTimeMillis())
			.build();
		assertThat(c).isNotNull();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializationHelper.writeClasspathContainer(c, baos);

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		BndContainer c2 = serializationHelper.readClasspathContainer(bais);

		assertThat(c2).isNotSameAs(c);
		assertThat(c2.getClasspathEntries()).isEqualTo(c.getClasspathEntries());
		assertThat(c2.lastModified()).isEqualTo(c.lastModified());

	}

}
