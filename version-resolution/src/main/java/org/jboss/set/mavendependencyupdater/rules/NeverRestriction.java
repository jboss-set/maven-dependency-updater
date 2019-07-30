package org.jboss.set.mavendependencyupdater.rules;

public class NeverRestriction implements Restriction {

    public static final NeverRestriction INSTANCE = new NeverRestriction();

    @Override
    public boolean applies(String version) {
        return false;
    }
}
