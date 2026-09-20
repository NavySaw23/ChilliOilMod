package net.chillioil.aesthetic.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class MineSkinFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/MineSkinFetcher");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    // Global timestamp of last free-tier request to prevent spamming/getting banned
    private static final AtomicReference<Instant> LAST_FREE_REQUEST = new AtomicReference<>(Instant.EPOCH);

    public record ValidationResult(boolean valid, String message, byte[] imageBytes) {}

    public static CompletableFuture<ValidationResult> validateSkinUrl(String url) {
        return CompletableFuture.supplyAsync(() -> {
            if (url == null || url.isBlank()) {
                return new ValidationResult(false, "Skin URL cannot be empty.", null);
            }

            String cleanUrl = url.trim();

            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(cleanUrl))
                    .header("User-Agent", "ChilliOil/1.1.0")
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

                HttpResponse<byte[]> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200 || resp.body() == null || resp.body().length == 0) {
                    return new ValidationResult(false, "Failed to download image from URL (HTTP " + resp.statusCode() + ").", null);
                }

                byte[] bytes = resp.body();

                // Verify PNG file signature (magic bytes: 89 50 4E 47 0D 0A 1A 0A)
                if (bytes.length < 8 || (bytes[0] & 0xFF) != 0x89 || bytes[1] != 0x50 || bytes[2] != 0x4E || bytes[3] != 0x47) {
                    return new ValidationResult(false, "The downloaded file is not a valid PNG image.", null);
                }

                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) {
                    return new ValidationResult(false, "The downloaded file is not a valid image.", null);
                }

                int w = img.getWidth();
                int h = img.getHeight();
                if ((w == 64 && h == 64) || (w == 64 && h == 32) || (w == 128 && h == 128)) {
                    return new ValidationResult(true, "Valid skin dimensions (" + w + "x" + h + ").", bytes);
                } else {
                    return new ValidationResult(false, "Invalid skin dimensions: " + w + "x" + h + " (expected 64x64 or 64x32).", null);
                }
            } catch (Exception e) {
                return new ValidationResult(false, "Error reading image: " + e.getMessage(), null);
            }
        });
    }

    public static CompletableFuture<Optional<SkinData>> fetchSkinFromUrl(String url, String requestedVariant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = SkinWardrobeConfig.getApiKey();
                boolean isFreeTier = apiKey.isEmpty();

                // Built-in free-tier security guard delay
                if (isFreeTier) {
                    int guardSeconds = SkinWardrobeConfig.getFreeTierDelaySeconds();
                    synchronized (LAST_FREE_REQUEST) {
                        Instant now = Instant.now();
                        Instant last = LAST_FREE_REQUEST.get();
                        Duration diff = Duration.between(last, now);
                        long remainingMillis = (guardSeconds * 1000L) - diff.toMillis();
                        if (remainingMillis > 0) {
                            try {
                                Thread.sleep(remainingMillis);
                            } catch (InterruptedException ignored) {
                            }
                        }
                        LAST_FREE_REQUEST.set(Instant.now());
                    }
                }

                // Prepare request to MineSkin v2 Queue
                JsonObject reqObj = new JsonObject();
                reqObj.addProperty("url", url.trim());
                reqObj.addProperty("variant", SkinData.VARIANT_SLIM.equalsIgnoreCase(requestedVariant) ? "slim" : "classic");

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mineskin.org/v2/queue"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "ChilliOil/1.1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(reqObj.toString()));

                if (!apiKey.isEmpty()) {
                    reqBuilder.header("Authorization", "Bearer " + apiKey);
                }

                HttpResponse<String> queueResp = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (queueResp.statusCode() != 200 && queueResp.statusCode() != 202) {
                    LOGGER.warn("MineSkin queue submit returned status {}: {}", queueResp.statusCode(), queueResp.body());
                    return Optional.empty();
                }

                JsonObject queueJson = JsonParser.parseString(queueResp.body()).getAsJsonObject();
                if (!queueJson.has("job")) {
                    return Optional.empty();
                }

                // If skin was already completed or cached immediately
                if (queueJson.has("skin")) {
                    SkinData skin = parseSkinInfo(queueJson.getAsJsonObject("skin"), url, requestedVariant);
                    if (skin != null) {
                        return Optional.of(skin);
                    }
                }

                JsonObject jobObj = queueJson.getAsJsonObject("job");
                String jobId = jobObj.has("id") ? jobObj.get("id").getAsString() : null;
                if (jobId == null || jobId.isBlank()) {
                    return Optional.empty();
                }

                // Poll MineSkin queue job status (up to 30 seconds)
                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }

                    HttpRequest.Builder pollReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mineskin.org/v2/queue/" + jobId))
                        .header("User-Agent", "ChilliOil/1.1.0")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .GET();

                    if (!apiKey.isEmpty()) {
                        pollReq.header("Authorization", "Bearer " + apiKey);
                    }

                    HttpResponse<String> pollResp = HTTP_CLIENT.send(pollReq.build(), HttpResponse.BodyHandlers.ofString());
                    if (pollResp.statusCode() == 200 && pollResp.body() != null) {
                        JsonObject pollJson = JsonParser.parseString(pollResp.body()).getAsJsonObject();
                        if (pollJson.has("skin")) {
                            SkinData skin = parseSkinInfo(pollJson.getAsJsonObject("skin"), url, requestedVariant);
                            if (skin != null) {
                                return Optional.of(skin);
                            }
                        }
                        if (pollJson.has("job")) {
                            JsonObject currentJob = pollJson.getAsJsonObject("job");
                            String status = currentJob.has("status") ? currentJob.get("status").getAsString() : "";
                            if ("failed".equalsIgnoreCase(status)) {
                                LOGGER.warn("MineSkin job {} failed.", jobId);
                                return Optional.empty();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to generate skin from web URL {}", url, e);
            }
            return Optional.empty();
        });
    }

    private static SkinData parseSkinInfo(JsonObject skinObj, String sourceUrl, String fallbackVariant) {
        try {
            String variant = fallbackVariant;
            if (skinObj.has("variant")) {
                variant = skinObj.get("variant").getAsString();
            }
            if (skinObj.has("texture")) {
                JsonObject textureObj = skinObj.getAsJsonObject("texture");
                if (textureObj.has("data")) {
                    JsonObject dataObj = textureObj.getAsJsonObject("data");
                    String value = dataObj.has("value") ? dataObj.get("value").getAsString() : null;
                    String signature = dataObj.has("signature") ? dataObj.get("signature").getAsString() : null;
                    if (value != null && !value.isBlank()) {
                        return new SkinData(variant, sourceUrl, "WEB_URL", value, signature);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing SkinInfo from MineSkin", e);
        }
        return null;
    }
}
