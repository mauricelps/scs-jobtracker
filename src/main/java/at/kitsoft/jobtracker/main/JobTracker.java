package at.kitsoft.jobtracker.main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import at.kitsoft.jobtracker.util.JobTrackerUtils;
import at.kitsoft.jobtracker.util.LoggingRedirector;
import at.kitsoft.jobtracker.util.SFL_Console;
import at.kitsoft.jobtracker.util.SLF_File;

public class JobTracker {

    public static Gson gson = new Gson();
    private TelemetryClient telemetryClient;
    public WebSocketSender wsSender;
    public static Logger logger;

    public static void main(String[] args) {
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Level.ALL);
        configLogger();
        TrackerMetaData meta = JobTrackerUtils.getTrackerMetaData();
        JobTrackerUtils jtu = new JobTrackerUtils();
        if (meta != null) {
            int id = (int) new JobTrackerUtils().sendApiRequest("auth.php", new JobTrackerUtils().createLogonPayload(meta.getAppId(), "L2(-stk13hwuF>i]nSon3WAfYE>bS61d"));
            jtu.setUserId(id);
            meta.setUserId(id);
            if (id == 0) {
                logger.severe("User ID in TrackerMetaData is 0. Please set a valid User ID. Exiting...");
                System.exit(1);
            }
            jtu.setInstallationId(meta.getInstallationId());
            logger.info("Loaded TrackerMetaData: UserID=" + meta.getUserId() + ", InstallationID=" + jtu.getInstallationId());
        }
        JobTracker jobTracker = new JobTracker();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Quitting JobTracker. ByeBye!");
            new JobTrackerUtils().saveStateToFile();
            if (jobTracker.wsSender != null && jobTracker.wsSender.isOpen()) {
                jobTracker.wsSender.close();
                System.out.println("[REMOTE] Closed WebSocketSender connection.");
            }
            if (jobTracker.telemetryClient != null && jobTracker.telemetryClient.isOpen()) {
                jobTracker.telemetryClient.close();
                System.out.println("[TELEMETRY] Closed TelemetryClient connection.");
            }

            closeLogger();
        }));

        try {
            jobTracker.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException {
        System.out.println("JobTracker started.");
        new JobTrackerUtils().loadStateFromFile();
        while (true) {
            Optional<String> runningGame = JobTrackerUtils.getRunningGame();
            if (runningGame.isPresent()) {
                if (telemetryClient == null || !telemetryClient.isOpen()) {
                    System.out.println("Starting TelemetryClient for " + runningGame.get());
                    connectWebSocket();
                }
            } else {
                if (telemetryClient != null && telemetryClient.isOpen()) {
                    System.out.println("Stopping TelemetryClient");
                    telemetryClient.close();
                    telemetryClient = null;
                }
            }
            Thread.sleep(5000);
        }
    }

    private void connectWebSocket() {
        try {
            System.out.println("Connecting to WebSocket servers...");
            System.out.println("Remote WebSocket: " + JobTrackerUtils.websocket_address_remote);
            wsSender = new WebSocketSender(JobTrackerUtils.websocket_address_remote);
            wsSender.connectBlocking(5, TimeUnit.SECONDS);
            System.out.println("Telemetry WebSocket: " + JobTrackerUtils.websocket_address_telemetry);
            telemetryClient = new TelemetryClient(JobTrackerUtils.websocket_address_telemetry);
            telemetryClient.connect();
            System.out.println("WebSocket connections initiated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void configLogger() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        String date = sdf.format(new Date());
        File logsDir = new File("C:\\LTGJobTracker\\logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        try {
            FileHandler fileHandler = new FileHandler("C:\\LTGJobTracker\\logs\\log_" + date + ".log");
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SLF_File());
            logger.addHandler(fileHandler);
            LoggingRedirector.install(logger, new SFL_Console());
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void closeLogger() {
        for (var handler : logger.getHandlers()) {
            handler.close();
        }
    }
}