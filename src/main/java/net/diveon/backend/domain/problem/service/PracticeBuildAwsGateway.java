package net.diveon.backend.domain.problem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.codebuild.CodeBuildClient;
import software.amazon.awssdk.services.codebuild.model.BatchGetBuildsRequest;
import software.amazon.awssdk.services.codebuild.model.ListBuildsForProjectRequest;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import java.time.Duration;
import java.util.*;

@Service
@Profile("prod")
public class PracticeBuildAwsGateway implements PracticeBuildGateway {
    private final CodeBuildClient codebuild;
    private final EcrClient ecr;
    private final PracticeBuildProperties properties;
    private final String bucket, registry, repository;

    public PracticeBuildAwsGateway(CodeBuildClient codebuild, EcrClient ecr, PracticeBuildProperties properties,
            @Value("${aws.s3.bucket-practice-dockerfile}") String bucket,
            @Value("${aws.ecr.practice-registry}") String registry,
            @Value("${aws.ecr.practice-repository}") String repository) {
        this.codebuild = codebuild;
        this.ecr = ecr;
        this.properties = properties;
        this.bucket = bucket;
        this.registry = registry;
        this.repository = repository;
    }

    @Override
    public Build get(String id) {
        String canonical = id != null && id.startsWith("arn:") && id.contains(":build/")
                ? id.substring(id.indexOf(":build/") + 7) : id;
        PracticeBuildIdentity.imageTag(canonical);
        if (!canonical.startsWith(properties.getProject() + ":"))
            throw new IllegalArgumentException("Unexpected CodeBuild project");
        var response = codebuild.batchGetBuilds(BatchGetBuildsRequest.builder().ids(canonical).build());
        var actual = response.builds().stream().filter(b -> canonical.equals(b.id())).findFirst()
                .orElseThrow(() -> new IllegalStateException("CodeBuild result unavailable"));
        if (id.startsWith("arn:") && !id.equals(actual.arn()))
            throw new IllegalArgumentException("Unexpected CodeBuild ARN");
        return verified(actual);
    }

    private Build verified(software.amazon.awssdk.services.codebuild.model.Build build) {
        if (!properties.getProject().equals(build.projectName()) || build.id() == null
                || !build.id().startsWith(properties.getProject() + ":") || build.environment() == null
                || build.source() == null || !"S3".equals(build.source().typeAsString()))
            throw new IllegalArgumentException("Unexpected CodeBuild source or project");
        PracticeBuildIdentity.imageTag(build.id());
        var keys = build.environment().environmentVariables().stream()
                .filter(v -> "S3_KEY".equals(v.name()) && "PLAINTEXT".equals(v.typeAsString())).toList();
        if (keys.size() != 1) throw new IllegalArgumentException("Missing unique S3_KEY");
        var identity = PracticeBuildIdentity.fromKey(keys.getFirst().value());
        String location = build.source().location();
        if (!(bucket + "/").equals(location) && !(bucket + "/" + identity.sourceKey()).equals(location))
            throw new IllegalArgumentException("Unexpected source bucket");
        String status = build.buildStatusAsString();
        if (status == null || !Set.of("IN_PROGRESS", "SUCCEEDED", "FAILED", "FAULT", "STOPPED", "TIMED_OUT").contains(status))
            throw new IllegalArgumentException("Unknown CodeBuild status");
        return new Build(build.id(), identity, status);
    }

    @Override
    public Map<PracticeBuildIdentity, Build> discover(Set<PracticeBuildIdentity> identities) {
        Map<PracticeBuildIdentity, Build> found = new HashMap<>();
        if (identities.isEmpty()) return found;
        String token = null;
        Set<String> seen = new HashSet<>();
        for (int page = 0; page < properties.getDiscoveryPages(); page++) {
            // AWS rejects explicit sortOrder for projects with >100 builds.
            var listing = codebuild.listBuildsForProject(ListBuildsForProjectRequest.builder()
                    .projectName(properties.getProject()).nextToken(token).build());
            if (!listing.ids().isEmpty()) {
                var builds = codebuild.batchGetBuilds(BatchGetBuildsRequest.builder().ids(listing.ids()).build());
                for (var build : builds.builds()) {
                    try {
                        var candidate = verified(build);
                        if (identities.contains(candidate.identity())) found.putIfAbsent(candidate.identity(), candidate);
                    } catch (IllegalArgumentException ignored) {
                        // Never adopt historical or foreign sources as current requests.
                    }
                }
            }
            token = listing.nextToken();
            if (found.keySet().containsAll(identities) || token == null || token.isBlank() || !seen.add(token)) break;
        }
        return found;
    }

    @Override
    public String imageUri(Build build) {
        String tag = PracticeBuildIdentity.imageTag(build.id());
        var response = ecr.describeImages(DescribeImagesRequest.builder().repositoryName(repository)
                .imageIds(ImageIdentifier.builder().imageTag(tag).build())
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(5))
                        .apiCallAttemptTimeout(Duration.ofSeconds(2))).build());
        if (response.imageDetails().size() != 1) throw new IllegalStateException("Image is not available");
        var image = response.imageDetails().getFirst();
        if (!repository.equals(image.repositoryName()) || !image.imageTags().contains(tag)
                || image.imageDigest() == null || !image.imageDigest().matches("sha256:[0-9a-f]{64}"))
            throw new IllegalStateException("Image digest could not be verified");
        return registry + "/" + repository + "@" + image.imageDigest();
    }
}
