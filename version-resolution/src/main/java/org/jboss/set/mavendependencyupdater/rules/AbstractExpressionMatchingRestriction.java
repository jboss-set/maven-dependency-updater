package org.jboss.set.mavendependencyupdater.rules;

import java.util.regex.Pattern;

/**
 * Restriction matching strings against specified array of regular expressions.
 */
abstract class AbstractExpressionMatchingRestriction implements Restriction {

    private Pattern[] patterns;

    /**
     * @param expressions array of regular expressions
     */
    AbstractExpressionMatchingRestriction(String[] expressions) {
        this.patterns = new Pattern[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            patterns[i] = Pattern.compile(expressions[i]);
        }
    }

    /**
     * Tests if the string is matched by one of configured patterns.
     *
     * @param str string to be tested
     * @return string matched?
     */
    boolean matches(String str) {
        for (Pattern pattern: patterns) {
            if (pattern.matcher(str).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if the string is found by one of configured patterns.
     *
     * @param str string to be tested
     * @return string was found?
     */
    boolean find(String str) {
        for (Pattern pattern: patterns) {
            if (pattern.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }
}
