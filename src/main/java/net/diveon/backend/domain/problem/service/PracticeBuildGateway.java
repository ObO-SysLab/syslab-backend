package net.diveon.backend.domain.problem.service;

import java.util.Map;
import java.util.Set;

public interface PracticeBuildGateway {
    record Build(String id, PracticeBuildIdentity identity, String status) {}
    Build get(String id);
    Map<PracticeBuildIdentity, Build> discover(Set<PracticeBuildIdentity> identities);

    /**
     * Digest URI of the build's image. requiredTag (nullable) must be on the same digest; it proves the
     * trusted build applied the requested command policy.
     */
    String imageUri(Build build, String requiredTag);

    /**
     * Validated author-facing reason of a failed build ("CODE" or "CODE:command"), or null when the build
     * left none or it cannot be read. Never returns raw build output.
     */
    String failureDetail(PracticeBuildIdentity identity);

    /** The image exists but lacks the policy marker, so it is not the image that was requested. */
    class RestrictionUnverifiedException extends IllegalStateException {
        public RestrictionUnverifiedException() { super("Command restriction marker missing"); }
    }
}
