// SPDX-License-Identifier: MIT
package com.daimler.sechub.adapter.checkmarx.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;

import com.daimler.sechub.adapter.AdapterException;
import com.daimler.sechub.adapter.checkmarx.CheckmarxAdapterConfig;
import com.daimler.sechub.adapter.checkmarx.CheckmarxContext;
import com.daimler.sechub.adapter.checkmarx.CheckmarxSastScanSettings;
import com.daimler.sechub.adapter.checkmarx.CheckmarxSessionData;
import com.daimler.sechub.adapter.support.JSONAdapterSupport;
import com.daimler.sechub.adapter.support.JSONAdapterSupport.Access;

public class CheckmarxProjectSupport {

	private static final Logger LOG = LoggerFactory.getLogger(CheckmarxProjectSupport.class);

	public void ensureProjectExists(CheckmarxContext context) throws AdapterException {
		CheckmarxAdapterConfig config = context.getConfig();
		String projectName = config.getProjectId();
		String teamId = config.getTeamIdForNewProjects();

		Map<String, String> map = new LinkedHashMap<>();
		map.put("projectName", projectName);
		map.put("teamId", teamId);
		String url = context.getAPIURL("projects", map);
		RestOperations restTemplate = context.getRestOperations();

		// https://checkmarx.atlassian.net/wiki/spaces/KC/pages/814285654/Swagger+Examples+v8.8.0+-+v2
		// https://checkmarx.atlassian.net/wiki/spaces/KC/pages/564330665/Get+All+Project+Details+-+GET+projects+v8.8.0+and+up
		// example:
		// CxRestAPI/projects?projectName=myProject&teamId=00000000-1111-1111-b111-989c9070eb11
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			context.setSessionData(extractFirstProjectFromJsonWithProjectArray(context.json(), response.getBody()));
			context.setNewProject(false);
			return;
		} catch (HttpStatusCodeException e) {
			if (e.getRawStatusCode() != 404) {
				/* only 404 - not found is accepted */
				throw context.asAdapterException("Unexpected HTTP status error", e);
			}
		}
		/* 404 error - okay, lets create */
		context.setSessionData(createProject(context));
		context.setNewProject(true);
	}

	private CheckmarxSessionData createProject(CheckmarxContext context) throws AdapterException {
		CheckmarxAdapterConfig config = context.getConfig();
		String projectName = config.getProjectId();
		String teamId = config.getTeamIdForNewProjects();

		Map<String, String> json = new TreeMap<>();
		json.put("name", projectName);
		json.put("owningTeam", teamId);
		json.put("isPublic", "false");

		String url = context.getAPIURL("projects");
		String jsonAsString = context.json().toJSON(json);
		RestOperations restTemplate = context.getRestOperations();

		// https://checkmarx.atlassian.net/wiki/spaces/KC/pages/222265747/Create+Project+with+Default+Configuration+-+POST+projects
		// https://checkmarx.atlassian.net/wiki/spaces/KC/pages/814285654/Swagger+Examples+v8.8.0+-+v2
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.set("Content-Type", "application/json;v=2.0");

		HttpEntity<String> request = new HttpEntity<>(jsonAsString, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
		CheckmarxSessionData sessionData = extractProjectFromJsonWithProjectCreationData(projectName, context.json(), response.getBody());

		updatePresetIdWhenNecessary(context, sessionData);

		return sessionData;
	}

	private void updatePresetIdWhenNecessary(CheckmarxContext context, CheckmarxSessionData sessionData) throws AdapterException {
		Long presetId = context.getConfig().getPresetIdForNewProjectsOrNull();
		if (presetId == null) {
			LOG.debug("No presetId defined, so keep default preset.");
			return;
		}
		LOG.debug("Wanted preset id {} for project {}", presetId, context.getConfig().getProjectId());
		CheckmarxSastScanSettings currentSettings = fetchCurrentSastScanSettings(context, sessionData);

		/* change preset id to wanted one */
		LOG.debug("Found old preset id for for project {}", currentSettings.getPresetId(), context.getConfig().getProjectId());

		updateSastScanSettings(context, presetId, currentSettings);

	}

	private void updateSastScanSettings(CheckmarxContext context, Long presetId, CheckmarxSastScanSettings currentSettings) throws AdapterException {
		MultiValueMap<String, String> headers3 = new LinkedMultiValueMap<>();
		headers3.set("Content-Type", "application/json;v=1.1");
		RestOperations restTemplate3 = context.getRestOperations();

		/* write */
		Map<String, Long> updateJSON = new TreeMap<>();
		updateJSON.put("projectId", currentSettings.getProjectId());
		updateJSON.put("presetId", presetId);
		updateJSON.put("engineConfigurationId", currentSettings.getEngineConfigurationId());

		String updateScanSettingsURL = context.getAPIURL("sast/scanSettings");

		String updateJSONAsString = context.json().toJSON(updateJSON);
		HttpEntity<String> request2 = new HttpEntity<>(updateJSONAsString, headers3);
		restTemplate3.put(updateScanSettingsURL, request2);

		LOG.debug("Did change preset id from {} t o {} for project {}", currentSettings.getPresetId(), presetId, context.getConfig().getProjectId());
	}

	private CheckmarxSastScanSettings fetchCurrentSastScanSettings(CheckmarxContext context, CheckmarxSessionData sessionData) throws AdapterException {
		CheckmarxSastScanSettings settings;
		// project scan settings cannot be defined at creation time.
		// see
		// https://checkmarx.atlassian.net/wiki/spaces/KC/pages/1140555950/CxSAST+REST+API+-+Swagger+Examples+v8.9.0+-+v1.1
		// https://checkmarx.atlassian.net/wiki/spaces/KC/pages/334299447/Get+Scan+Settings+by+Project+Id+-+GET+sast+scanSettings+projectId+v8.7.0+and+up
		MultiValueMap<String, String> headers2 = new LinkedMultiValueMap<>();
		headers2.set("Content-Type", "application/json;v=1.1");
		RestOperations restTemplate2 = context.getRestOperations();

		/* read current setup */
		String fetchScanSettingsURL = context.getAPIURL("sast/scanSettings/" + sessionData.getProjectId());
		ResponseEntity<String> scanSettingsResponse = restTemplate2.getForEntity(fetchScanSettingsURL, String.class);
		settings = extractSastScanSettingsFromGet(scanSettingsResponse.getBody(), context.json());
		return settings;
	}

	CheckmarxSessionData extractFirstProjectFromJsonWithProjectArray(JSONAdapterSupport support, String json) throws AdapterException {
		CheckmarxSessionData data = new CheckmarxSessionData();
		Access rootNode = support.fetchRootNode(json);
		Access first = support.fetchArray(0, rootNode.asArray());
		data.setProjectId(first.fetch("id").asLong());
		data.setProjectName(first.fetch("name").asText());
		return data;
	}

	CheckmarxSessionData extractProjectFromJsonWithProjectCreationData(String projectName, JSONAdapterSupport support, String json) throws AdapterException {
		CheckmarxSessionData data = new CheckmarxSessionData();
		Access rootNode = support.fetchRootNode(json);
		data.setProjectId(rootNode.fetch("id").asLong());
		data.setProjectName(projectName);
		return data;
	}

	// https://swagger-open-api.herokuapp.com/v1/swagger#/SAST/ScanSettingsV1_1__GetByprojectId
	CheckmarxSastScanSettings extractSastScanSettingsFromGet(String json, JSONAdapterSupport support) throws AdapterException {
		Access rootNode = support.fetchRootNode(json);

		CheckmarxSastScanSettings settings = new CheckmarxSastScanSettings();
		settings.setProjectId(rootNode.fetch("project").fetch("id").asLong());
		settings.setPresetId(rootNode.fetch("preset").fetch("id").asLong());
		settings.setEngineConfigurationId(rootNode.fetch("engineConfiguration").fetch("id").asLong());
		return settings;
	}
}
