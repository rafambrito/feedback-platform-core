package com.feedback.platform.event;

public interface PublicadorEvento {

    void publicarFeedbackCritico(String feedbackId, String alunoId, String professorId);
}