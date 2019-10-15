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
     *
     * @param str string to be tested
     * @param wholeString if true whole string must match, if false part of the string must match
     * @return string matches one of configured patterns
     */
    boolean matches(String str, boolean wholeString) {
        for (Pattern pattern: patterns) {
            if (wholeString
                    ? pattern.matcher(str).matches()
                    : pattern.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }
}
