// Copyright (C) 2011 Splunk Inc.
//
// Splunk Inc. licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.splunk.shuttl.archiver.thaw;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shuttl.archiver.archive.BucketFormat;
import com.splunk.shuttl.archiver.archive.PathResolver;
import com.splunk.shuttl.archiver.filesystem.ArchiveFileSystem;
import com.splunk.shuttl.archiver.model.Bucket;
import com.splunk.shuttl.testutil.TUtilsBucket;

@Test(groups = { "fast-unit" })
public class BucketFormatResolverTest {

	BucketFormatResolver bucketFormatResolver;
	PathResolver pathResolver;
	ArchiveFileSystem archiveFileSystem;
	BucketFormatChooser bucketFormatChooser;

	List<Bucket> mockedBucketsList;

	@BeforeMethod
	public void setUp() {
		mockedBucketsList = Arrays.asList(createBucketWithMockedURI());

		pathResolver = mock(PathResolver.class);
		archiveFileSystem = mock(ArchiveFileSystem.class);
		bucketFormatChooser = mock(BucketFormatChooser.class);
		bucketFormatResolver = new BucketFormatResolver(pathResolver,
				archiveFileSystem, bucketFormatChooser);
	}

	private Bucket createBucketWithMockedURI() {
		Bucket bucketWithMockedURI = mock(Bucket.class);
		URI uri = URI.create("valid:/uri");
		when(bucketWithMockedURI.getURI()).thenReturn(uri);
		return bucketWithMockedURI;
	}

	@Test(groups = { "fast-unit" })
	public void resolveBucketsFormats_givenFormatsHomeForBucket_listFormatsHomeInArchiveFileSystem()
			throws IOException {
		URI formatsHome = URI.create("valid:/uri");
		when(pathResolver.getFormatsHome(anyString(), anyString())).thenReturn(
				formatsHome);
		bucketFormatResolver.resolveBucketsFormats(mockedBucketsList);
		verify(archiveFileSystem).listPath(formatsHome);
	}

	public void resolveBucketsFormats_givenFormatUris_directoryNameIsFormat()
			throws IOException {
		BucketFormat format = BucketFormat.SPLUNK_BUCKET;
		URI formatURI = URI.create("valid:/uri/" + format);
		when(archiveFileSystem.listPath(any(URI.class))).thenReturn(
				Arrays.asList(formatURI));
		bucketFormatResolver.resolveBucketsFormats(mockedBucketsList);
		verify(bucketFormatChooser).chooseBucketFormat(Arrays.asList(format));
	}

	@SuppressWarnings("unchecked")
	public void resolveBucketsFormats_givenChosenFormat_resolvingUriForBucketWithFormat() {
		Bucket bucket = TUtilsBucket.createBucket();
		BucketFormat format = BucketFormat.SPLUNK_BUCKET;
		when(bucketFormatChooser.chooseBucketFormat(anyList())).thenReturn(format);

		bucketFormatResolver.resolveBucketsFormats(Arrays.asList(bucket));

		verify(pathResolver).resolveArchivedBucketURI(bucket.getIndex(),
				bucket.getName(), format);
	}

	@SuppressWarnings("unchecked")
	public void resolveBucketsFormats_givenBucketToResolveForAndChosenFormat_bucketWithFormatIndexAndName() {
		Bucket bucket = TUtilsBucket.createBucket();
		BucketFormat format = BucketFormat.SPLUNK_BUCKET;
		when(bucketFormatChooser.chooseBucketFormat(anyList())).thenReturn(format);
		List<Bucket> bucketsWithFormat = bucketFormatResolver
				.resolveBucketsFormats(Arrays.asList(bucket));

		// Verify
		assertEquals(1, bucketsWithFormat.size());
		assertEquals(format, bucketsWithFormat.get(0).getFormat());
		assertEquals(bucket.getIndex(), bucketsWithFormat.get(0).getIndex());
		assertEquals(bucket.getName(), bucketsWithFormat.get(0).getName());
	}

	public void resolveBucketsFormats_givenUriToBucketWithResolvedFormat_bucketWithUri() {
		URI uri = URI.create("valid:/uri/to/bucket/with/new/format");
		when(
				pathResolver.resolveArchivedBucketURI(anyString(), anyString(),
						any(BucketFormat.class))).thenReturn(uri);

		List<Bucket> bucketsWithFormat = bucketFormatResolver
				.resolveBucketsFormats(mockedBucketsList);
		assertEquals(1, bucketsWithFormat.size());
		assertEquals(uri, bucketsWithFormat.get(0).getURI());
	}
}
