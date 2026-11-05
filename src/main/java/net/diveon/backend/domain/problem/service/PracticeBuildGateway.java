package net.diveon.backend.domain.problem.service;

import java.util.Map;
import java.util.Set;

public interface PracticeBuildGateway {
    record Build(String id, PracticeBuildIdentity identity, String status) {}
    Build get(String id);
    Map<PracticeBuildIdentity, Build> discover(Set<PracticeBuildIdentity> identities);
    String imageUri(Build build);
}
