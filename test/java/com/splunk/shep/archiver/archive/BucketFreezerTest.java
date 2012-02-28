package com.splunk.shep.archiver.archive;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shep.testutil.ShellClassRunner;

public class BucketFreezerTest {

    private ShellClassRunner shellClassRunner;

    @BeforeMethod(groups = { "slow" })
    public void setUp() {
	shellClassRunner = new ShellClassRunner();
    }

    @AfterMethod(groups = { "slow" })
    public void tearDown() {
	deleteDirectory(getTestDirectory());
    }

    private void deleteDirectory(File dir) {
	try {
	    FileUtils.deleteDirectory(dir);
	} catch (IOException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

    @Test(groups = { "fast" })
    public void testDirectory_shouldNot_exist() {
	assertFalse(getTestDirectory().exists());
    }

    @Test(groups = { "slow" })
    public void should_returnExitStatusZero_when_runWithOneArgument_where_theArgumentIsAnExistingDirectory() {
	File directory = createTestDirectory();
	assertEquals(0, runBucketFreezerMain(directory.getAbsolutePath()));
    }

    @Test(groups = { "slow" })
    public void should_returnExitStatus_1_when_runWithZeroArguments() {
	assertEquals(1, runBucketFreezerMain());
    }

    @Test(groups = { "slow" })
    public void should_returnExitStatus_2_when_runWithMoreThanOneArgument() {
	assertEquals(2, runBucketFreezerMain("one", "two"));
    }

    private int runBucketFreezerMain(String... args) {
	return shellClassRunner.runClassWithArgs(BucketFreezer.class, args)
		.getExitCode();
    }

    @Test(groups = { "slow" })
    public void should_returnExitStatus_3_when_runWithArgumentThatIsNotADirectory()
	    throws IOException {
	File file = File.createTempFile("ArchiveTest", ".tmp");
	file.deleteOnExit();
	assertTrue(!file.isDirectory());
	assertEquals(3, runBucketFreezerMain(file.getAbsolutePath()));
    }

    @Test(groups = { "fast" })
    public void should_moveDirectoryToaSafeLocation_when_givenPath()
	    throws IOException {
	String safeLocation = System.getProperty("user.home") + "/"
		+ getClass().getName();
	File safeLocationDirectory = new File(safeLocation);
	assertTrue(!safeLocationDirectory.exists());

	BucketFreezer bucketFreezer = new BucketFreezer(
		safeLocationDirectory.getAbsolutePath());
	File dirToBeMoved = createTestDirectory();

	// Test
	int exitStatus = bucketFreezer.freezeBucket(dirToBeMoved
		.getAbsolutePath());
	assertEquals(0, exitStatus);

	// Verify
	assertTrue(!dirToBeMoved.exists());
	assertTrue(safeLocationDirectory.exists());
	assertTrue(isDirectoryWithChildren(safeLocationDirectory));
	FileUtils.deleteDirectory(safeLocationDirectory);
    }

    private boolean isDirectoryWithChildren(File directory) {
	File[] listFiles = directory.listFiles();
	System.out.println(Arrays.toString(listFiles));
	return listFiles.length > 0;
    }

    private File getTestDirectory() {
	return new File(getClass().getSimpleName() + "-test-dir");
    }

    private File createTestDirectory() {
	return createDirectory(getTestDirectory());
    }

    private File createDirectory(File dir) {
	dir.mkdir();
	assertTrue(dir.exists());
	return dir;
    }

}
