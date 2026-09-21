package lab.designpatterns.simplification;

import java.util.List;
import java.util.Objects;

/**
 * Idiomatic, simple CSV exporter avoiding pattern over-engineering (YAGNI & KISS). Eliminates
 * artificial Factory, Bridge, and Visitor layers for a straightforward text transformation.
 */
public final class UserCsvExporter {

    private UserCsvExporter() {}

    public static String toCsv(List<UserRecord> users) {
        Objects.requireNonNull(users, "Users list must not be null");
        StringBuilder sb = new StringBuilder("id,username,email\n");
        for (UserRecord user : users) {
            sb.append(user.id())
                    .append(',')
                    .append(user.username())
                    .append(',')
                    .append(user.email())
                    .append('\n');
        }
        return sb.toString();
    }
}
