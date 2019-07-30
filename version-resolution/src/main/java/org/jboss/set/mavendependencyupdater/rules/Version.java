package org.jboss.set.mavendependencyupdater.rules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {

    private static final String DELIMITERS = ".-_";

    private static final Pattern NUMERICAL_SEGMENT_PATTERN = Pattern.compile("^\\d+");
    private static final Pattern QUALIFIER_PATTERN = Pattern.compile("^((\\d+)([\\._-]\\d+)*)([\\._-](.*))?");

    private List<String> segments = new ArrayList<>();
    private List<String> delimiters = new ArrayList<>();
    private String versionString;

    private Version(String versionString, List<String> segments, List<String> delimiters) {
        this.versionString = versionString;
        this.segments = segments;
        this.delimiters = delimiters;
    }

    public static Version parse(String version) {
        StringTokenizer tokenizer = new StringTokenizer(version, DELIMITERS, true);

        List<String> segments = new ArrayList<>();
        List<String> delimiters = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            segments.add(tokenizer.nextToken());
            if (tokenizer.hasMoreTokens()) {
                delimiters.add(tokenizer.nextToken());
            }
        }

        return new Version(version, segments, delimiters);
    }

    public boolean isPrefixOf(Version version) {
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
}
