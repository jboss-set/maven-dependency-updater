package org.jboss.set.mavendependencyupdater;

public enum VersionStream {
    ANY(-1, null),
    MAJOR(0, ANY),
    MINOR(1, MAJOR),
    MICRO(2, MINOR),
    QUALIFIER(null, MICRO)
    ;

    Integer level;
    VersionStream higher;

    VersionStream(Integer level, VersionStream higher) {
        this.level = level;
        this.higher = higher;
    }

    public Integer level() {
        return level;
    }

    public VersionStream higher() {
        return higher;
    }

}
