package org.jboss.set.mavendependencyupdater.rules;

import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.utils.VersionUtils;

public class VersionStreamRestriction implements Restriction {

    private VersionStream stream;

    public VersionStreamRestriction(VersionStream stream) {
        this.stream = stream;
    }

    @Override
    public boolean applies(String version, String originalVersion) {
        return VersionUtils.equalMmm(version, originalVersion, stream.higher());
    }
}
