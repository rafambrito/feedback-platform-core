package com.feedback.platform.notifier.repository;

import com.feedback.platform.notifier.domain.Notificacao;

public interface NotificationSender {

    void enviar(Notificacao notificacao);
}
