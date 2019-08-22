package org.jboss.set.mavendependencyupdater.git;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides control of local git repository.
 */
public class GitManipulator {

    private Repository repository;
    private Git git;
    private String accessToken;

    // TODO: replace accessToken with Configuration instance
    public GitManipulator(File gitDir, String accessToken) throws IOException {
        repository = FileRepositoryBuilder.create(gitDir);
        git = new Git(repository);
        this.accessToken = accessToken;
    }

    public void checkout(String branch) throws GitAPIException {
        checkout(branch, false);
    }

    public void checkout(String branch, boolean createNew) throws GitAPIException {
        CheckoutCommand checkout = git.checkout();
        checkout.setName(branch);
        checkout.setCreateBranch(createNew);
        checkout.call();
    }

    public void push(String remote, String branch) throws GitAPIException {
        PushCommand push = git.push();
        push.setRemote(remote);
        push.setRefSpecs(new RefSpec(branch + ":" + branch));
        if (!StringUtils.isEmpty(accessToken)) {
            push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken, ""));
        }
        push.call();
    }

    public void commit(String message) throws GitAPIException {
        git.commit().setMessage(message).call();
    }

    public void add(String filePattern) throws GitAPIException {
        git.add().addFilepattern(filePattern).call();
    }

    public List<String> getRemoteBranches() throws GitAPIException {
        List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        return list.stream().map(Ref::getName).collect(Collectors.toList());
    }

    Repository getRepository() {
        return repository;
    }

    Git getGit() {
        return git;
    }
}
