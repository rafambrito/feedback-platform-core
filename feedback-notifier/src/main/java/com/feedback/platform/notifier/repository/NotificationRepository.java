package com.feedback.platform.notifier.repository;

import com.feedback.platform.notifier.domain.Notificacao;

public interface NotificationRepository {

    /**
     * Salva notificação em DynamoDB caso ainda não exista
     * @param notificacao Notificacao a persistir
     * @return Notificacao persistida ou notificação existente
     */
    Notificacao salvarSeAusente(Notificacao notificacao);

    /**
     * Busca notificação por ID
     * @param id Identificador da notificação
     * @return Notificacao encontrada ou null
     */
    Notificacao buscarPorId(String id);

    /**
     * Atualiza status da notificação
     * @param id Identificador da notificação
     * @param status Novo status
     */
    void atualizarStatus(String id, Notificacao.StatusNotificacao status);
}
