package aQute.bnd.osgi.resource;

import static org.assertj.core.api.Assertions.assertThatObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import aQute.bnd.osgi.resource.FileResourceCache.CacheKey;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

class FileResourceCacheKeyTest {

	@Test
	void unchanged(@InjectTemporaryDirectory
	Path tmp) throws Exception {
		Path subject = tmp.resolve("test");
		IO.store("line1", subject, StandardCharsets.UTF_8);
		CacheKey key1 = new CacheKey(subject);
		CacheKey key2 = new CacheKey(subject);
		assertThatObject(key1).isEqualTo(key2);
		assertThatObject(key1).hasSameHashCodeAs(key2);
	}

	@Test
	void change_modified(@InjectTemporaryDirectory
	Path tmp) throws Exception {
		Path subject = tmp.resolve("test");
		IO.store("line1", subject, StandardCharsets.UTF_8);
		CacheKey key1 = new CacheKey(subject);
		BasicFileAttributes attributes = Files.getFileAttributeView(subject, BasicFileAttributeView.class)
			.readAttributes();
		FileTime lastModifiedTime = attributes.lastModifiedTime();
		Instant plusSeconds = lastModifiedTime.toInstant()
			.plusSeconds(10L);
		Files.setLastModifiedTime(subject, FileTime.from(plusSeconds));
		CacheKey key2 = new CacheKey(subject);
		assertThatObject(key1).isNotEqualTo(key2);
		assertThatObject(key1).doesNotHaveSameHashCodeAs(key2);
	}

	@Test
	void change_size(@InjectTemporaryDirectory
	Path tmp) throws Exception {
		Path subject = tmp.resolve("test");
		IO.store("line1", subject, StandardCharsets.UTF_8);
		CacheKey key1 = new CacheKey(subject);
		BasicFileAttributes attributes = Files.getFileAttributeView(subject, BasicFileAttributeView.class)
			.readAttributes();
		FileTime lastModifiedTime = attributes.lastModifiedTime();
		IO.store("line100", subject, StandardCharsets.UTF_8);
		Files.setLastModifiedTime(subject, lastModifiedTime);
		CacheKey key2 = new CacheKey(subject);
		assertThatObject(key1).isNotEqualTo(key2);
		assertThatObject(key1).doesNotHaveSameHashCodeAs(key2);
	}

	@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows FS does not support fileKey")
	@Test
	void change_filekey(@InjectTemporaryDirectory
	Path tmp) throws Exception {
		Path subject = tmp.resolve("test");
		IO.store("line1", subject, StandardCharsets.UTF_8);
		CacheKey key1 = new CacheKey(subject);
		BasicFileAttributes attributes = Files.getFileAttributeView(subject, BasicFileAttributeView.class)
			.readAttributes();
		assertThatObject(attributes.fileKey()).isNotNull();
		FileTime lastModifiedTime = attributes.lastModifiedTime();
		Path subject2 = tmp.resolve("test.tmp");
		IO.store("line2", subject2, StandardCharsets.UTF_8);
		Files.setLastModifiedTime(subject2, lastModifiedTime);
		IO.rename(subject2, subject);
		CacheKey key2 = new CacheKey(subject);
		attributes = Files.getFileAttributeView(subject, BasicFileAttributeView.class)
			.readAttributes();
		assertThatObject(attributes.fileKey()).isNotNull();
		assertThatObject(key1).as("key2 not equal")
			.isNotEqualTo(key2);
		assertThatObject(key1).as("key2 different hash")
			.doesNotHaveSameHashCodeAs(key2);
	}

	@Test
	void change_file_modified(@InjectTemporaryDirectory
	Path tmp) throws Exception {
		Path subject = tmp.resolve("test");
		IO.store("line1", subject, StandardCharsets.UTF_8);
		CacheKey key1 = new CacheKey(subject);
		Path subject2 = tmp.resolve("test.tmp");
		IO.store("line2", subject2, StandardCharsets.UTF_8);
		BasicFileAttributes attributes = Files.getFileAttributeView(subject2, BasicFileAttributeView.class)
			.readAttributes();
		FileTime lastModifiedTime = attributes.lastModifiedTime();
		Instant plusSeconds = lastModifiedTime.toInstant()
			.plusSeconds(10L);
		Files.setLastModifiedTime(subject2, FileTime.from(plusSeconds));
		IO.rename(subject2, subject);
		CacheKey key2 = new CacheKey(subject);
		assertThatObject(key1).as("key2 not equal")
			.isNotEqualTo(key2);
		assertThatObject(key1).as("key2 different hash")
			.doesNotHaveSameHashCodeAs(key2);
	}

	@Test
	void different_files(@InjectTemporaryDirectory
	Path tmp) throws Exception {
		Path subject1 = tmp.resolve("test1");
		IO.store("line1", subject1, StandardCharsets.UTF_8);
		CacheKey key1 = new CacheKey(subject1);
		Path subject2 = tmp.resolve("test2");
		IO.copy(subject1, subject2);
		CacheKey key2 = new CacheKey(subject2);
		assertThatObject(key1).isNotEqualTo(key2);
		assertThatObject(key1).doesNotHaveSameHashCodeAs(key2);
	}

}
