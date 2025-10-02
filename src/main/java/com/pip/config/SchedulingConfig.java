package com.pip.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração para habilitar tarefas agendadas
 * 
 * @author Luiz Gustavo Finotello
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Habilita o suporte a @Scheduled
}
