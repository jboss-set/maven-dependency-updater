package org.jboss.set.mavendependencyupdater.rules;

import java.util.regex.Pattern;

/**
 * Restricts version qualifier to match one of the provided regular expressions.
 *
 * Qualifier is the part of the version that follows last leading numerical segment of the version. E.g. qualifier in
 * version "1.1.1.Beta.1" is "Beta.1". Delimiter character is stripped.
 */
public class QualifierRestriction extends AbstractExpressionMatchingRestriction {

    private Pattern[] patterns;

    public QualifierRestriction(String[] expressions) {
        super(expressions);
    }

    @Override
    public boolean applies(String version, String originalVersion) {
        String qualifier = Version.parse(version).getQualifier();
        return matches(qualifier, true);
    }
}
