package net.diveon.backend.domain.problem.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.codebuild.CodeBuildClient;
import software.amazon.awssdk.services.codebuild.model.*;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.*;

class PracticeBuildAwsGatewayTest {
    final CodeBuildClient client = mock(CodeBuildClient.class);
    final EcrClient ecr = mock(EcrClient.class);
    final software.amazon.awssdk.services.s3.S3Client s3 = mock(software.amazon.awssdk.services.s3.S3Client.class);
    final PracticeBuildProperties props = new PracticeBuildProperties();
    final PracticeBuildAwsGateway gateway = new PracticeBuildAwsGateway(client, ecr, s3, props, "bucket", "registry", "repo");
    final PracticeBuildIdentity identity = new PracticeBuildIdentity(1, REQUEST);
    final String arn = "arn:aws:codebuild:ap-northeast-2:123456789012:build/" + BUILD;

    Build.Builder actual() {
        return Build.builder().id(BUILD).arn(arn).projectName(props.getProject()).buildStatus("SUCCEEDED")
                .source(ProjectSource.builder().type("S3").location("bucket/").build())
                .environment(ProjectEnvironment.builder().environmentVariables(EnvironmentVariable.builder()
                        .name("S3_KEY").type("PLAINTEXT").value(identity.sourceKey()).build()).build());
    }
    void returns(Build build) {
        when(client.batchGetBuilds(any(BatchGetBuildsRequest.class))).thenReturn(BatchGetBuildsResponse.builder().builds(build).build());
    }

    @Test void standardArnUsesActualApiIdentityAndUniqueSourceKey() {
        returns(actual().build());
        var build = gateway.get(arn);
        assertThat(build.identity()).isEqualTo(identity);
        assertThat(build.id()).isEqualTo(BUILD);
        verify(client).batchGetBuilds(argThat((BatchGetBuildsRequest r) -> r.ids().equals(java.util.List.of(BUILD))));
    }

    @ParameterizedTest @ValueSource(strings={"FAILED","FAULT","STOPPED","TIMED_OUT","IN_PROGRESS"})
    void readsActualStatus(String status) {
        returns(actual().buildStatus(status).build());
        assertThat(gateway.get(BUILD).status()).isEqualTo(status);
    }

