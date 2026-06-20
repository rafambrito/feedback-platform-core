package com.feedback.platform.service;

import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.domain.Criticidade;

public interface FeedbackService {

    FeedbackResponseDTO processarFeedback(FeedbackRequestDTO request);

    Criticidade avaliarCriticidade(FeedbackRequestDTO request);

    FeedbackResponseDTO buscarPorId(String id);
}
