package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import static org.mockito.Mockito.*;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.BUILD;

class PracticeBuildResultConsumerTest {
    final SqsClient sqs = mock(SqsClient.class);
    final PracticeBuildReconciler reconciler = mock(PracticeBuildReconciler.class);
    final PracticeBuildProperties props = new PracticeBuildProperties();
    final PracticeBuildResultConsumer consumer = new PracticeBuildResultConsumer(sqs, new ObjectMapper(), reconciler, props, "queue");

    void message(String body) {
        props.setEnabled(true);
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().body(body).receiptHandle("handle").messageId("id").build()).build());
    }
    String event(String status) {
        return "{\"source\":\"aws.codebuild\",\"detail-type\":\"CodeBuild Build State Change\",\"detail\":{\"project-name\":\"syslab-practice-build\",\"build-id\":\"" + BUILD + "\",\"build-status\":\"" + status + "\"}}";
    }

    @ParameterizedTest @ValueSource(strings={"SUCCEEDED","FAILED","FAULT","TIMED_OUT","STOPPED","IN_PROGRESS"})
    void verifiesEveryEventBeforeAck(String status) {
        message(event(status));
        consumer.poll();
        var order = inOrder(reconciler, sqs);
        order.verify(reconciler).accept(BUILD);
        order.verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test void apiOrDatabaseFailureIsNotAcknowledged() {
        message(event("SUCCEEDED"));
        doThrow(new IllegalStateException()).when(reconciler).accept(BUILD);
        consumer.poll();
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @ParameterizedTest @ValueSource(strings={"not json", "{}", "{\"detail\":{\"build-status\":\"SUCCEEDED\"}}"})
    void invalidOrLegacyMessagesRemainForRedrive(String body) {
        message(body);
        consumer.poll();
        verifyNoInteractions(reconciler);
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test void disabledDoesNotReceiveMessages() {
        consumer.poll();
        verifyNoInteractions(sqs, reconciler);
    }
}
