package org.jboss.set.mavendependencyupdater.rules;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenizedVersion implements Comparable<TokenizedVersion> {

    private static final String DELIMITERS = ".-_";

    private static final Pattern NUMERICAL_SEGMENT_PATTERN = Pattern.compile("^\\d+");
    private static final Pattern QUALIFIER_PATTERN = Pattern.compile("^((\\d+)([._-]\\d+)*)([._-](.*))?");

    /**
     * Suffixes that are going to be removed before version comparisons.
     */
    private static final List<String> BUILD_SUFFIXES = new ArrayList<>();
    static {
        BUILD_SUFFIXES.add("redhat");
        BUILD_SUFFIXES.add("jbossorg");
    }

    private static final VersionScheme AETHER_VERSION_SCHEME = new GenericVersionScheme();

    private List<String> segments;
    private List<String> delimiters;
    private String versionString;

    private TokenizedVersion(String versionString, List<String> segments, List<String> delimiters) {
        this.versionString = versionString;
        this.segments = segments;
        this.delimiters = delimiters;
    }

    public static TokenizedVersion parse(String version) {
        StringTokenizer tokenizer = new StringTokenizer(version, DELIMITERS, true);

        List<String> segments = new ArrayList<>();
        List<String> delimiters = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            segments.add(tokenizer.nextToken());
            if (tokenizer.hasMoreTokens()) {
                delimiters.add(tokenizer.nextToken());
            }
        }

        return new TokenizedVersion(version, segments, delimiters);
    }

    public boolean isPrefixOf(TokenizedVersion version) {
        Iterator<String> thisIterator = segments.iterator();
        Iterator<String> thatIterator = version.segments.iterator();
        while (thisIterator.hasNext()) {
            if (!thatIterator.hasNext()) {
                return false;
            }
            if (!thisIterator.next().equals(thatIterator.next())) {
                return false;
            }
        }
        return true;
    }

    public String getQualifier() {
        Matcher matcher = QUALIFIER_PATTERN.matcher(versionString);
        if (matcher.matches()) {
            String q = matcher.group(5);
            return q == null ? "" : q;
        }
        // as a backup return whole version string
        return versionString;
    }

    public String getNumericalPart() {
        Matcher matcher = QUALIFIER_PATTERN.matcher(versionString);
        if (matcher.matches()) {
            String q = matcher.group(1);
            return q == null ? "" : q;
        }
        // as a backup return whole version string
        return "";
    }

    public String[] getNumericalSegments() {
        ArrayList<String> numericalSegments = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            if (NUMERICAL_SEGMENT_PATTERN.matcher(segments.get(i)).matches()) {
                numericalSegments.add(segments.get(i));
            } else {
                break;
            }
        }
        return numericalSegments.toArray(new String[0]);
    }

    public String getPrefix(int numOfSegments) {
        if (this.segments.size() == 0 || numOfSegments == 0) {
            return "";
        }
        StringBuilder prefix = new StringBuilder(this.segments.get(0));
        for (int i = 1; i < Integer.min(numOfSegments, this.segments.size()); i++) {
            prefix.append(this.delimiters.get(i - 1)).append(this.segments.get(i));
        }
        return prefix.toString();
    }

    public String getVersionString() {
        return versionString;
    }

    public Version toAetherVersion() {
        try {
            return AETHER_VERSION_SCHEME.parseVersion(versionString);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException("Can't parse version: " + e.getVersion(), e);
        }
    }

    public boolean hasBuildSuffix() {
        int size = segments.size();
        return size > 3 && BUILD_SUFFIXES.contains(segments.get(size - 2).toLowerCase());
    }

    /**
     * @return version segments representing build suffix (e.g. ["redhat", "00001"]).
     */
    public String[] getBuildSuffixSegments() {
        if (hasBuildSuffix()) {
            return new String[] {
                    segments.get(segments.size() - 2).toLowerCase(),
                    segments.get(segments.size() - 1).toLowerCase()
            };
        }
        return new String[0];
    }

    public String getVersionWithoutBuildSuffix() {
        if (hasBuildSuffix()) {
            return getPrefix(segments.size() - 2);
        } else {
            return versionString;
        }
    }

    @Override
    public int compareTo(TokenizedVersion that) {
        try {
            // compare versions without build suffix
            Version v1 = AETHER_VERSION_SCHEME.parseVersion(this.getVersionWithoutBuildSuffix());
            Version v2 = AETHER_VERSION_SCHEME.parseVersion(that.getVersionWithoutBuildSuffix());

            int comp = v1.compareTo(v2);
            if (comp != 0) {
                return comp;
            }

            String[] suffix1 = this.getBuildSuffixSegments();
            String[] suffix2 = that.getBuildSuffixSegments();

            if (suffix1.length == 0 && suffix2.length == 0) {
                return 0; // no suffix on either side
            } else if (suffix1.length < suffix2.length) {
                return -1; // suffix wins
            } else if (suffix1.length > suffix2.length) {
                return 1; // suffix wins
            } else {
                // compare textual part of the suffix
                comp = suffix1[0].compareTo(suffix2[0]);
                if (comp != 0) {
                    return comp;
                }
                // compare numerical part of the suffix
                try {
                    Integer num1 = Integer.parseInt(suffix1[1]);
                    Integer num2 = Integer.parseInt(suffix2[1]);
                    return num1.compareTo(num2);
                } catch (NumberFormatException e) {
                    // fallback in case numerical parts are not really numerical
                    return suffix1[1].compareTo(suffix2[1]);
                }
            }
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException("Can't parse version: " + e.getVersion(), e);
        }
    }

    @Override
    public String toString() {
        return versionString;
    }
}
