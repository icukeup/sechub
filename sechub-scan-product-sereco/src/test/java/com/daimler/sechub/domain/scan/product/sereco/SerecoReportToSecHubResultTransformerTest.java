// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.product.sereco;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.daimler.sechub.domain.scan.AssertSecHubResult;
import com.daimler.sechub.domain.scan.SecHubCodeCallStack;
import com.daimler.sechub.domain.scan.SecHubFinding;
import com.daimler.sechub.domain.scan.SecHubResult;
import com.daimler.sechub.sereco.metadata.SerecoClassification;
import com.daimler.sechub.sereco.metadata.SerecoCodeCallStackElement;
import com.daimler.sechub.sereco.metadata.SerecoMetaData;
import com.daimler.sechub.sereco.metadata.SerecoSeverity;
import com.daimler.sechub.sereco.metadata.SerecoVulnerability;
import com.daimler.sechub.sharedkernel.util.JSONConverter;

public class SerecoReportToSecHubResultTransformerTest {

	private SerecoReportToSecHubResultTransformer transformerToTest;

	@Before
	public void before() {
		transformerToTest = new SerecoReportToSecHubResultTransformer();
	}

	@Test
	public void one_vulnerability_in_meta_results_in_one_finding() throws Exception {
		/* prepare */
		String converted = createMetaDataWithOneVulnerabilityFound();

		/* execute */
		SecHubResult result = transformerToTest.transform(converted);

		/* test */
		AssertSecHubResult.assertSecHubResult(result).hasFindings(1);
	}

	@Test
	public void one_vulnerability_as_code_in_meta_results_in_one_finding() throws Exception {
		/* prepare */
		String converted = createMetaDataWithOneVulnerabilityAsCodeFound();

		/* execute */
		SecHubResult result = transformerToTest.transform(converted);

		/* test */
		AssertSecHubResult.assertSecHubResult(result).hasFindings(1);
		SecHubFinding finding1 = result.getFindings().get(0);

		SecHubCodeCallStack code1 = finding1.getCode();
		assertNotNull(code1);
		assertEquals(Integer.valueOf(1),code1.getLine());
		assertEquals(Integer.valueOf(2),code1.getColumn());
		assertEquals("Location1",code1.getLocation());
		assertEquals("source1",code1.getSource());
		assertEquals("relevantPart1",code1.getRelevantPart());

		SecHubCodeCallStack code2 = code1.getCalls();
		assertNotNull(code2);
		assertEquals(Integer.valueOf(3),code2.getLine());
		assertEquals(Integer.valueOf(4),code2.getColumn());
		assertEquals("Location2",code2.getLocation());
		assertEquals("source2",code2.getSource());
		assertEquals("relevantPart2",code2.getRelevantPart());


	}

	@Test
	public void transformation_of_id_finding_description_severity_and_name_are_done() throws Exception {
		/* prepare */
		String converted = createMetaDataWithOneVulnerabilityFound();

		/* execute */
		SecHubResult result = transformerToTest.transform(converted);

		/* test */
		/* @formatter:off */
		AssertSecHubResult.assertSecHubResult(result).
			hasFindingWithId(1).
				hasDescription("desc1").
				hasSeverity(com.daimler.sechub.domain.scan.Severity.MEDIUM).
				hasName("type1");
		/* @formatter:on */
	}

	private String createMetaDataWithOneVulnerabilityFound() {
		SerecoMetaData data = new SerecoMetaData();
		List<SerecoVulnerability> vulnerabilities = data.getVulnerabilities();

		SerecoVulnerability v1 = new SerecoVulnerability();
		v1.setDescription("desc1");
		v1.setSeverity(SerecoSeverity.MEDIUM);
		v1.setType("type1");
		v1.setUrl("url1");

		SerecoClassification cl = v1.getClassification();
		cl.setCapec("capec1");

		vulnerabilities.add(v1);

		String converted = JSONConverter.get().toJSON(data);
		return converted;
	}

	private String createMetaDataWithOneVulnerabilityAsCodeFound() {
		SerecoMetaData data = new SerecoMetaData();
		List<SerecoVulnerability> vulnerabilities = data.getVulnerabilities();

		SerecoVulnerability v1 = new SerecoVulnerability();
		v1.setSeverity(SerecoSeverity.MEDIUM);
		v1.setType("type1");
		v1.setUrl("url1");

		SerecoCodeCallStackElement serecoCode1 = new SerecoCodeCallStackElement();
		serecoCode1.setLine(1);
		serecoCode1.setColumn(2);
		serecoCode1.setLocation("Location1");
		serecoCode1.setSource("source1");
		serecoCode1.setRelevantPart("relevantPart1");

		v1.setCode(serecoCode1);

		SerecoCodeCallStackElement serecoCode2 = new SerecoCodeCallStackElement();
		serecoCode2.setLine(3);
		serecoCode2.setColumn(4);
		serecoCode2.setLocation("Location2");
		serecoCode2.setSource("source2");
		serecoCode2.setRelevantPart("relevantPart2");

		serecoCode1.setCalls(serecoCode2);

		SerecoClassification cl = v1.getClassification();
		cl.setCapec("capec1");

		vulnerabilities.add(v1);

		String converted = JSONConverter.get().toJSON(data);
		return converted;
	}

}
