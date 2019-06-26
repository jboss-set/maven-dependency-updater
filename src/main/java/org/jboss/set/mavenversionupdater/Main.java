package org.jboss.set.mavenversionupdater;

import static org.jboss.set.mavenversionupdater.VersionComparison.ComparisonLevel.MINOR;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;
import org.jboss.logging.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        RepositorySystem system = newRepositorySystem();
        DefaultRepositorySystemSession session = newRepositorySystemSession(system);

        File dependenciesFile;
        if (args.length == 1) {
            dependenciesFile = new File(args[0]);
        } else {
            dependenciesFile = new File("/home/thofman/Projects/wildfly/dependencies.txt");
        }
        List<String> dependencies = Files.readAllLines(dependenciesFile.toPath());

        for (String gav: dependencies) {
            Artifact artifact = newArtifact(gav);
            Artifact rangeArtifact = newVersionRangeArtifact(gav);

            if (artifact.getBaseVersion().startsWith("$")) {
                LOG.infof("Skipping %s", artifact);
                continue;
            }
            try {
                VersionRangeRequest rangeRequest = new VersionRangeRequest();
                rangeRequest.setArtifact(rangeArtifact);
                rangeRequest.setRepositories(newRepositories());

                VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);

                List<Version> versions = rangeResult.getVersions();
                Optional<Version> latest = VersionComparison.findLatest(MINOR, artifact.getBaseVersion(), versions);

                System.out.println(String.format("Available versions for %s: %s", gav, versions));
                if (latest.isPresent()) {
                    if (artifact.getBaseVersion().equals(latest.get().toString())) {
                        System.out.println("  => no change");
                    } else {
                        System.out.println(String.format("  => %s", latest.get().toString()));
                    }
                } else {
                    System.out.println("  => no upgrade possible");
                }
            } catch (VersionRangeResolutionException e) {
                LOG.errorf("Could not resolve %s", rangeArtifact.toString());
            }
        }
    }

    private static Artifact newArtifact(String gav) {
        String[] split = gav.split(":");
        if (split.length != 3) {
            throw new RuntimeException("Invalid GAV: " + gav);
        }
        return new DefaultArtifact(split[0], split[1], null, split[2]);
    }

    private static Artifact newVersionRangeArtifact(String gav) {
        String[] split = gav.split(":");
        if (split.length != 3) {
            throw new RuntimeException("Invalid GAV: " + gav);
        }
        return new DefaultArtifact(split[0], split[1], null, "[" + split[2] + ",)");
    }

    private static DefaultServiceLocator newServiceLocator() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(MetadataGeneratorFactory.class, SnapshotMetadataGeneratorFactory.class);
        locator.addService(MetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
        return locator;
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
//        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
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

    private static List<RemoteRepository> newRepositories() {
        return Collections.singletonList(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                        .build());
    }
}
