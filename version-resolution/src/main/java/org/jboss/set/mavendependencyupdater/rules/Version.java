package org.jboss.set.mavendependencyupdater.rules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {

    private static String DELIMITERS = ".-_";

    private static Pattern QUALIFIER_PATTERN = Pattern.compile("^(\\d+)([\\._-]\\d+)*([\\._-](.*))?");

    private List<String> segments;
    private String versionString;

    private Version(String versionString, List<String> segments) {
        this.versionString = versionString;
        this.segments = segments;
    }

    public static Version parse(String version) {
        StringTokenizer tokenizer = new StringTokenizer(version, DELIMITERS);

        List<String> segments = new ArrayList<>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            segments.add(tokenizer.nextToken());
        }

        return new Version(version, segments);
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
            String q = matcher.group(4);
            return q == null ? "" : q;
        }
        // as a backup return whole version string
        return versionString;
    }
}
