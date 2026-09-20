package net.chillioil.aesthetic.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MojangSkinFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/MojangSkinFetcher");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static CompletableFuture<Optional<SkinData>> fetchSkin(String username, String requestedVariant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Resolve username to UUID
                String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + username.trim();
                HttpRequest uuidReq = HttpRequest.newBuilder()
                    .uri(URI.create(uuidUrl))
                    .header("User-Agent", "ChilliOil/1.1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

                HttpResponse<String> uuidResp = HTTP_CLIENT.send(uuidReq, HttpResponse.BodyHandlers.ofString());
                if (uuidResp.statusCode() != 200 || uuidResp.body() == null || uuidResp.body().isBlank()) {
                    LOGGER.warn("Mojang UUID lookup returned status {} for {}", uuidResp.statusCode(), username);
                    return Optional.empty();
                }

                JsonObject uuidJson = JsonParser.parseString(uuidResp.body()).getAsJsonObject();
                if (!uuidJson.has("id")) {
                    return Optional.empty();
                }
                String rawUuid = uuidJson.get("id").getAsString();
                String actualName = uuidJson.has("name") ? uuidJson.get("name").getAsString() : username;

                // Step 2: Fetch profile with textures & signature
                String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + rawUuid + "?unsigned=false";
                HttpRequest profileReq = HttpRequest.newBuilder()
                    .uri(URI.create(profileUrl))
                    .header("User-Agent", "ChilliOil/1.1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

                HttpResponse<String> profileResp = HTTP_CLIENT.send(profileReq, HttpResponse.BodyHandlers.ofString());
                if (profileResp.statusCode() != 200 || profileResp.body() == null) {
                    LOGGER.warn("Mojang session lookup returned status {} for UUID {}", profileResp.statusCode(), rawUuid);
                    return Optional.empty();
                }

                JsonObject profileJson = JsonParser.parseString(profileResp.body()).getAsJsonObject();
                if (!profileJson.has("properties")) {
                    return Optional.empty();
                }

                JsonArray properties = profileJson.getAsJsonArray("properties");
                for (JsonElement elem : properties) {
                    if (elem.isJsonObject()) {
                        JsonObject prop = elem.getAsJsonObject();
                        if (prop.has("name") && "textures".equals(prop.get("name").getAsString())) {
                            String val = prop.has("value") ? prop.get("value").getAsString() : null;
                            String sig = prop.has("signature") ? prop.get("signature").getAsString() : null;
                            if (val != null) {
                                return Optional.of(new SkinData(requestedVariant, actualName, "MOJANG_USER", val, sig));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to fetch Mojang skin for {}", username, e);
            }
            return Optional.empty();
        });
    }
}
