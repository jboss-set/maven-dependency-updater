package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates and records digests of modified files (currently only top level pom.xml).
 * <p>
 * This should prevent us from creating multiple pull requests in case when multiple dependencies share common
 * version variable and therefore created PRs would be identical.
 * <p>
 * TODO: This only solves identical patches problem during a single run, some kind of permanent store is needed to
 *  solve this for repeated runs.
 */
public class PatchDigestRecorder {

    private static final String POM_XML = "pom.xml";

    private Map<Map<String, String>, Pair<ArtifactRef, String>> patchDigests = new HashMap<>();

    /**
     * Records pom.xml digest. Returns previously recorded upgrade with the same digest, or null current digest
     * is unique.
     *
     * @param pomFile    modified pom.xml
     * @param ref        upgraded artifact
     * @param newVersion version that artifact is upgraded to
     * @return previously recorded upgrade
     */
    public Pair<ArtifactRef, String> recordPatchDigest(File pomFile, ArtifactRef ref, String newVersion)
            throws IOException {
        String hash = DigestUtils.sha1Hex(new FileInputStream(pomFile));
        Map<String, String> patchDigest = Collections.singletonMap(POM_XML, hash);
        return patchDigests.putIfAbsent(patchDigest, Pair.of(ref, newVersion));
    }

}
