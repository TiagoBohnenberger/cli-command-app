package io.github.tiagobohnenberger.cli.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import com.google.gson.Gson;
import io.github.tiagobohnenberger.cli.util.BeanUtils;

public class JsonBody {

    private final static Gson gson = BeanUtils.getInstanceByType(Gson.class);

    public static <T> HttpRequest.BodyPublisher publish(T obj) {
        return new JsonBodyPublisher<>(obj, gson);
    }

    public static <T> HttpResponse.BodyHandler<T> handle(Type targetType) {
        return new JsonBodyHandler<>(targetType, gson);
    }

    private static class JsonBodyPublisher<T> implements HttpRequest.BodyPublisher {

        private final byte[] jsonBytes;

        private JsonBodyPublisher(T body, Gson gson) {
            this.jsonBytes = gson.toJson(body, body.getClass()).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public long contentLength() {
            return this.jsonBytes.length;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            try (var publisher = new SubmissionPublisher<ByteBuffer>()) {
                publisher.subscribe(subscriber);

                publisher.submit(ByteBuffer.wrap(jsonBytes));
            }
        }
    }

    private static class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {
        private final Gson gson;
        private final Type targetType;

        public JsonBodyHandler(Type targetType, Gson gson) {
            this.gson = gson;
            this.targetType = targetType;
        }

        @Override
        public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
            HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();


            return HttpResponse.BodySubscribers.mapping(
                    upstream,
                    inputStream -> gson.fromJson(new InputStreamReader(inputStream), targetType));
        }
    }
}