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

import static org.osgi.test.common.inject.FieldInjector.findAnnotatedFields;
import static org.osgi.test.common.inject.FieldInjector.findAnnotatedNonStaticFields;
import static org.osgi.test.common.inject.FieldInjector.setField;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TemporaryDirectoryExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {
		List<Field> fields = findAnnotatedFields(extensionContext.getRequiredTestClass(),
			InjectTemporaryDirectory.class,
			m -> Modifier.isStatic(m.getModifiers()));

		fields.forEach(field -> {
			assertValidFieldCandidate(field);
			InjectTemporaryDirectory annotation = field.getAnnotation(InjectTemporaryDirectory.class);
			Class<?> targetType = field.getType();
			setField(field, null, resolveReturnValue(targetType, annotation, extensionContext));
		});
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws Exception {
		for (Object instance : extensionContext.getRequiredTestInstances()
			.getAllInstances()) {
			List<Field> fields = findAnnotatedNonStaticFields(instance.getClass(), InjectTemporaryDirectory.class);

			fields.forEach(field -> {
				assertValidFieldCandidate(field);
				InjectTemporaryDirectory annotation = field.getAnnotation(InjectTemporaryDirectory.class);
				Class<?> targetType = field.getType();
				setField(field, instance, resolveReturnValue(targetType, annotation, extensionContext));
			});
		}
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
		throws ParameterResolutionException {
		Optional<InjectTemporaryDirectory> annotation = parameterContext
			.findAnnotation(InjectTemporaryDirectory.class);
		Parameter parameter = parameterContext.getParameter();
		Class<?> targetType = parameter.getType();
		return resolveReturnValue(targetType, annotation.get(), extensionContext);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
		throws ParameterResolutionException {
		if (!parameterContext.isAnnotated(InjectTemporaryDirectory.class)) {
			return false;
		}
		Parameter parameter = parameterContext.getParameter();
		Class<?> targetType = parameter.getType();
		if (targetType.isAssignableFrom(File.class) || targetType.isAssignableFrom(Path.class)
			|| targetType.isAssignableFrom(String.class)) {
			return true;
		}
		throw new ParameterResolutionException("Can only resolve @" + InjectTemporaryDirectory.class.getSimpleName()
			+ " parameter for File, Path, or String");
	}

	static void assertValidFieldCandidate(Field field) {
		if (Modifier.isFinal(field.getModifiers()) || Modifier.isPrivate(field.getModifiers())) {
			throw new ExtensionConfigurationException("@" + InjectTemporaryDirectory.class.getSimpleName() + " field ["
				+ field.getName() + "] must not be final or private.");
		}
	}

	static Object resolveReturnValue(Class<?> targetType, InjectTemporaryDirectory annotation,
		ExtensionContext extensionContext) throws ParameterResolutionException {

		// supportsParameter() If Jupiter does the right thing then this method
		// should not be called with an incorrect type
		assert targetType.isAssignableFrom(File.class) || targetType.isAssignableFrom(Path.class)
			|| targetType.isAssignableFrom(String.class);

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

		if (targetType.isAssignableFrom(File.class)) {
			return temporaryDirectory;
		} else if (targetType.isAssignableFrom(Path.class)) {
			return temporaryDirectoryPath;
		} else if (targetType.isAssignableFrom(String.class)) {
			return temporaryDirectory.getAbsolutePath();
		}
		throw new ParameterResolutionException(
			"Can only resolve @" + InjectTemporaryDirectory.class.getSimpleName()
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
