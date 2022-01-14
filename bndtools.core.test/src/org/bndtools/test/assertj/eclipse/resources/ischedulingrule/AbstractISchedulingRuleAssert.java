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

package org.bndtools.test.assertj.eclipse.resources.ischedulingrule;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public abstract class AbstractISchedulingRuleAssert<SELF extends AbstractISchedulingRuleAssert<SELF, ACTUAL>, ACTUAL extends ISchedulingRule>
	extends AbstractAssert<SELF, ACTUAL> {

	protected AbstractISchedulingRuleAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF containsRule(ISchedulingRule other) {
		isNotNull();
		if (!actual.contains(other)) {
			throw failure("%nExpecting scheduling rule:%n <%s>%nto contain scheduling rule:%n  <%s>%n but it does not",
				actual, other);
		}
		return myself;
	}

	public SELF doesNotContainRule(ISchedulingRule other) {
		isNotNull();
		if (actual.contains(other)) {
			throw failure("%nExpecting scheduling rule:%n <%s>%nto not contain scheduling rule:%n  <%s>%n but it does",
				actual, other);
		}
		return myself;
	}

	public SELF conflictsWithRule(ISchedulingRule other) {
		isNotNull();
		if (!actual.isConflicting(other)) {
			throw failure(
				"%nExpecting scheduling rule:%n <%s>%nto conflict with scheduling rule:%n  <%s>%n but it does not",
				actual, other);
		}
		return myself;
	}

	public SELF doesNotConflictWithRule(ISchedulingRule other) {
		isNotNull();
		if (actual.isConflicting(other)) {
			throw failure(
				"%nExpecting scheduling rule:%n <%s>%nto not conflict with scheduling rule:%n  <%s>%n but it does",
				actual, other);
		}
		return myself;
	}
}