    @Test void wrongProjectRejectedBeforeAwsCall() {
        assertThatThrownBy(() -> gateway.get(BUILD.replace("syslab-practice-build", "foreign"))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test void wrongAccountArnRejected() {
        returns(actual().build());
        assertThatThrownBy(() -> gateway.get(arn.replace("123456789012", "999999999999"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void wrongSourceBucketRejected() {
        returns(actual().source(ProjectSource.builder().type("S3").location("foreign/").build()).build());
        assertThatThrownBy(() -> gateway.get(BUILD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void missingEnvironmentIdentityRejected() {
        returns(actual().environment(ProjectEnvironment.builder().build()).build());
        assertThatThrownBy(() -> gateway.get(BUILD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void unknownStatusDoesNotBecomeFailureOrSuccess() {
        returns(actual().buildStatus("FUTURE_STATE").build());
        assertThatThrownBy(() -> gateway.get(BUILD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void discoveryPaginatesWithoutExplicitSortAndSkipsLegacyBuilds() {
        when(client.listBuildsForProject(any(ListBuildsForProjectRequest.class)))
                .thenReturn(ListBuildsForProjectResponse.builder().ids("legacy").nextToken("page2").build())
                .thenReturn(ListBuildsForProjectResponse.builder().ids(BUILD).build());
        when(client.batchGetBuilds(any(BatchGetBuildsRequest.class)))
                .thenReturn(BatchGetBuildsResponse.builder().builds(actual().environment(ProjectEnvironment.builder().build()).build()).build())
                .thenReturn(BatchGetBuildsResponse.builder().builds(actual().build()).build());
        assertThat(gateway.discover(Set.of(identity)).get(identity).id()).isEqualTo(BUILD);
        verify(client).listBuildsForProject(argThat((ListBuildsForProjectRequest r) -> "page2".equals(r.nextToken()) && r.sortOrder() == null));
    }

    @Test void discoveryIsBounded() {
        props.setDiscoveryPages(1);
        when(client.listBuildsForProject(any(ListBuildsForProjectRequest.class)))
                .thenReturn(ListBuildsForProjectResponse.builder().nextToken("more").build());
        assertThat(gateway.discover(Set.of(identity))).isEmpty();
        verify(client, times(1)).listBuildsForProject(any(ListBuildsForProjectRequest.class));
    }

    @Test void successUsesBuildSpecificTagAndReturnsDigest() {
        String tag = PracticeBuildIdentity.imageTag(BUILD);
        when(ecr.describeImages(any(DescribeImagesRequest.class))).thenReturn(DescribeImagesResponse.builder()
                .imageDetails(ImageDetail.builder().repositoryName("repo").imageTags(tag).imageDigest("sha256:" + "a".repeat(64)).build()).build());
        assertThat(gateway.imageUri(new PracticeBuildGateway.Build(BUILD, identity, "SUCCEEDED"), null)).isEqualTo(IMAGE);
        verify(ecr).describeImages(argThat((DescribeImagesRequest r) -> r.imageIds().getFirst().imageTag().equals(tag)));
    }

    @Test void restrictedBuildNeedsMarkerOnTheSameDigest() {
        String tag = PracticeBuildIdentity.imageTag(BUILD);
        String marker = PracticeCommandPolicy.markerTag(BUILD, java.util.List.of("cat", "ls"));
        var build = new PracticeBuildGateway.Build(BUILD, identity, "SUCCEEDED");
        when(ecr.describeImages(any(DescribeImagesRequest.class))).thenReturn(DescribeImagesResponse.builder()
                .imageDetails(ImageDetail.builder().repositoryName("repo").imageTags(tag, marker).imageDigest("sha256:" + "a".repeat(64)).build()).build());
        assertThat(gateway.imageUri(build, marker)).isEqualTo(IMAGE);
        assertThatThrownBy(() -> gateway.imageUri(build, marker.replace("eeba", "0000")))
                .isInstanceOf(PracticeBuildGateway.RestrictionUnverifiedException.class);
    }

    @Test void restrictedBuildWithoutMarkerIsUnverified() {
        String tag = PracticeBuildIdentity.imageTag(BUILD);
        when(ecr.describeImages(any(DescribeImagesRequest.class))).thenReturn(DescribeImagesResponse.builder()
                .imageDetails(ImageDetail.builder().repositoryName("repo").imageTags(tag).imageDigest("sha256:" + "a".repeat(64)).build()).build());
        assertThatThrownBy(() -> gateway.imageUri(new PracticeBuildGateway.Build(BUILD, identity, "SUCCEEDED"),
                PracticeCommandPolicy.markerTag(BUILD, java.util.List.of("ls"))))
                .isInstanceOf(PracticeBuildGateway.RestrictionUnverifiedException.class);
    }

    @Test void missingImageCannotBecomeReady() {
        when(ecr.describeImages(any(DescribeImagesRequest.class))).thenReturn(DescribeImagesResponse.builder().build());
        assertThatThrownBy(() -> gateway.imageUri(new PracticeBuildGateway.Build(BUILD, identity, "SUCCEEDED"), null)).isInstanceOf(IllegalStateException.class);
    }

    @Test void failureReasonIsReadFromTheRequestsResultObject() {
        when(s3.getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class))).thenReturn(
                software.amazon.awssdk.core.ResponseBytes.fromByteArray(
                        software.amazon.awssdk.services.s3.model.GetObjectResponse.builder().build(),
                        "{\"version\":1,\"code\":\"COMMAND_NOT_FOUND\",\"command\":\"xxd\"}".getBytes()));
        assertThat(gateway.failureDetail(identity)).isEqualTo("COMMAND_NOT_FOUND:xxd");
        verify(s3).getObjectAsBytes(argThat((software.amazon.awssdk.services.s3.model.GetObjectRequest r) ->
                "bucket".equals(r.bucket()) && identity.resultKey().equals(r.key())));
    }

    @Test void missingResultObjectMeansNoReason() {
        when(s3.getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenThrow(software.amazon.awssdk.services.s3.model.NoSuchKeyException.builder().build());
        assertThat(gateway.failureDetail(identity)).isNull();
    }

    @Test void onlyKnownCodesAndCommandNamesSurvive() {
        assertThat(PracticeBuildAwsGateway.parseFailure("{\"version\":1,\"code\":\"DOCKERFILE_FAILED\"}".getBytes())).isEqualTo("DOCKERFILE_FAILED");
        assertThat(PracticeBuildAwsGateway.parseFailure("{\"version\":1,\"code\":\"BUSYBOX_COMMAND\",\"command\":\"$(id)\"}".getBytes())).isEqualTo("BUSYBOX_COMMAND");
        assertThat(PracticeBuildAwsGateway.parseFailure("{\"version\":1,\"code\":\"<script>\"}".getBytes())).isNull();
        assertThat(PracticeBuildAwsGateway.parseFailure("{\"version\":2,\"code\":\"PUSH_FAILED\"}".getBytes())).isNull();
        assertThat(PracticeBuildAwsGateway.parseFailure("not json".getBytes())).isNull();
        assertThat(PracticeBuildAwsGateway.parseFailure(("{\"version\":1,\"code\":\"PUSH_FAILED\",\"pad\":\"" + "x".repeat(1100) + "\"}").getBytes())).isNull();
    }
}
