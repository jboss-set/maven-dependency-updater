package org.jboss.set.mavendependencyupdater.configuration;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.rules.TokenizedVersion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ConfigurationGenerator {

    private static final Logger LOG = Logger.getLogger(ConfigurationGenerator.class);

    private static final String ORIGINAL_VERSION_COMMENT = "Auto-generated from version ";

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> IGNORE_SCOPES = Arrays.asList("test");

    private static final String[] DEFAULT_QUALIFIERS = {
            "",
            "Final",
            "final",
            "Final-jbossorg-\\d+",
            "final-jbossorg-\\d+",
            "GA"
    };

    private static final Map<String, Object> DEFAULT_RULE = new RuleBuilder()
            .stream(VersionStream.MICRO)
            .qualifier(DEFAULT_QUALIFIERS)
            .build();

    public void generateDefautlConfig(File configurationFile, Collection<ScopedArtifactRef> dependencies) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ConfigurationModel.IGNORE_SCOPES, IGNORE_SCOPES);
        map.put(ConfigurationModel.RULES, generateRules(dependencies));

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(configurationFile, map);
    }

    private Map<String, Map<String, Object>> generateRules(Collection<ScopedArtifactRef> dependencies) {
        Map<String, Map<String, Object>> rules = new LinkedHashMap<>();
        rules.put("*:*", DEFAULT_RULE);
        dependencies.stream().sorted().forEach(dep -> {
            if (!IGNORE_SCOPES.contains(dep.getScope())) {
                Map<String, Object> rule = ruleFromVersion(dep);
                if (rule != null) {
                    rules.put(dep.getGroupId() + ":" + dep.getArtifactId(), rule);
                }
            }
        });
        return rules;
    }

    private Map<String, Object> ruleFromVersion(ArtifactRef ref) {
        TokenizedVersion v = TokenizedVersion.parse(ref.getVersionString());
        String q = v.getQualifier();
        if (!isEmpty(q)) {
            if (q.equals("Final")) {
//                return latestWithQualifier(v, "Final");
                return null;
            }
            if (q.equals("final")) {
//                return latestWithQualifier(v, "final");
                return null;
            }
            if (q.equals("GA")) {
//                return latestWithQualifier(v, "GA");
                return null;
            }
            if (matches(q, "Final-jbossorg-\\d+")) {
                return latestWithQualifier(v, "Final-jbossorg-\\d+");
            }
            if (matches(q, "jbossorg-\\d+")) {
                return latestWithQualifier(v, "jbossorg-\\d+");
            }
            if (matches(q, "RC\\d+")) {
                return latestWithQualifier(v, "RC\\d+", "Final", "");
            }
            if (q.equals("jre")) {
                return latestWithQualifier(v, "jre");
            }
            if (q.startsWith("SP")) {
                return latestSP(v);
            }
            String milestonePattern = "A?M\\d+";
            if (matches(q, milestonePattern)) { // milestone
                return prefixAndQualifier(v, milestonePattern);
            }
            LOG.warnf("Not sure what rule to assign to this qualifier: " + ref);
            return null;
        }
//        return latest(v);
        return null;
    }

    private boolean matches(String str, String regex) {
        return Pattern.compile(regex).matcher(str).matches();
    }

    /**
     * Creates prefix rule containing all leading numerical segments of the original version, plus qualifier must be
     * a service pack number ("SP" + number).
     *
     * @param version original version
     * @return rule definition
     */
    private Map<String, Object> latestSP(TokenizedVersion version) {
        return prefixAndQualifier(version, "SP\\d+");
    }

    /**
     * Creates prefix rule containing all leading numerical segments of the original version, plus qualifier must match
     * given pattern.
     *
     * @param version original version
     * @param qualifier regular expression pattern
     * @return rule definition
     */
    private Map<String, Object> prefixAndQualifier(TokenizedVersion version, String qualifier) {
        int numericalSegments = version.getNumericalSegments().length;
        return new RuleBuilder()
                .prefix(version.getPrefix(numericalSegments))
                .qualifier(qualifier)
                .comment(ORIGINAL_VERSION_COMMENT + version.getVersionString())
                .build();
    }

    /**
     * Same as {@link #latest}, plus qualifier must match one of patterns.
     *
     * @param version original version
     * @param qualifiers regular expression patterns
     * @return rule definition
     */
    private Map<String, Object> latestWithQualifier(TokenizedVersion version, String... qualifiers) {
        int numericalSegments = version.getNumericalSegments().length;
        return new RuleBuilder()
                .prefix(version.getPrefix(Integer.max(2, numericalSegments - 1)))
                .qualifier(qualifiers)
                .comment(ORIGINAL_VERSION_COMMENT + version.getVersionString())
                .build();
    }

    /**
     * Creates prefix rule containing one less numerical segment than the original version:
     *
     * 1.2.3 => prefix "1.2"
     * 1.2.3.4 => prefix "1.2.3"
     *
     * At least two segments are used though:
     *
     * 1.2 => prefix "1.2"
     *
     * @param version original version
     * @return rule definition
     */
    private Map<String, Object> latest(TokenizedVersion version) {
        int numericalSegments = version.getNumericalSegments().length;
        return new RuleBuilder()
                .prefix(version.getPrefix(Integer.max(2, numericalSegments - 1)))
                .comment(ORIGINAL_VERSION_COMMENT + version.getVersionString())
                .build();
    }

    private static class RuleBuilder {
        private String prefix;
        private String stream;
        private List<String> qualifiers = new ArrayList<>();
        private String comment;

        RuleBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        RuleBuilder qualifier(String... regexprs) {
            qualifiers.addAll(Arrays.asList(regexprs));
            return this;
        }

        RuleBuilder stream(VersionStream stream) {
            this.stream = stream.name();
            return this;
        }

        RuleBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        Map<String, Object> build() {
            Map<String, Object> rule = new HashMap<>();

            if (prefix != null) {
                rule.put(Configuration.PREFIX, prefix);
            }

            if (stream != null) {
                rule.put(Configuration.STREAM, stream);
            }

            if (qualifiers.size() == 1) {
                rule.put(Configuration.QUALIFIER, qualifiers.get(0));
            } else if (qualifiers.size() > 1) {
                rule.put(Configuration.QUALIFIER, qualifiers);
            }

            if (comment != null) {
                rule.put(Configuration.COMMENT, comment);
            }

            return rule;
        }
    }
}
