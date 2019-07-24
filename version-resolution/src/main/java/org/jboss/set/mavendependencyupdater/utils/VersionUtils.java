package org.jboss.set.mavendependencyupdater.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.commonjava.maven.ext.core.impl.Version;
import org.jboss.set.mavendependencyupdater.VersionStream;
import org.jboss.set.mavendependencyupdater.rules.Restriction;
import org.jboss.set.mavendependencyupdater.rules.VersionPrefixRestriction;

public class VersionUtils {

    public static String DELIMITER_REGEX = "[.\\-_]";

    /**
     * Do given versions belong to the same stream stream?
     *
     * E.g.: "1.1.2" and 1.1.3" belong to the same MINOR stream, but not into the same MICRO stream.
     */
    public static boolean equalMmm(String v1, String v2, VersionStream stream) {
        String mmm1 = Version.getMMM(v1);
        String[] mmm1Split = mmm1.split(DELIMITER_REGEX);
        String mmm2 = Version.getMMM(v2);
        String[] mmm2Split = mmm2.split(DELIMITER_REGEX);

        int correctedLevel = stream.level() != null ? stream.level()
                : Math.max(mmm1Split.length, mmm2Split.length) - 1; // correct null value

        for (int i = 0; i <= correctedLevel; i++) {
            if (mmm1Split.length <= i && mmm2Split.length <= i) { // segments not available on either side, return true
                return true;
            }

            int x1 = mmm1Split.length > i ? Integer.parseInt(mmm1Split[i]) : 0;
            int x2 = mmm2Split.length > i ? Integer.parseInt(mmm2Split[i]) : 0;

            if (x1 != x2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Searches given list of available versions for the latest version in given stream.
     *
     * @param stream the highest segment of the version that is allowed to change.
     *               This parameter is ignored if `restrictions` list contains `VersionPrefixRestriction`.
     * @param restrictions list of restrictions that versions must satisfy.
     *                     If `VersionPrefixRestriction` is present, `stream` parameter is ignored.
     * @param originalVersion original artifact version.
     * @param availableVersions available versions.
     * @return latest available version in given stream.
     */
    public static Optional<org.eclipse.aether.version.Version> findLatest(VersionStream stream,
                                                                          List<Restriction> restrictions,
                                                                          String originalVersion,
                                                                          List<org.eclipse.aether.version.Version> availableVersions) {
        boolean restrictedPrefix = restrictions.stream().anyMatch(r -> r instanceof VersionPrefixRestriction);

        Stream<org.eclipse.aether.version.Version> workingStream = availableVersions.stream();

        if (!restrictedPrefix && !VersionStream.ANY.equals(stream)) { // don't filter by stream if prefix restriction is present
            workingStream = workingStream.filter(v -> equalMmm(originalVersion, v.toString(), stream.higher()));
        }

        for (Restriction restriction: restrictions) {
            workingStream = workingStream.filter(v -> restriction.applies(v.toString()));
        }

        return workingStream.max(Comparator.naturalOrder());
    }
}
