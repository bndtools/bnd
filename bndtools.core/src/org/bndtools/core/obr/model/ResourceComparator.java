/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.bndtools.core.obr.model;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.felix.bundlerepository.Resource;

class ResourceComparator implements Comparator<Resource>, Serializable {
    private static final long serialVersionUID = -1024310463155368274L;

    public int compare(Resource r1, Resource r2) {
        String name1 = r1.getPresentationName();
        String name2 = r2.getPresentationName();
        if (name1 == null) {
            return -1;
        } else if (name2 == null) {
            return 1;
        }
        return name1.compareToIgnoreCase(name2);
    }
}