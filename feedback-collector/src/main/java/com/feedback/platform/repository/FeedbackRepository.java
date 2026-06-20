package com.feedback.platform.repository;

import com.feedback.platform.domain.Feedback;

public interface FeedbackRepository {

    void save(Feedback feedback);

    Feedback findById(String id);
}
