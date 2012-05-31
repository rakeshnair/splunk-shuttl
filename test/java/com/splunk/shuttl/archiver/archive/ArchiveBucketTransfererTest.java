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

package com.splunk.shuttl.archiver.archive;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shuttl.archiver.fileSystem.ArchiveFileSystem;
import com.splunk.shuttl.archiver.model.Bucket;
import com.splunk.shuttl.testutil.TUtilsBucket;

@Test(groups = { "fast-unit" })
public class ArchiveBucketTransfererTest {

	private ArchiveFileSystem archive;
	private PathResolver pathResolver;
	private ArchiveBucketTransferer archiveBucketTransferer;

	@BeforeMethod
	public void setUp() {
		archive = mock(ArchiveFileSystem.class);
		pathResolver = mock(PathResolver.class);
		archiveBucketTransferer = new ArchiveBucketTransferer(archive, pathResolver);
	}

	@Test(groups = { "fast-unit" })
	public void transferBucketToArchive_givenValidBucketAndUri_putBucketWithArchiveFileSystem()
			throws IOException {
		Bucket bucket = TUtilsBucket.createBucket();
		URI destination = URI.create("file:/some/path");
		when(pathResolver.resolveArchivePath(bucket)).thenReturn(destination);
		archiveBucketTransferer.transferBucketToArchive(bucket);
		verify(archive).putFileAtomically(bucket.getDirectory(), destination);
	}
}
