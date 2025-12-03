package org.example.places.api;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    private final OkHttpClient client;

    public ApiClient() {
        this.client = new OkHttpClient();
    }

    public CompletableFuture<String> getAsync(String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TouristApp/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("Unexpected code " + response));
                        return;
                    }
                    future.complete(responseBody.string());
                }
            }
        });

        return future;
    }
}
