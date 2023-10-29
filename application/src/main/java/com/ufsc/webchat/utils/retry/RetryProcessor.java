package com.ufsc.webchat.utils.retry;

import static java.util.Objects.isNull;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryProcessor {
	private static final Logger logger = LoggerFactory.getLogger(RetryProcessor.class);

	public static void execute(Object target, Method method, Object[] args, MaxAttemptCallable whenMaxAttempts) throws Exception {
		Retry annotation = method.getAnnotation(Retry.class);

		if (isNull(annotation)) {
			logger.error("Method does not contain retry annotation");
			return;
		}

		int maxAttempts = annotation.maxAttempts();
		long delayMillis = annotation.delayMillis();
		Class<? extends Throwable> exceptionClass = annotation.onException();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				method.invoke(target, args);
				return; // O método foi executado com sucesso; sair do loop de retentativa
			} catch (Throwable e) {
				if (!exceptionClass.isInstance(e)) {
					throw e; // Lançar exceção se não for da classe especificada
				} else if (attempt == maxAttempts) { // Se passou das tentativas máximas, chamar função de tratamento especificada.
					whenMaxAttempts.call(e.getMessage());
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

