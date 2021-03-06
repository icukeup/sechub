// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.admin.importer;

import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.daimler.sechub.developertools.DeveloperToolsTestFileSupport;
import com.daimler.sechub.developertools.admin.DeveloperAdministration;

public class UnassignUserFromProjectMassCSVImporterTest {

	private UnassignUserToProjectMassCSVImporter importerToTest;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private DeveloperAdministration administration;

	@Before
	public void before() {
		administration = mock(DeveloperAdministration.class);
		importerToTest = new UnassignUserToProjectMassCSVImporter(administration);
	}

	@Test
	public void example_3_user2projects_can_be_imported() throws Exception {
		/* prepare */
		File file = DeveloperToolsTestFileSupport.getTestfileSupport().createFileFromResourcePath("csv/example4-developer-admin-ui_mass-import_user2projects-unassign.csv");

		/* execute */
		importerToTest.importUsersFromProjectUnassignmentsByCSV(file);

		/* test */
		verify(administration).unassignUserFromProject("scenario2_user1", "testproject_1");

		verify(administration).unassignUserFromProject("scenario2_user2", "testproject_2");
		verify(administration,never()).unassignUserFromProject("scenario2_user1", "testproject_2");

		verify(administration).unassignUserFromProject("scenario2_user1", "testproject_3");
		verify(administration).unassignUserFromProject("scenario2_user2", "testproject_3");

	}
}
