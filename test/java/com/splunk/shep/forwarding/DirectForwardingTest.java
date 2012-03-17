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
package com.splunk.shep.forwarding;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.splunk.Args;
import com.splunk.Index;
import com.splunk.InputCollection;
import com.splunk.InputKind;
import com.splunk.Job;
import com.splunk.SavedSearch;
import com.splunk.SavedSearchCollection;
import com.splunk.Service;
import com.splunk.shep.testutil.FileSystemUtils;
import com.splunk.shep.testutil.SplunkServiceParameters;
import com.splunk.shep.testutil.SplunkTestUtils;


public class DirectForwardingTest {
    private static final String TEST_INPUT_FILENAME = "smallsyslog.log";
    private FileSystem fileSystem;
    private Service splunkService;
    private SplunkServiceParameters splunkServiceParams;

    private void waitForShepEventIndexing(int seconds) {
	while (seconds > 0) {
	    try {
		// 20000 ms (20 second sleep)
		Thread.sleep(20000);
		seconds = seconds - 20;
		String query = "search index=_internal source=Shep earliest=-2m";
		Job job = splunkService.getJobs().create(query, null);
	        SplunkTestUtils.waitWhileJobFinishes(job);
	        if (job.getEventCount() > 0) {
	           return; 
		}
	    } catch (InterruptedException e) {
		continue;
	    }
	}
	String msg = "No internal shep data was indexed in the past " + seconds
		+ " seconds";
	throw new RuntimeException(msg);
    }

    @Parameters({ "splunk.host", "splunk.mgmtport", "splunk.username",
	    "splunk.password" })
    @BeforeClass(groups = { "defunct" })
    public void setUp(String splunkHost, String splunkMgmtPort,
	    String splunkUser, String splunkPass) throws IOException {
	System.out.println("SetUp for Direct forwarding tests");
	splunkServiceParams = new SplunkServiceParameters(splunkUser,
		splunkPass, splunkHost, splunkMgmtPort, "shep");
	splunkService = splunkServiceParams.getLoggedInService();
	// TODO: set up appending for splunk
    }

    @Test(groups = { "defunct" })
    public void monitorFileInSplunk() throws InterruptedException {
	System.out.println("Running monitorFileInSplunk");
        String indexName = "directfwd";
	Index index = SplunkTestUtils.createSplunkIndex(splunkService,
		indexName);

        InputCollection inputs = splunkService.getInputs();
	File inputFile = new File(SplunkTestUtils.TEST_RESOURCES_PATH
		+ File.separator + TEST_INPUT_FILENAME);
	String name = inputFile.getAbsolutePath();
        Args args = new Args();
        args.put("sourcetype", "syslog");
        args.put("index", indexName);
        inputs.refresh();
        inputs.create(name, InputKind.Monitor, args);
        inputs.refresh();

        // wait at most 1 minute for indexing to complete
	SplunkTestUtils.waitForIndexing(index, 100, 60);
	assertEquals(index.getTotalEventCount(), 100);

        // check that events are searchable
        String query = "search index=directfwd";
        Job job;
        job = splunkService.getJobs().create(query, null);
        SplunkTestUtils.waitWhileJobFinishes(job);
	assertEquals(job.getEventCount(), 100);
	// Wait at most 2 minutes for events to get forwarded
	waitForShepEventIndexing(120);
    }

    @Test(groups = { "defunct" }, dependsOnMethods = { "monitorFileInSplunk" })
    public void checkTotalEventsSearch() throws IOException {
        SavedSearchCollection savedSearches = splunkService.getSavedSearches();
	assertTrue(savedSearches.containsKey("HC total events"));
        SavedSearch savedSearch = savedSearches.get("HC total events");
        Job job = savedSearch.dispatch();
        SplunkTestUtils.waitWhileJobFinishes(job);

	assertNotEquals(job.getResultCount(), 0);
        InputStream is = job.getResults(new Args("output_mode", "json"));
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String,Integer>> results = mapper.readValue(is, 
        	new TypeReference<List<HashMap<String,Integer>>>() { });
	assertEquals(results.size(), 1, "Search doesn't return 1 result");
	assertTrue(results.get(0).containsKey("Total Events"),
		"Total Events key doesn't exist in search results");
        int totalEvents = results.get(0).get("Total Events");
	assertEquals(totalEvents, 100,
		"HC total events saved search returns incorrect results");
    }

    @Parameters({ "hadoop.host", "hadoop.port" })
    @Test(groups = { "defunct" }, dependsOnMethods = { "monitorFileInSplunk" })
    public void checkEventCountInHdfs(String hadoopHost, String hadoopPort) 
	    throws IOException, URISyntaxException {
	fileSystem = FileSystemUtils.getRemoteFileSystem(hadoopHost,
		hadoopPort);
	URI hdfsFile = new URI("hdsf", null, hadoopHost,
		Integer.parseInt(hadoopPort), "/splunkeventdata*", null, null);
	Path pattern = new Path(hdfsFile);
	assertTrue(fileSystem != null, "fileSystem is null");
	FileStatus fs[] = fileSystem.globStatus(pattern);
	assertTrue(fs.length > 0,
		"No files exist which match the pattern: " + pattern);
	FileStatus latest = fs[0];
	for (int i = 1; i < fs.length; i++) {
	    if (fs[i].getModificationTime() > latest.getModificationTime()) {
		latest = fs[i];
	    }
	}

	FSDataInputStream open = fileSystem.open(latest.getPath());
	List<String> readLines = IOUtils.readLines(open);
	// count the number of events, each event has the word "body" in it
	int numberOfEvents = 0;
	for (String line : readLines) {
	    if (line.contains("body")) {
		numberOfEvents = numberOfEvents + 1;
	    }
	}
	String msg = "HADOOP-271: Incorrect number of events in " + latest.getPath() + ":"
		+ readLines;
	assertEquals(numberOfEvents, 100, msg);
    }

}