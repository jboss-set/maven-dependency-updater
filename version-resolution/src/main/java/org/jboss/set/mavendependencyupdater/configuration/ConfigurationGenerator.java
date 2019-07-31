package org.jboss.set.mavendependencyupdater.configuration;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.common.ident.ScopedArtifactRef;
import org.jboss.set.mavendependencyupdater.rules.Version;

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

    private static final String ORIGINAL_VERSION_COMMENT = "Original version ";

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> IGNORE_SCOPES = Arrays.asList("test");

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
        Version v = Version.parse(ref.getVersionString());
        String q = v.getQualifier();
        if (!isEmpty(q)) {
            if (q.equals("Final")) {
                return latestWithQualifier(v, "Final");
            }
            if (q.equals("final")) {
                return latestWithQualifier(v, "final");
            }
            if (q.equals("GA")) {
                return latestWithQualifier(v, "GA");
            }
            if (matches(q, "Final-jbossorg-\\d+")) {
                return latestWithQualifier(v, "Final", "Final-jbossorg-\\d+");
            }
            if (matches(q, "jbossorg-\\d+")) {
                return latestWithQualifier(v, "", "jbossorg-\\d+");
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
            LOG.warnf("Not sure what to do with qualifier: " + ref);
        }
        return latest(v);
    }

    private boolean matches(String str, String regex) {
        return Pattern.compile(regex).matcher(str).matches();
    }

    private Map<String, Object> latestSP(Version version) {
        return prefixAndQualifier(version, "SP\\d+");
    }

    private Map<String, Object> prefixAndQualifier(Version version, String qualifier) {
        int numericalSegments = version.getNumericalSegments().length;
        return new RuleBuilder()
                .prefix(version.getPrefix(numericalSegments))
                .qualifier(qualifier)
                .comment(ORIGINAL_VERSION_COMMENT + version.getVersionString())
                .build();
    }

    private Map<String, Object> latestWithQualifier(Version version, String... qualifiers) {
        int numericalSegments = version.getNumericalSegments().length;
        return new RuleBuilder()
                .prefix(version.getPrefix(Integer.max(2, numericalSegments - 1)))
                .qualifier(qualifiers)
                .comment(ORIGINAL_VERSION_COMMENT + version.getVersionString())
                .build();
    }

    private Map<String, Object> latest(Version version) {
        int numericalSegments = version.getNumericalSegments().length;
        return new RuleBuilder()
                .prefix(version.getPrefix(Integer.max(2, numericalSegments - 1)))
                .comment(ORIGINAL_VERSION_COMMENT + version.getVersionString())
                .build();
    }

    private static class RuleBuilder {
        private String prefix;
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

        RuleBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        Map<String, Object> build() {
            Map<String, Object> rule = new HashMap<>();

            if (prefix != null) {
                rule.put(Configuration.PREFIX, prefix);
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
