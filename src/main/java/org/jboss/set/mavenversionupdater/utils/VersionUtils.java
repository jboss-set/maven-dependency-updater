package org.jboss.set.mavenversionupdater.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.commonjava.maven.ext.core.impl.Version;
import org.jboss.set.mavenversionupdater.VersionStream;

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
     * @param stream the highest segment of the version that is allowed to change
     * @param originalVersion original artifact version
     * @param availableVersions available version
     * @return latest available version in given stream
     */
    public static Optional<org.eclipse.aether.version.Version> findLatest(VersionStream stream,
                                                                          String originalVersion,
                                                                          List<org.eclipse.aether.version.Version> availableVersions) {
        if (VersionStream.ANY.equals(stream)) { // ANY => consider all available versions
            return availableVersions.stream().max(Comparator.naturalOrder());
        }

        // otherwise consider only versions in given stream
        return availableVersions.stream()
                .filter(v -> equalMmm(originalVersion, v.toString(), stream.higher()))
                .max(Comparator.naturalOrder());
    }

}
