package nars.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jcog.Log;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Open-AI/Ollama HTTP JSON API client
 */
public class LM {

    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final ObjectMapper JSON = new ObjectMapper();
    public final String url, model;

    protected String sysPrompt;

    public boolean json;

    public static final Logger logger = Log.log(LM.class);

    public LM(String url, String model) {
        this.url = url;
        this.model = model;
    }

    private Map<String, ? extends Serializable> queryParam(String model, String prompt, @Nullable String systemPrompt) {
        var x = new HashMap<>(Map.of(
            "model", model,
            "prompt", prompt,
            "stream", false
            //TODO temperature?
            //TODO # output tokens
            //TODO ...
        ));
        if (systemPrompt!=null)
            x.put("system", systemPrompt);
        if (json)
            x.put("format", "json");
        return x;
    }

    public void setSysPrompt(String sysPrompt) {
        this.sysPrompt = sysPrompt;
    }

    public String query(String prompt) {
        return query(model, prompt, sysPrompt);
    }

    public String query(String prompt, @Nullable String sysPrompt) {
        return query(model, prompt, sysPrompt);
    }

    public String query(String model, String prompt, @Nullable String systemPrompt) {
        var x = queryParam(model, prompt, systemPrompt);
        try {
            return response(x, response(x));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String response(Map<String, ? extends Serializable> x, HttpResponse<String> Y) throws JsonProcessingException {
        var y = txt(Y);
        logger.info("{}\n{}\n\n", x, y);
        return y;
    }

    private String txt(HttpResponse<String> y) throws JsonProcessingException {
        return JSON.readTree(y.body()).at("/response").asText();
    }

    private HttpResponse<String> response(Map<String, ? extends Serializable> x) throws IOException {
        return response(JSON.writeValueAsBytes(x));
    }

    private HttpResponse<String> response(byte[] xs) throws IOException {
        try {
             var response = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(xs))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200)
                throw new IOException("API request failed with status code: " + response.statusCode());
            return response;
        } catch (Exception e) {
            throw e instanceof IOException r ? r : new IOException("Error getting LM feedback: " + e.getMessage(), e);
        }
    }

    public final CompletableFuture<String> queryAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> query(prompt));
    }

    public CompletableFuture<String> queryAsync(String prompt, String sysPrompt) {
        return CompletableFuture.supplyAsync(() -> query(prompt, sysPrompt));
    }
}
