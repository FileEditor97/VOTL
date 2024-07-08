package dev.fileeditor.votl.servlet.oauth2;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class OkHttpResponseFuture implements Callback {
	public final CompletableFuture<Response> future = new CompletableFuture<>();

	public OkHttpResponseFuture() {}

	@Override
	public void onResponse(Call call, Response response) throws IOException {
		future.complete(response);
	}

	@Override
	public void onFailure(Call call, IOException e) {
		future.completeExceptionally(e);
	}
}
