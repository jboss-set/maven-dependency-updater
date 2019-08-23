package org.jboss.set.mavendependencyupdater.cli.upgradeprocessing;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.PomDependencyUpdater;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;
import org.jboss.set.mavendependencyupdater.configuration.GitConfigurationModel;
import org.jboss.set.mavendependencyupdater.configuration.GitHubConfigurationModel;
import org.jboss.set.mavendependencyupdater.git.GitManipulator;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates separate PRs for each component upgrade.
 */
@SuppressWarnings("WeakerAccess")
public class SeparatePRsProcessingStrategy implements UpgradeProcessingStrategy {

    private static final Logger LOG = Logger.getLogger(SeparatePRsProcessingStrategy.class);

    private static final String PR_DESCRIPTION = "New version of dependency %s:%s was found.\n\n" +
            "(This pull request was automatically generated.)";

    private Configuration configuration;
    private File pomFile;
    private GitManipulator gitManipulator;
    private GitHub gitHub;

    public SeparatePRsProcessingStrategy(Configuration configuration, File pomFile) {
        this.configuration = configuration;
        this.pomFile = pomFile;

        // local git repo control
        File gitDir = new File(pomFile.getParent(), ".git");
        try {
            this.gitManipulator = new GitManipulator(gitDir, configuration.getGitHub().getAccessToken());
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

    @Override
    public boolean process(Map<ArtifactRef, String> upgrades) {
        boolean result = true;
        String baseBranch = configuration.getGit().getBaseBranch();

        for (Map.Entry<ArtifactRef, String> entry: upgrades.entrySet()) {
            try {
                gitManipulator.checkout(baseBranch);
                boolean partialResult = createPRForUpgrade(entry.getKey(), entry.getValue());
                result = partialResult && result;
                gitManipulator.checkout(baseBranch);
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to checkout base branch: " + baseBranch);
            }
        }

        return result;
    }

    protected boolean createPRForUpgrade(ArtifactRef artifact, String newVersion) {
        try {
            String workingBranch = getBranchName(artifact, newVersion);
            String commitMessage = getCommitMessage(artifact, newVersion);
            GitConfigurationModel gitConfig = configuration.getGit();
            GitHubConfigurationModel ghConfig = configuration.getGitHub();
            GHRepository repo = this.gitHub.getRepository(ghConfig.getUpstreamRepository());

            // TODO: devise a way to record open PRs and intelligently handle situations when a new PR is to be opened
            //  for a dependency which already has an older PR open. We could: reuse the old one; close the old one;
            //  or at least link them?

            // check that remote branch of the same name doesn't exist (local branches would get deleted, if this runs
            // in a CI server)
            String remoteRef = "refs/remotes/" + gitConfig.getRemote() + "/" + workingBranch;
            if (gitManipulator.getRemoteBranches().contains(remoteRef)) {
                LOG.infof("Remote branch '%s' already exists, skipping this upgrade`.", workingBranch);
                return true;
            }
            // check that open PR with the same title doesn't exist
            Optional<GHPullRequest> existingPR = findOpenPRByTitle(repo, commitMessage);
            if (existingPR.isPresent()) {
                LOG.infof("PR already exists, skipping this upgrade: %s", existingPR.get().getHtmlUrl());
                return true;
            }

            // prepare new branch
            gitManipulator.checkout(workingBranch, true);

            // perform single upgrade
            PomDependencyUpdater.upgradeDependencies(pomFile, Collections.singletonMap(artifact, newVersion));

            // commit and push to origin
            gitManipulator.add("pom.xml"); // TODO: possibly more files
            gitManipulator.commit(commitMessage);
            gitManipulator.push(gitConfig.getRemote(), workingBranch);

            // create PR
            String sourceBranch = getSourceBranch(ghConfig.getOriginRepository(), workingBranch);
            String baseBranch = ghConfig.getUpstreamBaseBranch();
            @SuppressWarnings("UnnecessaryLocalVariable")
            String title = commitMessage;
            String description = String.format(PR_DESCRIPTION, artifact.getGroupId(), artifact.getArtifactId());
            GHPullRequest pr = repo.createPullRequest(title, sourceBranch, baseBranch, description);
            System.out.println(pr.getHtmlUrl());
            return true;
        } catch (Exception e) {
            // just report, let the loop continue
            LOG.error("PR creation failed", e);
            return false;
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
        for (GHPullRequest pr: pullRequests) {
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
