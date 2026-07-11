package com.feedback.platform.notifier.service;

import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.dto.FeedbackEventDTO;

public interface NotificationService {

    /**
     * Processa um evento de feedback crítico e envia notificação
     * @param event Evento de feedback com dados de urgência
     * @return Notificacao persistida
     */
    Notificacao procesarNotificacao(FeedbackEventDTO event);

    /**
     * Consulta notificação por ID
     * @param id Identificador da notificação
     * @return Notificacao encontrada
     */
    Notificacao buscarPorId(String id);

    /**
     * Simula recebimento de mensagem para testes
     * @param messageBody JSON da mensagem
     */
    void simularRecebimento(String messageBody);
}
