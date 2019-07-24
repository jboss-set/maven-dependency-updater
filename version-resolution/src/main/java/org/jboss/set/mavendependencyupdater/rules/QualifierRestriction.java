package org.jboss.set.mavendependencyupdater.rules;

import java.util.regex.Pattern;

/**
 * Restricts version qualifier to match one of the provided regular expressions.
 *
 * Qualifier is the part of the version that follows last leading numerical segment of the version. E.g. qualifier in
 * version "1.1.1.Beta.1" is "Beta.1". Delimiter character is stripped.
 */
public class QualifierRestriction implements Restriction {

    private Pattern[] patterns;

    public QualifierRestriction(String[] regexprs) {
        patterns = new Pattern[regexprs.length];
        for (int i = 0; i < regexprs.length; i++) {
            patterns[i] = Pattern.compile(regexprs[i]);
        }
    }

    @Override
    public boolean applies(String version) {
        String qualifier = Version.parse(version).getQualifier();
        for (Pattern pattern: patterns) {
            if (pattern.matcher(qualifier).matches()) {
                return true;
            }
        }
        return false;
    }
}
