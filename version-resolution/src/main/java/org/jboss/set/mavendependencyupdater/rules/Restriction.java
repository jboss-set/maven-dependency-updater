package org.jboss.set.mavendependencyupdater.rules;

/**
 * Interfaces that's to be implemented by version restrictions.
 */
public interface Restriction {

    /**
     * Applies the restriction.
     *
     * @param version version string
     * @return true if given version passed the restriction test (version can be considered candidate for upgrading)
     */
    boolean applies(String version);
}
