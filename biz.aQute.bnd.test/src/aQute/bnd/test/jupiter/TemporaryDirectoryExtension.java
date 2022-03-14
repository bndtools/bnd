/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package aQute.bnd.test.jupiter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.osgi.test.common.inject.TargetType;
import org.osgi.test.junit5.inject.InjectingExtension;

public class TemporaryDirectoryExtension extends InjectingExtension<InjectTemporaryDirectory> {

	public TemporaryDirectoryExtension() {
		super(InjectTemporaryDirectory.class, File.class, Path.class, String.class);
	}

	@Override
	protected Object resolveValue(TargetType targetType, InjectTemporaryDirectory annotation,
		ExtensionContext extensionContext) throws ParameterResolutionException {

		File temporaryDirectory = new File(annotation.value());
		Optional<Class<?>> testClass = extensionContext.getTestClass();
		if (testClass.isPresent()) {
			temporaryDirectory = new File(temporaryDirectory, testClass.get()
				.getName());
		}
		Optional<Method> testMethod = extensionContext.getTestMethod();
		if (testMethod.isPresent()) {
			temporaryDirectory = new File(temporaryDirectory, testMethod.get()
				.getName());
		}
		temporaryDirectory = temporaryDirectory.getAbsoluteFile();
		Path temporaryDirectoryPath = temporaryDirectory.toPath();

		try {
			if (annotation.clear()) {
				delete(temporaryDirectoryPath);
			}
			mkdirs(temporaryDirectoryPath);
		} catch (IOException e) {
			throw new ParameterResolutionException("unable to clear temporary directory", e);
		}

		if (targetType.matches(File.class)) {
			return temporaryDirectory;
		}
		if (targetType.matches(Path.class)) {
			return temporaryDirectoryPath;
		}
		if (targetType.matches(String.class)) {
			return temporaryDirectory.getAbsolutePath();
		}
		throw new ParameterResolutionException(
			"Can only resolve @" + annotation().getSimpleName()
				+ " parameter for File, Path, or String");
	}

	private static void delete(Path path) throws IOException {
		path = path.toAbsolutePath();
		if (Files.notExists(path) && !Files.isSymbolicLink(path)) {
			return;
		}
		if (path.equals(path.getRoot()))
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		Files.walkFileTree(path, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				try {
					Files.delete(file);
				} catch (IOException e) {
					throw exc;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) { // directory iteration failed
					throw exc;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static Path mkdirs(Path dir) throws IOException {
		if (Files.isSymbolicLink(dir)) {
			Path target = Files.readSymbolicLink(dir);
			boolean recreateSymlink = (File.separatorChar == '\\') && !Files.exists(target, LinkOption.NOFOLLOW_LINKS);
			Path result = mkdirs(target);
			if (recreateSymlink) { // recreate symlink on windows
				delete(dir);
				createSymbolicLink(dir, target);
			}
			return result;
		}
		if (Files.isDirectory(dir)) {
			return dir;
		}
		return Files.createDirectories(dir);
	}

	private static void createSymbolicLink(Path link, Path target) throws IOException {
		if (Files.isSymbolicLink(link)) {
			Path linkTarget = Files.readSymbolicLink(link);
			if (target.equals(linkTarget)) {
				return;
			} else {
				Files.delete(link);
			}
		}

		try {
			Files.createSymbolicLink(link, target);
		} catch (Exception e) {
			// ignore
		}
	}
}
