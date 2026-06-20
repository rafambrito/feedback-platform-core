package com.feedback.platform.event;

public interface EventPublisher {

    void publishCriticalFeedback(String feedbackId, String alunoId, String professorId);
}