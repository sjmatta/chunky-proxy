package com.sjmatta.chunks.proxy;

import com.thedeanda.lorem.LoremIpsum;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@RestController
public class LoremIpsumController {

    private final LoremIpsumController self;
    private static final Random RANDOM = new Random();
    private static final int MAX_CHUNK_SIZE = 12;
    private static final int MIN_CHUNK_SIZE = 3;
    private static final int STREAM_DELAY_MS = 100;

    public LoremIpsumController(@Lazy LoremIpsumController self) {
        this.self = self;
    }

    @GetMapping("/stream")
    public ResponseBodyEmitter streamLoremIpsum() {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        self.asyncStreamLoremIpsum(emitter);
        return emitter;
    }

    @Async
    public void asyncStreamLoremIpsum(ResponseBodyEmitter emitter) {
        try {
            for (String chunk : generateLoremIpsumChunks()) {
                emitter.send(chunk + "\n");
                TimeUnit.MILLISECONDS.sleep(STREAM_DELAY_MS);  // Simulate delay between chunks
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private List<String> generateLoremIpsumChunks() {
        String loremIpsumText = LoremIpsum.getInstance().getParagraphs(2, 4);
        List<String> chunks = new ArrayList<>();
        int currentIndex = 0;

        while (currentIndex < loremIpsumText.length()) {
            int chunkSize = calculateChunkSize();
            int endIndex = Math.min(currentIndex + chunkSize, loremIpsumText.length());
            chunks.add(loremIpsumText.substring(currentIndex, endIndex));
            currentIndex = endIndex;
        }

        return chunks;
    }

    private int calculateChunkSize() {
        return MIN_CHUNK_SIZE + RANDOM.nextInt(MAX_CHUNK_SIZE - MIN_CHUNK_SIZE + 1);
    }
}
