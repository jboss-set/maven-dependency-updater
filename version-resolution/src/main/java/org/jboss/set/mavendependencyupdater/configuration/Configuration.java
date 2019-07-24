package org.jboss.set.mavendependencyupdater.configuration;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.rules.QualifierRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;

/**
 * Provides app configuration.
 */
public class Configuration {

    private static final String WILDCARD = "*";
    private static final String QUALIFIER = "QUALIFIER";
    private static final String PREFIX = "PREFIX";
    private static final String STREAM = "STREAM";

    private ArtifactRef bomCoordinates;
    private Map<String, Map<String, VersionStream>> streams = new HashMap<>();
    private Map<String, Map<String, List<Restriction>>> restrictions = new HashMap<>();

    public Configuration(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ConfigurationModel data = mapper.readValue(file, ConfigurationModel.class);


        // BOM coords

        if (data.getBomCoordinates() != null) {
            bomCoordinates = SimpleArtifactRef.parse(data.getBomCoordinates());
        } else {
            bomCoordinates = new SimpleArtifactRef("undefined", "undefined", "0.1-SNAPSHOT", null, null);
        }


        // rules

        for (Map.Entry<String, Object> gaEntry : data.getRules().entrySet()) {
            String ga = gaEntry.getKey();
            if (gaEntry.getValue() instanceof String) {
                // if only string is provided, consider it VersionStream value
                addStreamRule(ga, VersionStream.valueOf((String) gaEntry.getValue()));
            } else if (gaEntry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) gaEntry.getValue();

                if (configMap.containsKey(PREFIX) && configMap.containsKey(STREAM)) {
                    throw new IllegalArgumentException("Only one of STREAM and PREFIX keys can be defined for " + ga);
                }

                for (Map.Entry<String, Object> restrictionEntry : configMap.entrySet()) {
                    Object restrictionObject = restrictionEntry.getValue();
                    switch (restrictionEntry.getKey()) {
                        case PREFIX:
                            addRestriction(ga, new VersionPrefixRestriction((String) restrictionObject));
                            break;
                        case QUALIFIER:

                            String[] masks;
                            if (restrictionObject instanceof String) {
                                masks = new String[]{(String) restrictionObject};
                            } else if (restrictionObject instanceof List) {
                                String[] stringArray = new String[((List) restrictionObject).size()];
                                masks = (String[]) ((List) restrictionObject).toArray(stringArray);
                            } else {
                                throw new IllegalArgumentException(String.format("String of list of strings expected (%s, %s)",
                                        ga, QUALIFIER));
                            }
                            addRestriction(ga, new QualifierRestriction(masks));
                            break;
                        case STREAM:
                            if (!(restrictionObject instanceof String)) {
                                throw new IllegalArgumentException(String.format("String expected (%s, %s)",
                                        ga, STREAM));
                            }
                            addStreamRule(ga, VersionStream.valueOf((String) restrictionObject));
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown rule: " + restrictionEntry.getKey());
                    }
                }
            }
        }
    }

    public ArtifactRef getBomCoordinates() {
        return bomCoordinates;
    }

    public VersionStream getStreamFor(String g, String a, VersionStream defaultStream) {
        return findConfigForGA(streams, g, a, defaultStream);
    }

    public List<Restriction> getRestrictionsFor(String g, String a) {
        return findConfigForGA(restrictions, g, a, Collections.emptyList());
    }

    private void addStreamRule(String ga, VersionStream stream) {
        String[] coord = ga.split(":");
        if (coord.length != 2) {
            throw new IllegalArgumentException("Invalid stream key: " + ga);
        }
        String g = coord[0];
        String a = coord[1];

        Map<String, VersionStream> groupStreams =
                streams.computeIfAbsent(g, k -> new HashMap<>());

        if (groupStreams.containsKey(a)) {
            throw new IllegalArgumentException("Stream for " + ga + " defined twice.");
        }

        groupStreams.put(a, stream);
    }

    private void addRestriction(String ga, Restriction restriction) {
        String[] coord = ga.split(":");
        if (coord.length != 2) {
            throw new IllegalArgumentException("Invalid rules ga: " + ga);
        }
        String g = coord[0];
        String a = coord[1];

        Map<String, List<Restriction>> groupRestrictions = restrictions.computeIfAbsent(g, k -> new HashMap<>());

        List<Restriction> restrictions = groupRestrictions.computeIfAbsent(a, k -> new ArrayList<>());

        restrictions.add(restriction);
    }

    private <T> T findConfigForGA(Map<String, Map<String, T>> streams, String g, String a, T defaultValue) {
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
        return defaultValue;
    }
}
