/*-
 * #%L
 * Slice - Core
 * %%
 * Copyright (C) 2012 Cognifide Limited
 * %%
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
 * #L%
 */
package com.cognifide.slice.core.internal.monitoring;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.cognifide.slice.api.injector.InjectorConfig;
import com.cognifide.slice.core.internal.injector.InjectorHierarchy;
import com.google.inject.Injector;

/**
 * Facade providing interface for accessing statistics data for all injectors and delegating settings changes
 * to InjectorStatisticsRepositories.
 * 
 * @author Jakub Przybytek, Jacek Fohs
 */

@Component
@Service(SliceStatistics.class)
public class SliceStatistics {

	@Reference
	private InjectorHierarchy injectorHierarchy;

	/**
	 * Collects all statistics available and groups them by injector hierarchy String. Injector hierarchy is
	 * resolved to String by joing its name with its consecutive parents names.
	 * 
	 * @return map mapping injetors hierarchy string to related model injection roots; see
	 * {@link ModelUsageData}.
	 */
	public Map<String, ModelUsageData> collectStatistics() {
		Map<String, ModelUsageData> statsHistory = new TreeMap<String, ModelUsageData>();

		for (String injectorName : injectorHierarchy.getInjectorNames()) {
			ModelUsageData injectorStats = getStatistics(injectorHierarchy, injectorName);
			String injectorFullName = resolveInjectoNameInheritanceStructure(injectorName, injectorHierarchy);

			if (!injectorStats.getSubModels().isEmpty()) {
				statsHistory.put(injectorFullName, injectorStats);
			}
		}

		return statsHistory;
	}

	private static ModelUsageData getStatistics(InjectorHierarchy injectorHierarchy, String injectorName) {
		Injector injector = injectorHierarchy.getInjectorByName(injectorName);
		InjectorStatisticsRepository statisticsRepository = injector.getInstance(InjectorStatisticsRepository.class);
		return statisticsRepository.getModelUsageDataRoot();
	}

	private String resolveInjectoNameInheritanceStructure(String injectorName, InjectorHierarchy injectorHierarchy) {
		Deque<String> injectorNamesStructure = new ArrayDeque<String>();

		String currentInjectorName = injectorName;
		injectorNamesStructure.push(currentInjectorName);

		InjectorConfig currentInjectorConfig = injectorHierarchy.getInjectorConfigByName(currentInjectorName);
		while (currentInjectorConfig.hasParent()) {
			currentInjectorName = currentInjectorConfig.getParentName();
			injectorNamesStructure.push(currentInjectorName);

			currentInjectorConfig = injectorHierarchy.getInjectorConfigByName(currentInjectorName);
		}

		return StringUtils.join(injectorNamesStructure, " > ");
	}

	/**
	 * Enables or disables statistics monitoring. Statistics collected so far are not affected.
	 * 
	 * @param statisticsEnabled flag indicating whether to enable or disable statistics monitoring.
	 */
	public void updateStatisticsRepositories(boolean statisticsEnabled) {
		for (String injectorName : injectorHierarchy.getInjectorNames()) {
			injectorHierarchy.getInjectorByName(injectorName).getInstance(InjectorStatisticsRepository.class)
					.setEnabled(statisticsEnabled);
		}
	}

	/**
	 * Deletes all statistics collected so far.
	 */
	public void reset() {
		for (String injectorName : injectorHierarchy.getInjectorNames()) {
			injectorHierarchy.getInjectorByName(injectorName).getInstance(InjectorStatisticsRepository.class).clear();
		}
	}
}