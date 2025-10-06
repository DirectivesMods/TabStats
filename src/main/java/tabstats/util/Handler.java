package tabstats.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Handler {
    public static final Locale LOCALE = getLocale();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(16,
        new ThreadFactoryBuilder().setNameFormat("TabStats-%d").setDaemon(true).build()
    );
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .setDateFormat(getDate().toPattern())
            .create();

    private static Locale getLocale() {
        String language = System.getProperty("user.language");
        if (language == null || language.trim().isEmpty()) {
            return Locale.getDefault();
        }

        String country = System.getProperty("user.country");
        if (country == null || country.trim().isEmpty()) {
            return new Locale(language);
        }

        return new Locale(language, country);
    }

    public static void asExecutor(Runnable runnable) {
        executorService.submit(runnable);
    }

    public static Gson getGson() {
        return GSON;
    }

    public static SimpleDateFormat getDate() {
        return new SimpleDateFormat("EEEEE dd MMMMM yyyy", LOCALE);
    }

    public static String plsSplit(double value) {
        return new DecimalFormat("##.##").format(value);
    }
}
