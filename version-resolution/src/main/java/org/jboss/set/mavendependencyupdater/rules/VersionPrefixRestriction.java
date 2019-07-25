package org.jboss.set.mavendependencyupdater.rules;

import java.util.regex.Pattern;

/**
 * Restricts candidate versions to those that match specified version prefix.
 *
 * E.g. `prefix` "1.1" matches versions "1.1", "1.1.1", "1.1.2-qualifier", but doesn't match versions "1.10" or "1.2".
 *
 * This restriction also allows to specify `remainderRegex`, which is a regular expression pattern that's gonna be used
 * to match remaining part of the version (the part not covered by the `prefix`).
 *
 * E.g. `prefix` "1.1" with `remainderRegex` "redhat-\\d+" would match versions "1.1.redhat-00001" or "1.1-redhat-00001"
 * but not version "1.1.1.redhat-00001" (i.e. micro being added where it was originally missing is not accepted).
 */
public class VersionPrefixRestriction implements Restriction {

    private String prefixString;
    private Version prefixVersion;
    private Pattern remainderRegex;

    public VersionPrefixRestriction(String prefixString) {
        this(prefixString, null);
    }

    public VersionPrefixRestriction(String prefix, String remainderRegex) {
        this.prefixString = prefix;
        this.prefixVersion = Version.parse(prefix);
        if (remainderRegex != null) {
            this.remainderRegex = Pattern.compile(remainderRegex);
        }
    }

    @Override
    public boolean applies(String versionString) {
        Version version = Version.parse(versionString);
        if (!prefixVersion.isPrefixOf(version)) {
            return false;
        }
        if (remainderRegex != null) {
            String suffix;
            if (versionString.length() > prefixString.length()) {
                suffix = versionString.substring(prefixString.length() + 1);
            } else {
                suffix = "";
            }
            return remainderRegex.matcher(suffix).matches();
        }
        return true;
    }

    public String getPrefixString() {
        return prefixString;
    }
}
