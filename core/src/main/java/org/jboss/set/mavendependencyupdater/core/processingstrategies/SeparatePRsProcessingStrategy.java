package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.ArtifactResult;
import org.jboss.set.mavendependencyupdater.ComponentUpgrade;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.configuration.GitConfigurationModel;
import org.jboss.set.mavendependencyupdater.configuration.GitHubConfigurationModel;
import org.jboss.set.mavendependencyupdater.git.GitRepository;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Generates separate PRs for each component upgrade.
 */
@SuppressWarnings("WeakerAccess")
public class SeparatePRsProcessingStrategy implements UpgradeProcessingStrategy {

    private static final Logger LOG = Logger.getLogger(SeparatePRsProcessingStrategy.class);

    private static final String PR_DESCRIPTION = "New version of dependency %s:%s was found.\n\n" +
            "(This pull request was automatically generated.)";
    private static final String POM_XML = "pom.xml";

    private final Configuration configuration;
    private final File pomFile;
    private final GitRepository gitRepository;
    private final GitHub gitHub;
    private final PatchDigestRecorder digestRecorder = new PatchDigestRecorder();

    public SeparatePRsProcessingStrategy(Configuration configuration, File pomFile) {
        this.configuration = configuration;
        this.pomFile = pomFile;

        // local git repo control
        File gitDir = new File(pomFile.getParent(), ".git");
        try {
            this.gitRepository = new GitRepository(gitDir, configuration.getGitHub().getAccessToken());
        } catch (IOException e) {
            throw new RuntimeException("Failure when reading git repository: " + gitDir, e);
        }

        // github connector
        try {
            GitHubConfigurationModel ghConfig = configuration.getGitHub();
            this.gitHub = GitHub.connect(ghConfig.getLogin(), ghConfig.getAccessToken());
        } catch (IOException e) {
            throw new RuntimeException("Can't connect to GitHub account", e);
        }
    }

    public SeparatePRsProcessingStrategy(Configuration configuration, File pomFile, GitRepository gitRepository, GitHub gitHub) {
        this.configuration = configuration;
        this.pomFile = pomFile;
        this.gitRepository = gitRepository;
        this.gitHub = gitHub;
    }

    @Override
    public boolean process(List<ArtifactResult<ComponentUpgrade>> upgrades) {
        boolean result = true;

        for (ArtifactResult<ComponentUpgrade> upgrade : upgrades) {
            if (upgrade.getLatestConfigured().isPresent()) {
                boolean partialResult = createPRForUpgrade(upgrade.getLatestConfigured().get());
                result = partialResult && result;
            }
        }

        return result;
    }

    /**
     * Upgrades a component in pom.xml
     *
     * @param componentUpgrade component upgrade
     * @return success?
     */
    protected boolean createPRForUpgrade(ComponentUpgrade componentUpgrade) {
        ArtifactRef artifact = componentUpgrade.getArtifact();
        String newVersion = componentUpgrade.getNewVersion();
        String baseBranch = configuration.getGit().getBaseBranch();
        String workingBranch = getBranchName(artifact, newVersion);
        String commitMessage = getCommitMessage(artifact, newVersion);
        GitConfigurationModel gitConfig = configuration.getGit();
        GitHubConfigurationModel ghConfig = configuration.getGitHub();

        try {
            GHRepository repo = this.gitHub.getRepository(ghConfig.getUpstreamRepository());

            // TODO: devise a way to record open PRs and intelligently handle situations when a new PR is to be opened
            //  for a dependency which already has an older PR open. We could: reuse the old one; close the old one;
            //  or at least link them?

            // check that remote branch of the same name doesn't exist (local branches would get deleted, if this runs
            // in a CI server)
            String remoteRef = "refs/remotes/" + gitConfig.getRemote() + "/" + workingBranch;
            if (gitRepository.getRemoteBranches().contains(remoteRef)) {
                LOG.infof("Remote branch '%s' already exists, skipping this upgrade`.", workingBranch);
                return true;
            }
            // check that open PR with the same title doesn't exist
            Optional<GHPullRequest> existingPR = findOpenPRByTitle(repo, commitMessage);
            if (existingPR.isPresent()) {
                LOG.infof("PR already exists, skipping this upgrade: %s", existingPR.get().getHtmlUrl());
                return true;
            }

            // make sure we are on the base branch
            gitRepository.checkout(baseBranch);

            // perform single upgrade
            PomDependencyUpdater.upgradeDependencies(pomFile, Collections.singletonList(componentUpgrade));

            // verify that the patch is unique to the already performed ones
            Pair<ArtifactRef, String> previousUpgrade = digestRecorder.recordPatchDigest(pomFile, artifact, newVersion);
            if (previousUpgrade != null) {
                LOG.infof("Patch for %s:%s:%s is identical to already created patch for %s:%s:%s," +
                                " skipping PR creation.",
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        newVersion,
                        previousUpgrade.getLeft().getGroupId(),
                        previousUpgrade.getLeft().getArtifactId(),
                        previousUpgrade.getRight());
                gitRepository.resetLocalChanges();
                return true;
            }

            // prepare new branch
            gitRepository.checkout(workingBranch, true);

            // commit and push to origin
            gitRepository.add(POM_XML); // TODO: possibly more files
            gitRepository.commit(commitMessage);
            gitRepository.push(gitConfig.getRemote(), workingBranch);

            // create PR
            String sourceBranch = getSourceBranch(ghConfig.getOriginRepository(), workingBranch);
            String upstreamBaseBranch = ghConfig.getUpstreamBaseBranch();
            @SuppressWarnings("UnnecessaryLocalVariable")
            String title = commitMessage;
            String description = String.format(PR_DESCRIPTION, artifact.getGroupId(), artifact.getArtifactId());
            GHPullRequest pr = repo.createPullRequest(title, sourceBranch, upstreamBaseBranch, description);
            System.out.println(pr.getHtmlUrl());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("PR creation failed", e);
        } finally {
            try {
                // return to base branch
                gitRepository.checkout(baseBranch);
            } catch (GitAPIException e) {
                LOG.errorf("Failed to checkout base branch: %s", baseBranch);
            }
        }
    }

    protected String getBranchName(ArtifactRef artifact, String newVersion) {
        return "upgrade_" + artifact.getGroupId() + "_" + artifact.getArtifactId() + "_" + newVersion;
    }

    protected String getCommitMessage(ArtifactRef artifact, String newVersion) {
        return String.format("Upgrade %s:%s from %s to %s",
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersionString(), newVersion);
    }

    private static Optional<GHPullRequest> findOpenPRByTitle(GHRepository repo, String title) throws IOException {
        List<GHPullRequest> pullRequests = repo.getPullRequests(GHIssueState.OPEN);
        for (GHPullRequest pr : pullRequests) {
            if (pr.getTitle().equals(title)) {
                return Optional.of(pr);
            }
        }
        return Optional.empty();
    }

    private static String getSourceBranch(String originRepository, String branch) {
        String[] origin = originRepository.split("/");
        return origin[0] + ":" + branch;
    }
}
