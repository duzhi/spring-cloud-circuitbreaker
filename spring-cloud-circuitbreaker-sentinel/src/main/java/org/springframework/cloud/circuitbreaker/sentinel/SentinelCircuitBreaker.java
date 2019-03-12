/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.circuitbreaker.sentinel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;

import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;
import org.springframework.util.Assert;

/**
 * Sentinel implementation of {@link CircuitBreaker}.
 *
 * @author Eric Zhao
 */
public class SentinelCircuitBreaker implements CircuitBreaker {

	private final String resourceName;
	private final EntryType entryType;

	private final List<DegradeRule> rules;

	public SentinelCircuitBreaker(String resourceName, EntryType entryType, List<DegradeRule> rules) {
		Assert.hasText(resourceName, "resourceName cannot be blank");
		Assert.notNull(rules, "rules should not be null");
		this.resourceName = resourceName;
		this.entryType = entryType;
		this.rules = Collections.unmodifiableList(rules);

		applyToSentinelRuleManager();
	}

	public SentinelCircuitBreaker(String resourceName, List<DegradeRule> rules) {
		this(resourceName, EntryType.OUT, rules);
	}

	public SentinelCircuitBreaker(String resourceName) {
		this(resourceName, EntryType.OUT, Collections.emptyList());
	}

	private void applyToSentinelRuleManager() {
		Set<DegradeRule> ruleSet = new HashSet<>(DegradeRuleManager.getRules());
		ruleSet.addAll(this.rules);
		DegradeRuleManager.loadRules(new ArrayList<>(ruleSet));
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		Entry entry = null;
		try {
			entry = SphU.entry(resourceName, entryType);
			return toRun.get();
		}
		catch (BlockException ex) {
			return fallback.apply(ex);
		}
		catch (Exception ex) {
			Tracer.trace(ex);
			return fallback.apply(ex);
		}
		finally {
			if (entry != null) {
				entry.exit();
			}
		}
	}
}
