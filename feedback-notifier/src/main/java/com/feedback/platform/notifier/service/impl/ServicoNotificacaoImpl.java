package com.feedback.platform.notifier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.platform.dto.UrgencyNotification;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.repository.EnviadorNotificacao;
import com.feedback.platform.notifier.repository.RepositorioNotificacao;
import com.feedback.platform.notifier.service.ServicoNotificacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ServicoNotificacaoImpl implements ServicoNotificacao {

    private static final Logger LOG = Logger.getLogger(ServicoNotificacaoImpl.class);

    private final RepositorioNotificacao repositorio;
    private final EnviadorNotificacao enviadorNotificacao;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Inject
    public ServicoNotificacaoImpl(
            RepositorioNotificacao repositorio,
            EnviadorNotificacao enviadorNotificacao,
            ObjectMapper objectMapper,
            Validator validator) {
        this.repositorio = repositorio;
        this.enviadorNotificacao = enviadorNotificacao;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public Notificacao processarNotificacao(UrgencyNotification event) {
        LOG.infof("Processando notificação para professor: %s (urgência: %s)",
                event.professorId(), event.urgencia());

        // Validar evento
        Set<ConstraintViolation<UrgencyNotification>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validação falhou: " + details);
        }

        String notificacaoId = gerarChaveIdempotencia(event);
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

        Notificacao notificacaoPersistida = repositorio.salvarSeAusente(notificacao);
        if (notificacaoPersistida.status() == Notificacao.StatusNotificacao.ENVIADA) {
            LOG.infof("Notificação idempotente já enviada. id=%s", notificacaoId);
            return notificacaoPersistida;
        }

        // Enviar via SES
        try {
            enviadorNotificacao.enviar(notificacaoPersistida);
            repositorio.atualizarStatus(notificacaoId, Notificacao.StatusNotificacao.ENVIADA);
            LOG.infof("Notificação %s enviada via SES", notificacaoId);
            return repositorio.buscarPorId(notificacaoId);
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao enviar notificação %s via SES", notificacaoId);
            repositorio.atualizarStatus(notificacaoId, Notificacao.StatusNotificacao.FALHA);
            return repositorio.buscarPorId(notificacaoId);
        }
    }

    @Override
    public Notificacao buscarPorId(String id) {
        return repositorio.buscarPorId(id);
    }

    @Override
    public void simularRecebimento(String messageBody) {
        LOG.infof("Simulando recebimento de mensagem: %s", messageBody);
        try {
            UrgencyNotification event = parsearNotificacaoUrgente(messageBody);
            processarNotificacao(event);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Falha ao desserializar evento: %s", messageBody);
            throw new RuntimeException("Desserialização falhou", e);
        }
    }

    private UrgencyNotification parsearNotificacaoUrgente(String messageBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(messageBody);
        if (root != null && root.isObject() && root.has("detail")) {
            JsonNode detailNode = root.get("detail");
            if (detailNode == null || detailNode.isNull()) {
                throw new JsonProcessingException("Campo detail ausente no payload") {
                };
            }

            if (detailNode.isTextual()) {
                return objectMapper.readValue(detailNode.asText(), UrgencyNotification.class);
            }

            return objectMapper.treeToValue(detailNode, UrgencyNotification.class);
        }

        return objectMapper.treeToValue(root, UrgencyNotification.class);
    }

    private String gerarChaveIdempotencia(UrgencyNotification event) {
        String raw = event.feedbackId() + "|" + event.professorId() + "|" + event.urgencia();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
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

    private String construirCorpo(UrgencyNotification event) {
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
