package com.ufsc.webchat.server;

import java.lang.reflect.Method;

public class RetryProcessor {
    public static void executeWithRetry(Object target, Method method, Object[] args) throws Exception {
        Retry retryAnnotation = method.getAnnotation(Retry.class);
        int maxAttempts = retryAnnotation.maxAttempts();
        long delayMillis = retryAnnotation.delayMillis();
        Class<? extends Throwable> exceptionClass = retryAnnotation.onException();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                method.invoke(target, args);
                return; // O método foi executado com sucesso; sair do loop de retentativa
            } catch (Throwable e) {
                if (!exceptionClass.isInstance(e) || attempt == maxAttempts) {
                    throw e; // Lançar exceção se não for da classe especificada ou se atingir o máximo de tentativas
                }
                // Caso contrário, agende a próxima tentativa após o atraso especificado
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
