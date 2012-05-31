// Copyright (C) 2011 Splunk Inc.
//
// Splunk Inc. licenses this file
// to you under the Apache License, Version 2.0 (the
// License); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an AS IS BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.splunk.shuttl.archiver.importexport;

import static com.splunk.shuttl.testutil.TUtilsFile.*;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

import java.io.File;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shuttl.archiver.archive.ArchiveConfiguration;
import com.splunk.shuttl.archiver.archive.BucketFormat;
import com.splunk.shuttl.archiver.archive.UnknownBucketFormatException;
import com.splunk.shuttl.archiver.importexport.csv.CsvBucketCreator;
import com.splunk.shuttl.archiver.importexport.csv.CsvExporter;
import com.splunk.shuttl.archiver.model.Bucket;
import com.splunk.shuttl.testutil.TUtilsBucket;

@Test(groups = { "fast-unit" })
public class BucketExporterTest {

	BucketExporter bucketExporter;
	CsvExporter csvExporter;
	CsvBucketCreator csvBucketCreator;
	ArchiveConfiguration config;

	@BeforeMethod(groups = { "fast-unit" })
	public void setUp() {
		config = mock(ArchiveConfiguration.class);
		csvExporter = mock(CsvExporter.class);
		csvBucketCreator = mock(CsvBucketCreator.class);
		bucketExporter = new BucketExporter(config, csvExporter, csvBucketCreator);
	}

	@Test(groups = { "fast-unit" })
	public void exportBucketToFormat_whenBucketIsAlreadyInThatFormat_returnTheSameBucket() {
		Bucket bucket = mock(Bucket.class);
		when(bucket.getFormat()).thenReturn(BucketFormat.SPLUNK_BUCKET);
		stubArchiveFormat(BucketFormat.SPLUNK_BUCKET);
		Bucket exportedToFormat = bucketExporter.exportBucket(bucket);
		assertSame(bucket, exportedToFormat);
	}

	private void stubArchiveFormat(BucketFormat archiveFormat) {
		when(config.getArchiveFormat()).thenReturn(archiveFormat);
	}

	@Test(expectedExceptions = { UnknownBucketFormatException.class })
	public void exportBucketToFormat_formatIsUnknown_throwUnknownBucketFormatException() {
		Bucket bucket = mock(Bucket.class);
		stubArchiveFormat(BucketFormat.UNKNOWN);
		bucketExporter.exportBucket(bucket);
	}

	public void exportBucketToFormat_exportsSplunkBucketWithCsvExporter_createsAndReturnsBucketFromCsvFile() {
		Bucket bucket = TUtilsBucket.createBucket();
		File csvFile = createFile();
		when(csvExporter.exportBucketToCsv(bucket)).thenReturn(csvFile);
		Bucket csvBucket = mock(Bucket.class);
		when(csvBucketCreator.createBucketWithCsvFile(csvFile, bucket)).thenReturn(
				csvBucket);
		stubArchiveFormat(BucketFormat.CSV);
		Bucket newBucket = bucketExporter.exportBucket(bucket);
		assertEquals(csvBucket, newBucket);
	}

	public void exportBucketToFormat_bucketIsUnknownAndExportingToCsv_throwsUnsupportedOperationException() {
		Bucket unknownFormatedBucket = mock(Bucket.class);
		when(unknownFormatedBucket.getFormat()).thenReturn(BucketFormat.UNKNOWN);
		stubArchiveFormat(BucketFormat.CSV);
		try {
			bucketExporter.exportBucket(unknownFormatedBucket);
			fail();
		} catch (UnsupportedOperationException e) {
		}
		verifyZeroInteractions(csvExporter);
	}
}
