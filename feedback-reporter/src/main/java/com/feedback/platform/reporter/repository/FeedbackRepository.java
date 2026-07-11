package com.feedback.platform.reporter.repository;

import com.feedback.platform.reporter.domain.FeedbackRecord;

import java.util.List;

public interface FeedbackRepository {

    List<FeedbackRecord> findByProfessorId(String professorId);

    List<FeedbackRecord> findByCursoId(String cursoId);

    List<FeedbackRecord> findByCursoIdAndProfessorId(String cursoId, String professorId);
}
