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
import java.util.Iterator;
import java.util.List;

public class GitManipulatorTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File repoDir1;
    private File repoDir2;
    private GitManipulator manipulator1;
    private GitManipulator manipulator2;

    @Before
    public void setUp() throws IOException, URISyntaxException, GitAPIException {
        // create first repo
        repoDir1 = temporaryFolder.newFolder();
        File gitDir1 = new File(repoDir1, ".git");
        createRepository(gitDir1);
        manipulator1 = new GitManipulator(gitDir1, null);

        // create second repo
        repoDir2 = temporaryFolder.newFolder();
        File gitDir2 = new File(repoDir2, ".git");
        createRepository(gitDir2);
        manipulator2 = new GitManipulator(gitDir2, null);

        // set the second to be a remote of the first
        manipulator1.getGit().remoteAdd()
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

        // commit sample file
        manipulator1.add("test.txt");
        manipulator1.commit("initial commit");

        // verify commit
        Iterable<RevCommit> commits = manipulator1.getGit().log().call();
        Iterator<RevCommit> iterator = commits.iterator();
        RevCommit commit = iterator.next();
        Assert.assertEquals("initial commit", commit.getFullMessage());
        Assert.assertFalse(iterator.hasNext());

        // push to upstream repo and verify it can be checked out there
        manipulator1.checkout("branch", true);
        manipulator1.push("upstream", "branch");
        manipulator2.checkout("branch");
        Assert.assertTrue(new File(repoDir2, "test.txt").exists());

        List<String> remoteBranches = manipulator1.getRemoteBranches();
        Assert.assertTrue(remoteBranches.contains("refs/remotes/upstream/branch"));
    }

    private void createRepository(File gitDir) throws IOException {
        Repository repository = FileRepositoryBuilder.create(gitDir);
        repository.create();
        repository.close();
    }
}
