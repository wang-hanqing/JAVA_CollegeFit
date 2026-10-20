import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * All text shown to students lives in messages_en.properties.
 * To add a language later, create messages_es.properties and call setLocale().
 */
public final class Messages {
    private static ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);

    private Messages() { }

    public static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    /** Looks up a text by key and fills %s placeholders with the given values. */
    public static String get(String key, Object... args) {
        String text;
        try {
            text = bundle.getString(key);
        } catch (MissingResourceException e) {
            return "[" + key + "]"; // makes missing text easy to spot during testing
        }
        return args.length == 0 ? text : String.format(Locale.US, text, args);
    }

    /** Formats dollars. Negative historical averages are shown as reported, e.g. "-$1,264". */
    public static String money(Integer dollars) {
        if (dollars == null) return get("money.notReported");
        if (dollars < 0) return String.format(Locale.US, "-$%,d", -dollars);
        return String.format(Locale.US, "$%,d", dollars);
    }

    public static String number(Integer n) {
        return n == null ? get("money.notReported") : String.format(Locale.US, "%,d", n);
    }
}
