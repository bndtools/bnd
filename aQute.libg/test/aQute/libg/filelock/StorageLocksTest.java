package aQute.libg.filelock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;

public class StorageLocksTest {
	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void findsLocksAboveAndWhenDeletedBelow() throws Exception {
		File outer = new File(tmp, "outer").getCanonicalFile();
		File inner = new File(outer, "a/b/c/d/fw");
		inner.mkdirs();

		assertThat(StorageLocks.lockFile(outer)).isEqualTo(new File(tmp.getCanonicalFile(), "outer.lock"));
		assertThat(StorageLocks.findHeld(outer, true, Collections.emptySet())).isEmpty();

		File innerLock = StorageLocks.lockFile(inner);
		try (ProcessLock held = ProcessLock.tryLock(innerLock)
			.orElseThrow()) {
			assertThat(StorageLocks.findHeld(outer, false, Collections.emptySet())).isEmpty();
			assertThat(StorageLocks.findHeld(outer, true, Collections.emptySet())).contains(innerLock);
			assertThat(StorageLocks.findHeld(outer, true, Set.of(innerLock))).isEmpty();
		}
		assertThat(StorageLocks.findHeld(outer, true, Collections.emptySet())).isEmpty();

		File outerLock = StorageLocks.lockFile(outer);
		try (ProcessLock held = ProcessLock.tryLock(outerLock)
			.orElseThrow()) {
			assertThat(StorageLocks.findHeld(inner, false, Collections.emptySet())).contains(outerLock);
			assertThat(StorageLocks.findHeld(outer, false, Collections.emptySet())).contains(outerLock);
			assertThat(StorageLocks.findHeld(inner, true, Set.of(outerLock))).isEmpty();
		}
	}

	@Test
	public void unreadableDirectoryBelowFailsTheCheck() throws Exception {
		File outer = new File(tmp, "outer").getCanonicalFile();
		File hidden = new File(outer, "hidden");
		hidden.mkdirs();
		assumeTrue(hidden.setReadable(false) && !Files.isReadable(hidden.toPath()),
			"directory cannot be made unreadable");
		try {
			assertThatIOException().isThrownBy(() -> StorageLocks.findHeld(outer, true, Collections.emptySet()));
			assertThat(StorageLocks.findHeld(outer, false, Collections.emptySet())).isEmpty();
		} finally {
			hidden.setReadable(true);
		}
	}
}
