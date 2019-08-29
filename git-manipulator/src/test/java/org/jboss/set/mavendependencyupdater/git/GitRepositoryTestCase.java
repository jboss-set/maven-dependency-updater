package org.jboss.set.mavendependencyupdater.git;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

public class GitRepositoryTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File repoDir1;
    private File repoDir2;
    private GitRepository repo1;
    private GitRepository repo2;

    @Before
    public void setUp() throws IOException, URISyntaxException, GitAPIException {
        // create first repo
        repoDir1 = temporaryFolder.newFolder();
        File gitDir1 = new File(repoDir1, ".git");
        createRepository(gitDir1);
        repo1 = new GitRepository(gitDir1, null);

        // create second repo
        repoDir2 = temporaryFolder.newFolder();
        File gitDir2 = new File(repoDir2, ".git");
        createRepository(gitDir2);
        repo2 = new GitRepository(gitDir2, null);

        // set the second to be a remote of the first
        repo1.getGit().remoteAdd()
                .setName("upstream")
                .setUri(new URIish(gitDir2.getAbsolutePath()))
                .call();
    }

    @Test
    public void test() throws GitAPIException, IOException {
        // create sample file in the first repo
        File sampleFile = new File(repoDir1, "test.txt");
        Assert.assertTrue(sampleFile.createNewFile());
        Assert.assertTrue(sampleFile.exists());

        // create initial commit
        repo1.add("test.txt");
        repo1.commit("initial commit");

        // verify commit
        Iterable<RevCommit> commits = repo1.getGit().log().call();
        Iterator<RevCommit> iterator = commits.iterator();
        RevCommit commit = iterator.next();
        Assert.assertEquals("initial commit", commit.getFullMessage());
        Assert.assertFalse(iterator.hasNext());

        // push to upstream repo and verify it can be checked out there
        repo1.checkout("branch", true);
        repo1.push("upstream", "branch");
        repo2.checkout("branch");
        Assert.assertTrue(new File(repoDir2, "test.txt").exists());

        // test getRemoteBranches()
        List<String> remoteBranches = repo1.getRemoteBranches();
        Assert.assertTrue(remoteBranches.contains("refs/remotes/upstream/branch"));

        // modify file
        Files.write(sampleFile.toPath(), "abcd\nabcd".getBytes());
        Assert.assertEquals(2, Files.readAllLines(sampleFile.toPath()).size());

        // reset local changes
        repo1.resetLocalChanges();
        Assert.assertEquals(0, Files.readAllLines(sampleFile.toPath()).size());
    }

    private static void createRepository(File gitDir) throws IOException {
        Repository repository = FileRepositoryBuilder.create(gitDir);
        repository.create();
        repository.close();
    }
}
