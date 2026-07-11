package com.feedback.platform.notifier.repository;

import com.feedback.platform.notifier.domain.Notificacao;

public interface NotificationRepository {

    /**
     * Salva notificação em DynamoDB
     * @param notificacao Notificacao a persistir
     * @return Notificacao persistida
     */
    Notificacao salvar(Notificacao notificacao);

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

    /**
     * Envia notificação via SES
     * @param notificacao Notificacao a enviar
     */
    void enviarViaSES(Notificacao notificacao);
}
