package org.jboss.set.mavenversionupdater;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.commonjava.maven.ext.core.impl.Version;

public class VersionComparison {

    public static String DELIMITER_REGEX = "[.\\-_]";

    public enum ComparisonLevel {
        ANY(-1, null),
        MAJOR(0, ANY),
        MINOR(1, MAJOR),
        MICRO(2, MINOR),
//        QUALIFIER(null, MICRO)
        ;

        Integer level;
        ComparisonLevel higher;

        ComparisonLevel(Integer level, ComparisonLevel higher) {
            this.level = level;
            this.higher = higher;
        }

        public ComparisonLevel up() {
            return higher;
        }
    }

    /**
     *
     * @param v1
     * @param v2
     * @param level
     * @return
     */
    public static boolean equalMmm(String v1, String v2, ComparisonLevel level) {
        String mmm1 = Version.getMMM(v1);
        String[] mmm1Split = mmm1.split(DELIMITER_REGEX);
        String mmm2 = Version.getMMM(v2);
        String[] mmm2Split = mmm2.split(DELIMITER_REGEX);

        int correctedLevel = level.level != null ? level.level
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
     *
     * @param level the segment of the version that must remain unchanged
     * @param originalVersion original artifact version
     * @param availableVersions available version
     * @return
     */
    public static Optional<org.eclipse.aether.version.Version> findLatest(VersionComparison.ComparisonLevel level,
                                                                          String originalVersion,
                                                                          List<org.eclipse.aether.version.Version> availableVersions) {
        return availableVersions.stream()
                .filter(v -> equalMmm(originalVersion, v.toString(), level))
                .max(Comparator.naturalOrder());
    }

}
