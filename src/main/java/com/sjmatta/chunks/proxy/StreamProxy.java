package com.sjmatta.chunks.proxy;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@RestController
public class StreamProxy {

    private static final String STREAM_SOURCE_URL = "http://localhost:8080/stream";
    private final StreamProxy self;
    private final OkHttpClient client;

    public StreamProxy(@Lazy StreamProxy self) {
        this.self = self;
        this.client = new OkHttpClient();
    }

    @GetMapping("/stream-proxy")
    public ResponseBodyEmitter proxyStreamToClient() {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        self.asyncStreamDataToEmitter(emitter);
        return emitter;
    }

    @Async
    public void asyncStreamDataToEmitter(ResponseBodyEmitter emitter) {
        try (Response response = fetchStreamDataFromSource(STREAM_SOURCE_URL)) {
            streamResponseToEmitter(response, emitter);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private Response fetchStreamDataFromSource(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        return client.newCall(request).execute();
    }

    private void streamResponseToEmitter(Response response, ResponseBodyEmitter emitter)
        throws IOException {
        if (response.body() != null) {
            try (BufferedSource source = response.body().source()) {
                streamChunks(source, emitter);
            }
        }
        emitter.complete();
    }

    private void streamChunks(BufferedSource source, ResponseBodyEmitter emitter)
        throws IOException {
        while (!source.exhausted()) {
            String chunk = source.readUtf8Line();
            if (chunk != null) {
                emitter.send(chunk + "\n");
            }
        }
    }
}
