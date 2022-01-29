package org.bk.service;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import reactor.core.publisher.Mono;

public class ApiFutureUtil {
    public static <T> Mono<T> toMono(ApiFuture<T> apiFuture) {
        return Mono.create(sink -> {
            ApiFutureCallback<T> callback = new ApiFutureCallback<T>() {
                @Override
                public void onFailure(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onSuccess(T result) {
                    sink.success(result);
                }
            };
            ApiFutures.addCallback(apiFuture, callback, MoreExecutors.directExecutor());
        });
    }
}
