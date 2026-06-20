package com.feedback.platform.service;

import com.feedback.platform.domain.Criticidade;
import com.feedback.platform.domain.Feedback;
import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.event.EventPublisher;
import com.feedback.platform.repository.FeedbackRepository;
import com.feedback.platform.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    FeedbackRepository repository;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    FeedbackServiceImpl service;

    @Captor
    ArgumentCaptor<Feedback> captor;

    @Test
    void sucesso_processarFeedback_chamaRepositorioERetornaDTO() {
        FeedbackRequestDTO request = new FeedbackRequestDTO("c1","a1","p1",5,"bom");

        FeedbackResponseDTO response = service.processarFeedback(request);

        assertNotNull(response.id());
        assertEquals(5, response.nota());
        verify(repository, times(1)).save(captor.capture());
        verifyNoInteractions(eventPublisher);
        Feedback saved = captor.getValue();
        assertEquals("c1", saved.cursoId());
        assertEquals(Criticidade.MEDIA, saved.criticidade());
    }

    @Test
    void erro_repositorio_propagado() {
        doThrow(new RuntimeException("db"))
                .when(repository).save(any());

        FeedbackRequestDTO request = new FeedbackRequestDTO("c1","a1","p1",7,"ok");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.processarFeedback(request));
        assertEquals("db", ex.getMessage());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void regra_negocio_nota_baixa_e_alta_resulta_em_criticidade_correta() {
        FeedbackRequestDTO low = new FeedbackRequestDTO("c1","a1","p1",2,"ruim");
        FeedbackResponseDTO rLow = service.processarFeedback(low);
        assertEquals(Criticidade.ALTA, rLow.criticidade());
        verify(eventPublisher).publishCriticalFeedback(eq(rLow.id()), eq("a1"), eq("p1"));

        FeedbackRequestDTO high = new FeedbackRequestDTO("c1","a1","p1",8,"bom");
        FeedbackResponseDTO rHigh = service.processarFeedback(high);
        assertEquals(Criticidade.BAIXA, rHigh.criticidade());
    }

    @Test
    void nota_tres_nao_publica_evento_critico() {
        FeedbackRequestDTO request = new FeedbackRequestDTO("c1", "a1", "p1", 3, "regular");

        FeedbackResponseDTO response = service.processarFeedback(request);

        assertEquals(Criticidade.MEDIA, response.criticidade());
        verify(eventPublisher, never()).publishCriticalFeedback(anyString(), anyString(), anyString());
    }
}
