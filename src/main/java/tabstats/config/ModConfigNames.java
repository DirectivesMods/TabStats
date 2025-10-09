package tabstats.config;

public enum ModConfigNames {
    APIKEY("ApiKey"),
    RENDER_HEADER_FOOTER("RenderHeaderFooter"),
    MOD_ENABLED("ModEnabled");

    private final String name;

    ModConfigNames(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
