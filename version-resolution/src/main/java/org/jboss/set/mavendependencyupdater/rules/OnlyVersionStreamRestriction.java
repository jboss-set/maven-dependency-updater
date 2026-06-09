package org.jboss.set.mavendependencyupdater.rules;

import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.utils.VersionUtils;

public class OnlyVersionStreamRestriction extends VersionStreamRestriction {

    public OnlyVersionStreamRestriction(VersionStream stream) {
        super(stream);
    }

}
