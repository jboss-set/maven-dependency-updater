package org.jboss.set.mavendependencyupdater.utils;

import org.commonjava.maven.ext.core.impl.Version;
import org.jboss.logging.Logger;
import org.jboss.set.mavendependencyupdater.VersionStream;

public class VersionUtils {

    public static String DELIMITER_REGEX = "[.\\-_]";

    /**
     * Do given versions belong to the same stream?
     * <p>
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

            try {
                int x1 = mmm1Split.length > i ? Integer.parseInt(mmm1Split[i]) : 0;
                int x2 = mmm2Split.length > i ? Integer.parseInt(mmm2Split[i]) : 0;

                if (x1 != x2) {
                    return false;
                }
            } catch (NumberFormatException e) {
                if (!mmm1Split[i].equals(mmm2Split[i])) {
                    return false;
                }
            }
        }
        return true;
    }

}
