// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.config;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.daimler.sechub.sharedkernel.MustBeKeptStable;
import com.daimler.sechub.sharedkernel.util.JSONable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // we do ignore to avoid problems from wrong configured values!
@MustBeKeptStable("This configuration is used by users to schedule a job. It has to be backward compatible. To afford this we will NOT remove older parts since final API releases")
public class ScanConfig implements JSONable<ScanConfig> {

	private String apiVersion;

	private static final ScanConfig JSON_INITIALIZER = new ScanConfig();

	private Map<String, List<NamePatternToIdEntry>> namePatternMappings = new TreeMap<>();

	public static ScanConfig createFromJSON(String json) {
		return JSON_INITIALIZER.fromJSON(json);
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public Map<String, List<NamePatternToIdEntry>> getNamePatternMappings() {
		return namePatternMappings;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	@Override
	public Class<ScanConfig> getJSONTargetClass() {
		return ScanConfig.class;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (! (obj instanceof ScanConfig)) {
	        return false;
	    }
	    ScanConfig other = (ScanConfig) obj;
	    
	    return toJSON().equals(other.toJSON());
	}

}
