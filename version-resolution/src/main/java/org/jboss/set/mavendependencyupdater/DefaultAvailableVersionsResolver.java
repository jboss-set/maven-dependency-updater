package org.jboss.set.mavendependencyupdater;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultAvailableVersionsResolver implements AvailableVersionsResolver {

    private static final Logger LOG = Logger.getLogger(DefaultAvailableVersionsResolver.class);

    private RepositorySystem system;
    private DefaultRepositorySystemSession session;
    private List<RemoteRepository> repositories;

    public DefaultAvailableVersionsResolver(Configuration configuration) {
        system = newRepositorySystem();
        session = newRepositorySystemSession(system);
        this.repositories = newRemoteRepositoryList(configuration.getRepositories());
    }

    @Override
    public VersionRangeResult resolveVersionRange(Artifact artifact) throws RepositoryException {
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(repositories);

        VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);

        if (LOG.isDebugEnabled()) {
            for (Exception e: rangeResult.getExceptions()) {
                LOG.debugf(e, "Version resolution exception: %s", e.getMessage());
            }
        }


        return rangeResult;
    }


    private static DefaultServiceLocator newServiceLocator() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(org.eclipse.aether.impl.VersionRangeResolver.class, org.apache.maven.repository.internal.DefaultVersionRangeResolver.class);
        locator.addService(MetadataGeneratorFactory.class, SnapshotMetadataGeneratorFactory.class);
        locator.addService(MetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
        return locator;
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
            {
                LOG.errorf( "Service creation failed for %s implementation %s: %s",
                        type, impl, exception.getMessage(), exception );
            }
        } );

        return locator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

//        session.setTransferListener( new ConsoleTransferListener() );
//        session.setRepositoryListener( new ConsoleRepositoryListener() );

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    private static List<RemoteRepository> newRemoteRepositoryList(Map<String, String> repositories) {
        if (!repositories.isEmpty()) {
            ArrayList<RemoteRepository> repos = new ArrayList<>();
            for (Map.Entry<String, String> entry : repositories.entrySet()) {
                repos.add(new RemoteRepository.Builder(entry.getKey(), "default", entry.getValue()).build());
            }
            LOG.infof("Using repositories: %s", repositories.values());
            return repos;
        }
        // if none specified, return Maven Central as default
        LOG.infof("Using Maven Central repository");
        return Collections.singletonList(
                new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
                        .build());
    }

}
