package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.git.GitRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.mockito.ArgumentMatchers.anyString;

public class SeparatePRsProcessingStrategyTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File pomXml;
    private File config;
    private SeparatePRsProcessingStrategy strategy;
    private GitRepository localRepo;
    private GitRepository originRepo;
    private File localDir;
    private File originDir;

    @Before
    public void setUp() throws IOException, GitAPIException, URISyntaxException {
        localDir = temporaryFolder.newFolder("origin");
        File localGitDir = new File(localDir, ".git");
        createRepository(localGitDir);
        originDir = temporaryFolder.newFolder("upstream");
        File originGitDir = new File(originDir, ".git");
        createRepository(originGitDir);

        InputStream pomXmlResource = getClass().getClassLoader().getResourceAsStream("pom.xml");
        Assert.assertNotNull(pomXmlResource);
        pomXml = new File(localDir, "pom.xml");
        Files.copy(pomXmlResource, pomXml.toPath());

        config = temporaryFolder.newFile("configuration.json");
        InputStream configResource = getClass().getClassLoader().getResourceAsStream("configuration.json");
        Assert.assertNotNull(configResource);
        Files.copy(configResource, config.toPath(), StandardCopyOption.REPLACE_EXISTING);

        localRepo = new GitRepository(localGitDir, "token");
        localRepo.add("pom.xml");
        localRepo.commit("initial commit");

        originRepo = new GitRepository(originGitDir, "token");

        localRepo.getGit().remoteAdd()
                .setName("origin")
                .setUri(new URIish(originDir.getAbsolutePath()))
                .call();


        GHRepository ghRepository = Mockito.mock(GHRepository.class);
        Mockito.when(ghRepository.createPullRequest(anyString(), anyString(), anyString(), anyString())).thenReturn(new GHPullRequest());

        GitHub gitHub = Mockito.mock(GitHub.class);
        Mockito.when(gitHub.getRepository(anyString())).thenReturn(ghRepository);

        strategy = new SeparatePRsProcessingStrategy(new Configuration(config), pomXml, localRepo, gitHub);
    }

    @Test
    public void testGeneratedBranches() throws IOException, GitAPIException {
        // first component upgrade
        strategy.createPRForUpgrade(SimpleArtifactRef.parse("org.jboss.logging:jboss-logging:3.4.0.Final"),
                "3.4.1.Final");

        // check that the branch was created and pushed to the origin repo
        Assert.assertTrue(originRepo.getLocalBranches()
                .contains("refs/heads/upgrade_org.jboss.logging_jboss-logging_3.4.1.Final"));
        Assert.assertEquals(1, originRepo.getLocalBranches().size());

        // check that the version was upgraded
        originRepo.checkout("upgrade_org.jboss.logging_jboss-logging_3.4.1.Final");
        Assert.assertTrue(getFileContent(new File(originDir, "pom.xml"))
                .contains("<version.jboss-logging>3.4.1.Final</version.jboss-logging>"));

        // second component upgrade, version defined by the same variable, i.e. would be identical to the first one
        strategy.createPRForUpgrade(SimpleArtifactRef.parse("org.jboss.logging:jboss-logging-annotations:3.4.0.Final"),
                "3.4.1.Final");

        // no new branch should have been created
        Assert.assertEquals(1, originRepo.getLocalBranches().size());

        // local changes should have been reverted
        String pomContent = Files.readAllLines(pomXml.toPath()).stream().reduce(String::concat).get();
        Assert.assertTrue(pomContent.contains("<version.jboss-logging>3.4.0.Final</version.jboss-logging>"));
    }

    private static void createRepository(File gitDir) throws IOException {
        Repository repository = FileRepositoryBuilder.create(gitDir);
        repository.create();
        repository.close();
    }

    private static String getFileContent(File file) throws IOException {
        return Files.readAllLines(file.toPath()).stream().reduce(String::concat).get();
    }
}
