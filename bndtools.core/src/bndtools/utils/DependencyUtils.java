/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

public class DependencyUtils {

    public static interface Processor<T> {
        void process(T obj, IProgressMonitor monitor) throws CoreException;
    }

    public static <T> void processDependencyMap(Collection<T> input, Map<T,Set<T>> dependencies, Processor<T> processor, IProgressMonitor monitor) throws CoreException, CircularDependencyException {
        SubMonitor progress = SubMonitor.convert(monitor, input.size());
        Set<T> processed = new TreeSet<T>();
        Stack<T> callStack = new Stack<T>();
        for (T selected : input) {
            processDependencyMap(selected, callStack, dependencies, processed, processor, progress);
        }
    }

    private static <T> void processDependencyMap(T selected, Stack<T> callStack, Map<T,Set<T>> dependencies, Set<T> processed, Processor<T> processor, SubMonitor subMonitor) throws CoreException, CircularDependencyException {
        if (processed.contains(selected))
            return;

        // Check for cycles
        int stackIndex = callStack.indexOf(selected);
        if (stackIndex != -1) {
            List<T> subList = callStack.subList(stackIndex, callStack.size());
            List<T> cycle = new ArrayList<T>(subList.size() + 1);
            cycle.addAll(subList);
            cycle.add(selected);

            throw new CircularDependencyException(cycle);
        }
        try {
            callStack.push(selected);

            Set<T> selectedDeps = dependencies.get(selected);

            // Process dependencies first
            if (selectedDeps != null) {
                for (T dep : selectedDeps) {
                    processDependencyMap(dep, callStack, dependencies, processed, processor, subMonitor);
                }
            }

            // Process the selection
            processor.process(selected, subMonitor.newChild(1));
            processed.add(selected);
        } finally {
            callStack.pop();
        }
    }
}
