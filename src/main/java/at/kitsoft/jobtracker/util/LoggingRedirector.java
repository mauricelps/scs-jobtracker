package at.kitsoft.jobtracker.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Installiert Umleitungen für System.out / System.err und sorgt dafür,
 * dass Logger-Nachrichten sowohl in die Datei (FileHandler) als auch
 * formatiert in die Konsole geschrieben werden.
 *
 * Wichtig: Verwende die install(...) Überladung mit einem eigenen Formatter,
 * damit du deinen speziellen Console-Formatter benutzen kannst.
 */
public final class LoggingRedirector {

    private LoggingRedirector() {}

    /**
     * Bequemer Default: benutzt SimpleLogFormatter für die Konsole (falls nichts übergeben).
     */
    public static void install(Logger rootLogger) {
        install(rootLogger, new SLF_File());
    }

    /**
     * Installiert die Umleitung und benutzt den übergebenen consoleFormatter
     * für die Konsolen-Ausgabe (die Datei wird weiterhin vom FileHandler formatiert).
     *
     * Aufrufbeispiel in deiner Main.configLogger():
     *   LoggingRedirector.install(logger, new MyConsoleFormatter());
     *
     * @param rootLogger der Root-Logger, an den bereits FileHandler angehängt wurde
     * @param consoleFormatter dein eigener Formatter für die Konsolenanzeige
     */
    public static void install(Logger rootLogger, Formatter consoleFormatter) {
        // Sichere die Original-Streams (bevor wir System.setOut/Err ändern)
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;

        // Entferne vorhandene ConsoleHandler (verhindert doppelte Ausgabe / Rekursion)
        for (Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                rootLogger.removeHandler(h);
            }
        }

        // Verhindere Weitergabe an Eltern (sonst könnten Eltern-Handler erneut auf die Konsole schreiben)
        rootLogger.setUseParentHandlers(false);

        // Füge einen Handler hinzu, der LOG-Einträge an die gesicherten Original-Streams schreibt.
        // Dieser Handler benutzt den übergebenen consoleFormatter.
        Handler consoleForwardingHandler = new Handler() {
            private final Formatter formatter = consoleFormatter != null ? consoleFormatter : new SLF_File();

            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) return;
                try {
                    String msg = formatter.format(record);
                    // Schreibe an originalErr (Konsole). Schreibe an Err, damit Fehler auffallen.
                    originalErr.print(msg);
                    originalErr.flush();
                } catch (Exception ex) {
                    // Absichern: Handler darf nicht abstürzen
                    try { originalErr.println("LoggingForwardingHandler publish error: " + ex); originalErr.flush(); } catch (Exception ignore) {}
                }
            }

            @Override
            public void flush() {
                try { originalErr.flush(); } catch (Exception ignore) {}
            }

            @Override
            public void close() throws SecurityException {
                // originalErr nicht schließen
            }
        };
        consoleForwardingHandler.setLevel(Level.ALL);
        rootLogger.addHandler(consoleForwardingHandler);

        // Jetzt die Umleitung für System.out / System.err: Tee -> (Original-Konsole) + (Logger)
        OutputStream logOutStream = new LoggingOutputStream(rootLogger, Level.INFO);
        OutputStream logErrStream = new LoggingOutputStream(rootLogger, Level.SEVERE);

        // Tee: schreibt an Original-PrintStream UND an LoggingStream
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, logOutStream), true));
        System.setErr(new PrintStream(new TeeOutputStream(originalErr, logErrStream), true));
    }

    // LoggingOutputStream sammelt bytes bis '\n' und sendet dann die Zeile an den Logger
    private static class LoggingOutputStream extends OutputStream {
        private final Logger logger;
        private final Level level;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        LoggingOutputStream(Logger logger, Level level) {
            this.logger = logger;
            this.level = level;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            if (b == '\r') return; // ignore CR
            if (b == '\n') {
                flush();
            } else {
                buffer.write(b);
            }
        }

        @Override
        public synchronized void flush() {
            if (buffer.size() == 0) return;
            String message = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            buffer.reset();
            if (!message.isEmpty()) {
                logger.log(level, message);
            }
        }

        @Override
        public void close() throws IOException {
            flush();
            try { super.close(); } catch (IOException ignored) {}
        }
    }

    // TeeOutputStream schreibt an zwei OutputStreams
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public synchronized void write(int c) throws IOException {
            try { a.write(c); } catch (IOException ignored) {}
            try { b.write(c); } catch (IOException ignored) {}
        }

        @Override
        public synchronized void write(byte[] bb, int off, int len) throws IOException {
            try { a.write(bb, off, len); } catch (IOException ignored) {}
            try { this.b.write(bb, off, len); } catch (IOException ignored) {}
        }

        @Override
        public synchronized void flush() throws IOException {
            try { a.flush(); } catch (IOException ignored) {}
            try { b.flush(); } catch (IOException ignored) {}
        }

        @Override
        public synchronized void close() throws IOException {
            try { a.close(); } catch (IOException ignored) {}
            try { b.close(); } catch (IOException ignored) {}
        }
    }
}