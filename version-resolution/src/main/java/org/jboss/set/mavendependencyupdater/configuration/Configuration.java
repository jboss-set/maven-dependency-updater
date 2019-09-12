package org.jboss.set.mavendependencyupdater.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.rules.NeverRestriction;
import org.jboss.set.mavendependencyupdater.rules.QualifierRestriction;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides tool configuration.
 */
@SuppressWarnings("WeakerAccess")
public class Configuration {

    private static final String WILDCARD = "*";
    public static final String QUALIFIER = "QUALIFIER";
    public static final String PREFIX = "PREFIX";
    public static final String STREAM = "STREAM";
    public static final String COMMENT = "COMMENT";
    public static final String NEVER = "NEVER";

    private Map<String, Map<String, VersionStream>> streams = new HashMap<>();
    private Map<String, Map<String, List<Restriction>>> restrictions = new HashMap<>();
    private List<String> ignoreScopes = new ArrayList<>();
    private GitHubConfigurationModel gitHub;
    private GitConfigurationModel git;

    public Configuration(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ConfigurationModel data = mapper.readValue(file, ConfigurationModel.class);

        this.gitHub = data.getGitHub();
        this.git = data.getGit();

        // ignored scopes
        if (data.getIgnoreScopes() != null) {
            ignoreScopes.addAll(data.getIgnoreScopes());
        }

        // rules
        if (data.getRules() != null) {
            for (Map.Entry<String, Object> gaEntry : data.getRules().entrySet()) {
                String ga = gaEntry.getKey();
                if (gaEntry.getValue() instanceof String) {
                    if (NEVER.equals(gaEntry.getValue())) {
                        // if value is string, it can either be NEVER
                        addRestriction(ga, NeverRestriction.INSTANCE); // never upgrade
                    } else {
                        // or else consider it VersionStream value
                        addStreamRule(ga, VersionStream.valueOf((String) gaEntry.getValue()));
                    }
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
                            case COMMENT:
                                // ignore
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown rule: " + restrictionEntry.getKey());
                        }
                    }
                }
            }
        }
    }

    public VersionStream getStreamFor(String g, String a, VersionStream defaultStream) {
        return findConfigForGA(streams, g, a, defaultStream);
    }

    public List<Restriction> getRestrictionsFor(String g, String a) {
        return findConfigForGA(restrictions, g, a, Collections.emptyList());
    }

    public <T extends Restriction> Optional<T> getRestrictionFor(String g, String a, Class<T> restrictionClass) {
        List<Restriction> restrictions = findConfigForGA(this.restrictions, g, a, Collections.emptyList());
        //noinspection unchecked
        return restrictions.stream()
                .filter(r -> r.getClass().isAssignableFrom(restrictionClass))
                .map(r -> (T) r)
                .findFirst();
    }

    public List<String> getIgnoreScopes() {
        return ignoreScopes;
    }

    public Collection<Pair<ScopedArtifactRef, String>> findOutOfDateRestrictions(Collection<ScopedArtifactRef> currentDependencies) {
        Collection<Pair<ScopedArtifactRef, String>> outOfDate = new HashSet<>();
        for (ScopedArtifactRef ref: currentDependencies) {
            if (getIgnoreScopes().contains(ref.getScope())) {
                continue;
            }

            Optional<VersionPrefixRestriction> prefixRestriction =
                    getRestrictionFor(ref.getGroupId(), ref.getArtifactId(), VersionPrefixRestriction.class);
            if (prefixRestriction.isPresent()) {
                if (!prefixRestriction.get().applies(ref.getVersionString())) {
                    String prefixString = prefixRestriction.get().getPrefixString();
                    outOfDate.add(Pair.of(ref, prefixString));
                }
            }
        }
        return outOfDate;
    }

    public GitHubConfigurationModel getGitHub() {
        return gitHub;
    }

    public GitConfigurationModel getGit() {
        return git;
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

    private static <T> T findConfigForGA(Map<String, Map<String, T>> streams, String g, String a, T defaultValue) {
        if (streams.containsKey(g)) { // matching groupId
            if (streams.get(g).containsKey(a)) { // 1. exact match "group:artifact"
                return streams.get(g).get(a);
            } else if (streams.get(g).containsKey(WILDCARD)) { // 2. wildcard match "group:*"
                return streams.get(g).get(WILDCARD);
            }
        }
        for (Map.Entry<String, Map<String, T>> entry : streams.entrySet()) { // look for group.prefix.*:artifact
            String group = entry.getKey();
            Map<String, T> groupRules = entry.getValue();
            if (!group.equals(WILDCARD) && group.endsWith(WILDCARD)) {
                String groupPrefix = group.substring(0, group.length() - 1);
                if (g.startsWith(groupPrefix) && groupRules.containsKey(a)) { // 3. wildcard match "group.prefix.*:artifact"
                    return groupRules.get(a);
                } else if (g.startsWith(groupPrefix) && groupRules.containsKey(WILDCARD)) { // 4. wildcard match "group.prefix.*:*"
                    return groupRules.get(WILDCARD);
                }
            }
        }
        if (streams.containsKey(WILDCARD)) { // look for marching artifactId in wildcard groupId
            if (streams.get(WILDCARD).containsKey(a)) { // 5. wildcard match "*:artifact"
                return streams.get(WILDCARD).get(a);
            } else if (streams.get(WILDCARD).containsKey(WILDCARD)) { // 6. wildcard match "*:*"
                return streams.get(WILDCARD).get(WILDCARD);
            }
        }
        return defaultValue;
    }
}
