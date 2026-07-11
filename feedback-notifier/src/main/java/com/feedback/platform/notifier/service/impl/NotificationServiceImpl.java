package com.feedback.platform.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.dto.FeedbackEventDTO;
import com.feedback.platform.notifier.repository.NotificationRepository;
import com.feedback.platform.notifier.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Inject
    public NotificationServiceImpl(
            NotificationRepository repository,
            ObjectMapper objectMapper,
            Validator validator) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public Notificacao procesarNotificacao(FeedbackEventDTO event) {
        LOG.infof("Processando notificação para professor: %s (urgência: %s)",
                event.professorId(), event.urgencia());

        // Validar evento
        Set<ConstraintViolation<FeedbackEventDTO>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validação falhou: " + details);
        }

        // Construir notificação
        String notificacaoId = UUID.randomUUID().toString();
        Notificacao notificacao = new Notificacao(
                notificacaoId,
                event.feedbackId(),
                event.professorId(),
                event.alunoId(),
                event.urgencia(),
                gerarEmailProfessor(event.professorId()),
                construirAssunto(event.urgencia()),
                construirCorpo(event),
                Notificacao.StatusNotificacao.PENDENTE,
                Instant.now(),
                null
        );

        // Persistir
        Notificacao notificacaoPersistida = repository.salvar(notificacao);
        LOG.infof("Notificação %s persistida com sucesso", notificacaoId);

        // Enviar via SES (delegado ao repository)
        try {
            repository.enviarViaSES(notificacaoPersistida);
            LOG.infof("Notificação %s enviada via SES", notificacaoId);
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao enviar notificação %s via SES", notificacaoId);
            // Atualizar status para FALHA, mas continuar
            repository.atualizarStatus(notificacaoId, Notificacao.StatusNotificacao.FALHA);
        }

        return notificacaoPersistida;
    }

    @Override
    public Notificacao buscarPorId(String id) {
        return repository.buscarPorId(id);
    }

    @Override
    public void simularRecebimento(String messageBody) {
        LOG.infof("Simulando recebimento de mensagem: %s", messageBody);
        try {
            FeedbackEventDTO event = objectMapper.readValue(messageBody, FeedbackEventDTO.class);
            procesarNotificacao(event);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Falha ao desserializar evento: %s", messageBody);
            throw new RuntimeException("Desserialização falhou", e);
        }
    }

    private String gerarEmailProfessor(String professorId) {
        // Simples geração de email (em produção, seria consultado em banco de dados)
        return professorId + "@universidade.edu.br";
    }

    private String construirAssunto(String urgencia) {
        return switch (urgencia) {
            case "CRITICA" -> "🚨 CRÍTICA: Feedback Urgente Recebido";
            case "ALTA" -> "⚠️ ALTA: Feedback Importante Recebido";
            case "MEDIA" -> "ℹ️ MÉDIA: Feedback Recebido";
            default -> "📬 BAIXA: Feedback Recebido";
        };
    }

    private String construirCorpo(FeedbackEventDTO event) {
        return String.format(
                """
                        Olá Professor %s,
                        
                        Um novo feedback foi recebido com urgência %s.
                        
                        Detalhes:
                        - ID do Feedback: %s
                        - ID do Aluno: %s
                        - Urgência: %s
                        - Data: %s
                        
                        Por favor, verifique o sistema para mais detalhes.
                        
                        Atenciosamente,
                        Sistema de Feedback
                        """,
                event.professorId(),
                event.urgencia(),
                event.feedbackId(),
                event.alunoId(),
                event.urgencia(),
                Instant.now()
        );
    }
}
