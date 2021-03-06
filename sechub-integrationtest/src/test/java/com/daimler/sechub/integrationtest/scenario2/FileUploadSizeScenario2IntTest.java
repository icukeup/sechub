// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.scenario2;

import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static com.daimler.sechub.integrationtest.scenario2.Scenario2.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.springframework.web.client.HttpClientErrorException.NotAcceptable;

import com.daimler.sechub.integrationtest.api.IntegrationTestSetup;
import com.daimler.sechub.sharedkernel.util.FileChecksumSHA256Service;

public class FileUploadSizeScenario2IntTest {

	@Rule
	public IntegrationTestSetup setup = IntegrationTestSetup.forScenario(Scenario2.class).markLongRunning();

	@Rule
	public Timeout timeOut = Timeout.seconds(240);

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private FileChecksumSHA256Service checksumSHA256Service;

	/**
	 * Generate big zip file and violate file size limit
	 *
	 * @throws IOException
	 */
	@Test
	public void when_file_exceeds_5MB_a_NOT_ACCEPTABLE_is_returned() throws IOException {
		/* @formatter:off */
		handleBigUpload(true);
	}

	/**
	 * Generate maximum allowed big zip file - so not violate
	 *
	 * @throws IOException
	 */
	@Test
	public void when_file_exceeds_NOT_5MB_no_exception_is_thrown() throws IOException {
		/* @formatter:off */
		handleBigUpload(false);
	}

	private void handleBigUpload(boolean tooBig) throws FileNotFoundException, IOException {
		/* prepare */
		checksumSHA256Service = new FileChecksumSHA256Service();
		as(SUPER_ADMIN).
			assignUserToProject(USER_1, PROJECT_1);

		UUID jobUUID = assertUser(USER_1).
					doesExist().
					isAssignedToProject(PROJECT_1).
					canCreateWebScan(PROJECT_1);

		File largeFile = createZipFileContainingMegabytes(tooBig);

		/* test */
		if (tooBig) {
			expected.expect(NotAcceptable.class);
		}else {
			/* nothing - means expected no exception at all!*/
		}

		/* execute */
		as(USER_1).
			upload(PROJECT_1, jobUUID, largeFile, checksumSHA256Service.createChecksum(largeFile.getAbsolutePath()));
		/* @formatter:on */
	}

	/**
	 * A little bit tricky: ZipFile content differs from file size. Also multipart
	 * upload contains not only the file but meta information as well (e.g.
	 * filename, sha256checksum,..)
	 */
	private File createZipFileContainingMegabytes(boolean uploadShallBeTooLarge) throws FileNotFoundException, IOException {
		String tmpPath = "build/resources/bigFile";
		if (uploadShallBeTooLarge) {
		    tmpPath += "-too-large";
		} else {
		    tmpPath += "-accepted";
		}
		String tmpZipFilePath = tmpPath+ ".zip";

		int maximumUploadSizeInMB = 5;
		int maximumUploadSizeInBytes = 1024 * 1024 * maximumUploadSizeInMB;
		int bytesToOrder = maximumUploadSizeInBytes;
		if (!uploadShallBeTooLarge) {
			bytesToOrder = bytesToOrder - (3 * 1024); // we reduce 3kb (includes zipfile overhead, filename on multipart and sha256
														// checksum on upload)
		}
		byte[] content = new byte[bytesToOrder];
		try (FileOutputStream fileOutputStream = new FileOutputStream(tmpZipFilePath);
				ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream));) {

			ZipEntry zipEntry = new ZipEntry("test.bin");
			// Set compression level to minimum to generate big zip file
			zipOutputStream.setLevel(0);
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(content);
			zipOutputStream.flush();
		}
		File file = new File(tmpZipFilePath);
		if (uploadShallBeTooLarge && file.length() < maximumUploadSizeInBytes) {
			throw new IllegalStateException("Wanted at least file size: " + maximumUploadSizeInBytes + " but was:" + file.length());
		}
		if (!uploadShallBeTooLarge && file.length() >= maximumUploadSizeInBytes) {
			throw new IllegalStateException("Wanted a maximum file size: " + (maximumUploadSizeInBytes - (3 * 1024)) + " but was:" + file.length());
		}
		return file;
	}
}
