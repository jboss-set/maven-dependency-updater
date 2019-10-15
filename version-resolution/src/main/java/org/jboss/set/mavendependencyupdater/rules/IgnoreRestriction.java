package org.jboss.set.mavendependencyupdater.rules;

/**
 * Refuses versions that match any of given regular expressions.
 */
public class IgnoreRestriction extends AbstractExpressionMatchingRestriction {

    public IgnoreRestriction(String[] expressions) {
        super(expressions);
    }

    @Override
    public boolean applies(String version, String originalVersion) {
        return !matches(version, false);
    }
}
