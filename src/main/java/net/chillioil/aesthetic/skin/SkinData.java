package net.chillioil.aesthetic.skin;

public record SkinData(
    String variant, // "classic" or "slim"
    String source,  // username or URL or "default"
    String sourceType, // "MOJANG_USER", "WEB_URL", or "DEFAULT"
    String value,   // Base64 textures value
    String signature // Mojang signature
) {
    public static final String VARIANT_CLASSIC = "classic";
    public static final String VARIANT_SLIM = "slim";

    public boolean isSlim() {
        return VARIANT_SLIM.equalsIgnoreCase(variant);
    }
}
