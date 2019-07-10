package org.jboss.set.mavendependencyupdater.configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.jboss.set.mavendependencyupdater.VersionStream;

public class Configuration {

    private static final String WILDCARD = "*";

    private ArtifactRef bomCoordinates;
    private Map<String, Map<String, VersionStream>> streams = new HashMap<>();

    public Configuration(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ConfigurationModel data = mapper.readValue(file, ConfigurationModel.class);

        bomCoordinates = SimpleArtifactRef.parse(data.getBomCoordinates());

        for (Map.Entry<String, String> entry: data.getStreams().entrySet()) {
            String[] ga = entry.getKey().split(":");
            if (ga.length != 2) {
                throw new IllegalArgumentException("Invalid stream key: " + entry.getKey());
            }

            Map<String, VersionStream> groupConfig =
                    streams.computeIfAbsent(ga[0], k -> new HashMap<>());

            if (groupConfig.containsKey(ga[1])) {
                throw new IllegalArgumentException("Stream for " + entry.getKey() + " defined twice.");
            }

            VersionStream level = VersionStream.valueOf(entry.getValue());

            groupConfig.put(ga[1], level);
        }
    }

    public ArtifactRef getBomCoordinates() {
        return bomCoordinates;
    }

    public VersionStream getStreamFor(String g, String a, VersionStream defaultStream) {
        if (streams.containsKey(g)) {
            if (streams.get(g).containsKey(a)) { // group:artifact
                return streams.get(g).get(a);
            } else if (streams.get(g).containsKey(WILDCARD)) { // group:*
                return streams.get(g).get(WILDCARD);
            }
        }
        if (streams.containsKey(WILDCARD)) {
            if (streams.get(WILDCARD).containsKey(a)) { // *:artifact
                return streams.get(WILDCARD).get(a);
            } else if (streams.get(WILDCARD).containsKey(WILDCARD)) { // *:*
                return streams.get(WILDCARD).get(WILDCARD);
            }
        }
        return defaultStream;
    }
}
