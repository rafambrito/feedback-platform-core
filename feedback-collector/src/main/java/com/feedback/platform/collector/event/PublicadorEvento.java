package com.feedback.platform.collector.event;

public interface PublicadorEvento {

    void publicarFeedbackCritico(String feedbackId, String alunoId, String professorId);
}