package at.kitsoft.jobtracker.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SFL_Console extends Formatter {

    private final Date dat = new Date();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    @Override
    public synchronized String format(LogRecord record) {
        dat.setTime(record.getMillis());
        String timestamp = sdf.format(dat);

        String source = record.getSourceClassName();
        if (source == null) {
            source = record.getLoggerName();
        }
        StringBuilder sb = new StringBuilder(200);

        sb.append('[').append(timestamp).append("] ");
        sb.append(source);
        sb.append(" -> ");

        // formatMessage behandelt Parameter-Substitution (Message mit {0}, ...)
        sb.append(formatMessage(record)).append(System.lineSeparator());

        // Falls eine Exception angehängt ist, die Stacktrace anfügen
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.flush();
            sb.append(sw.toString());
        }

        return sb.toString();
    }

}
