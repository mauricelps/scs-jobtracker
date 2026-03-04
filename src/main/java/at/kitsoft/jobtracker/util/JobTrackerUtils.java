package at.kitsoft.jobtracker.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.simpleyaml.configuration.file.YamlFile;

import com.google.gson.JsonObject;

import at.kitsoft.jobtracker.main.JobState;
import at.kitsoft.jobtracker.main.JobTracker;
import at.kitsoft.jobtracker.main.TelemetryClient;
import at.kitsoft.jobtracker.main.TrackerMetaData;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class JobTrackerUtils {

    private static final String ets2_process_name = "eurotrucks2";
    private static final String ats_process_name = "amtrucks";
    public static final String websocket_address_telemetry = "ws://localhost:9995";
    public static final String websocket_address_remote = "ws://88.198.12.152:9996";
    public static final String api_base_url = "https://gtracker.kitsoft.at/api/";
    public static final String state_file = "job_progress.dat";
    private Long currentJobId = null;
    private double jobStartOdometer = 0.0;
    private double drivenDistance = 0.0;
    private double drivenDistanceSession = 0.0;
    private long jobFinishedTimestamp = 0;
    private double maxSpeed = 0.0;
    private double usedDiesel = 0.0;
    private double usedAdblue = 0.0;
    private JsonObject currentTelemetryState = new JsonObject();
    public static int userId = 0;
    public static UUID installationId = null;

    public Long getCurrentJobId() {
        return currentJobId;
    }

    public void setCurrentJobId(Long currentJobId) {
        this.currentJobId = currentJobId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        JobTrackerUtils.userId = userId;
    }

    public UUID getInstallationId() {
        return installationId;
    }

    public void setInstallationId(UUID installationId) {
        JobTrackerUtils.installationId = installationId;
    }

    public double getJobStartOdometer() {
        return jobStartOdometer;
    }

    public void setJobStartOdometer(double jobStartOdometer) {
        this.jobStartOdometer = jobStartOdometer;
    }

    public void setJobFinishedTimestamp(long jobFinishedTimestamp) {
        this.jobFinishedTimestamp = jobFinishedTimestamp;
    }

    public int boolToInt(boolean value) {
        return value ? 1 : 0;
    }

    public double getDrivenDistance() {
        return drivenDistance;
    }

    public void setDrivenDistance(double drivenDistance) {
        this.drivenDistance = drivenDistance;
    }

    public double getDrivenDistanceSession() {
        return drivenDistanceSession;
    }

    public void setDrivenDistanceSession(double drivenDistanceSession) {
        this.drivenDistanceSession = drivenDistanceSession;
    }

    public String getStringValue(JsonObject state, String key) {
        synchronized (state) {
            if (state.has(key) && !state.get(key).isJsonNull()) {
                return state.get(key).getAsString();
            }else {
                return "none";
            }
        }
    }

    public String getCargoIdFromCurrentState() {
        return getStringValue(currentTelemetryState, "job.cargo.id");
    }

    public long getJobFinishedTimestamp() {
        return jobFinishedTimestamp;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double speed) {
        this.maxSpeed = speed;
    }

    public double getUsedDiesel() {
        return usedDiesel;
    }

    public void setUsedDiesel(double usedDiesel) {
        this.usedDiesel = usedDiesel;
    }

    public double getUsedAdblue() {
        return usedAdblue;
    }

    public void setUsedAdblue(double usedAdblue) {
        this.usedAdblue = usedAdblue;
    }

    public double getDoubleValue(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsDouble() : 0.0;
    }

    public String createTransportsJobPayload(String transportType, int amount, String source, String target,
            long jobId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("game", getStringValue(currentTelemetryState, "game"));
        payload.addProperty("transportType", transportType);
        payload.addProperty("amount", amount);
        payload.addProperty("source", source);
        payload.addProperty("destination", target);
        payload.addProperty("jobId", jobId);
        // payload.addProperty("steamid", getSteamId());
        payload.addProperty("userid", userId); // DEBUG - user ID

        return JobTracker.gson.toJson(payload);
    }

    public String createLogonPayload(String appId, String logonToken) {
        com.google.gson.JsonObject payload = new JsonObject();
        payload.addProperty("appId", appId);
        payload.addProperty("userToken", logonToken);
        return JobTracker.gson.toJson(payload);
    }

    public String createTollJobPayload(int amount, long jobId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("game", getStringValue(currentTelemetryState, "game"));
        payload.addProperty("amount", amount);
        payload.addProperty("jobId", jobId);
        // payload.addProperty("steamid", getSteamId());
        payload.addProperty("userid", userId); // DEBUG - user ID

        return JobTracker.gson.toJson(payload);
    }

    public String createFineJobPayload(int amount, String offence, long jobId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("game", getStringValue(currentTelemetryState, "game"));
        payload.addProperty("amount", amount);
        payload.addProperty("offence", offence);
        payload.addProperty("jobId", jobId);
        // payload.addProperty("steamid", getSteamId());
        payload.addProperty("userid", userId); // DEBUG - user ID

        return JobTracker.gson.toJson(payload);
    }

    public String createStartJobPayload() {
        // Sicherer Zugriff auf JSON-Properties
        String truck = getStringValue(currentTelemetryState, "truck.brand") + " "
                + getStringValue(currentTelemetryState, "truck.name");
        String cargo = getStringValue(currentTelemetryState, "job.cargo");
        String sourceCity = getStringValue(currentTelemetryState, "job.source.city");
        String sourceCompany = getStringValue(currentTelemetryState, "job.source.company");
        String destCity = getStringValue(currentTelemetryState, "job.destination.city");
        String destCompany = getStringValue(currentTelemetryState, "job.destination.company");
        double plannedKm = getDoubleValue(currentTelemetryState, "job.planned_distance.km");
        String game = getStringValue(currentTelemetryState, "game");
        String truckLicensePlate = getStringValue(currentTelemetryState, "truck.license.plate");
        String truckLPlateCountry = getStringValue(currentTelemetryState, "truck.license.plate.country");
        String trailerLicensePlate = getStringValue(currentTelemetryState, "trailer.license.plate");
        String trailerLPlateCountry = getStringValue(currentTelemetryState, "trailer.license.plate.country");
        String truckLPlateCountryId = getStringValue(currentTelemetryState, "truck.license.plate.country.id");
        String trailerLPlateCountryId = getStringValue(currentTelemetryState, "trailer.license.plate.country.id");
        String trlBdyType = getStringValue(currentTelemetryState, "trailer.0.brand") + " "
                + getStringValue(currentTelemetryState, "trailer.0.name");

        JsonObject payload = new JsonObject();
        payload.addProperty("game", game);
        // payload.addProperty("driverSteamId", getSteamId());
        payload.addProperty("userid", userId); // DEBUG - user ID
        payload.addProperty("truck", truck);
        payload.addProperty("cargo", cargo);
        payload.addProperty("sourceCity", sourceCity);
        payload.addProperty("sourceCompany", sourceCompany);
        payload.addProperty("destinationCity", destCity);
        payload.addProperty("destinationCompany", destCompany);
        payload.addProperty("plannedDistanceKm", plannedKm);
        payload.addProperty("truckLicensePlate", truckLicensePlate);
        payload.addProperty("truckLicensePlateCountry", truckLPlateCountry);
        payload.addProperty("trailerLicensePlate", trailerLicensePlate);
        payload.addProperty("trailerLicensePlateCountry", trailerLPlateCountry);
        payload.addProperty("trailerBodyType", trlBdyType);
        payload.addProperty("truckLicensePlateCountryId", truckLPlateCountryId);
        payload.addProperty("trailerLicensePlateCountryId", trailerLPlateCountryId);

        return JobTracker.gson.toJson(payload);
    }

    public String createFinishJobPayload(String status, int income, int xp, boolean autoLoad, boolean autoPark) {

        double currentOdometer = getDoubleValue(currentTelemetryState, "truck.odometer");
        double drivenKm = currentOdometer - jobStartOdometer;
        String market = getStringValue(currentTelemetryState, "job.job.market");

        // WEAR
        double truckChassis = getDoubleValue(currentTelemetryState, "truck.wear.chassis");
        double truckEngine = getDoubleValue(currentTelemetryState, "truck.wear.engine");
        double truckTransmission = getDoubleValue(currentTelemetryState, "truck.wear.transmission");
        double truckWheels = getDoubleValue(currentTelemetryState, "truck.wear.wheels");
        double truckCabin = getDoubleValue(currentTelemetryState, "truck.wear.cabin");

        double trailerChassis = getDoubleValue(currentTelemetryState, "trailer.wear.chassis");
        double trailerWheels = getDoubleValue(currentTelemetryState, "trailer.wear.wheels");
        double trailerBody = getDoubleValue(currentTelemetryState, "trailer.wear.body");

        double cargoDamage = getDoubleValue(currentTelemetryState, "trailer.cargo.damage");
        double cargoMass = getDoubleValue(currentTelemetryState, "job.cargo.mass");

        JsonObject payload = new JsonObject();
        payload.addProperty("status", status); // usually DELIVERED or CANCELLED
        payload.addProperty("driven_km", drivenKm); // calculated diff from odometer
        // payload.addProperty("steamid", getSteamId()); //Steam ID from current driver
        payload.addProperty("userid", userId); // DEBUG - user ID
        payload.addProperty("income", income); // income from delivered job
        payload.addProperty("market", market); // quick job, freight market or loading market (need to test with WoTr)
        payload.addProperty("jobid", currentJobId);
        payload.addProperty("xp", xp); // earned XP from delivered job

        payload.addProperty("wearTruckCabin", truckCabin);
        payload.addProperty("wearTruckChassis", truckChassis);
        payload.addProperty("wearTruckEngine", truckEngine);
        payload.addProperty("wearTruckTransmission", truckTransmission);
        payload.addProperty("wearTruckWheels", truckWheels);
        payload.addProperty("wearTrailerChassis", trailerChassis);
        payload.addProperty("wearTrailerWheels", trailerWheels);
        payload.addProperty("wearTrailerBody", trailerBody);
        payload.addProperty("cargoDamage", cargoDamage);
        payload.addProperty("cargoMass", Math.round(cargoMass));
        payload.addProperty("maxspeed", Math.round(this.maxSpeed));
        payload.addProperty("autoLoad", boolToInt(autoLoad));
        payload.addProperty("autoPark", boolToInt(autoPark));
        payload.addProperty("usedDiesel", Math.round(this.usedDiesel * 100.0) / 100.0);
        payload.addProperty("usedAdblue", Math.round(this.usedAdblue * 100.0) / 100.0);
        return JobTracker.gson.toJson(payload);
    }

    public long sendApiRequest(String endpoint, String jsonPayload) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(api_base_url + endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && endpoint.contains("start_job")) {
                JsonObject responseJson = JobTracker.gson.fromJson(response.body(), JsonObject.class);
                return responseJson.get("jobId").getAsLong();
            } else if(response.statusCode() == 200 && endpoint.contains("auth.php")) {
                JsonObject responseJson = JobTracker.gson.fromJson(response.body(), JsonObject.class);
                int userId = responseJson.get("userId").getAsInt();
                System.out.println("Authentication successful. User ID: " + userId);
                return userId;
            } else if (response.statusCode() == 200) {
                return 0; // Erfolg für andere Anfragen
            } else {
                System.err.println("API-Fehler: " + response.statusCode() + " - " + response.body());
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Senden der API-Anfrage: " + e.getMessage());
            return -1;
        }
    }

    // Prüft, ob ETS2 oder ATS läuft
    public static Optional<String> getRunningGame() {
        OperatingSystem os = new SystemInfo().getOperatingSystem();
        return os.getProcesses().stream()
                .filter(p -> p.getName().equalsIgnoreCase(ets2_process_name)
                        || p.getName().equalsIgnoreCase(ats_process_name))
                .map(OSProcess::getName)
                .findFirst();
    }

    public synchronized void updateCurrentTelemetryState(JsonObject newState) {
        for (String key : newState.keySet()) {
            currentTelemetryState.add(key, newState.get(key));
        }
    }

    public void startNewJob() {
        String startPayload = createStartJobPayload();
        long newJobId = sendApiRequest("start_job.php", startPayload);
        if (newJobId != -1) {
            this.currentJobId = newJobId;
            System.out.println("Neuer Job gestartet mit ID: " + newJobId);
            this.jobStartOdometer = getDoubleValue(currentTelemetryState, "truck.odometer");

        }
    }

    public void clearJobData() {
        System.out.println("Clearing job data...");
        List<String> keysToRemove = new ArrayList<>();

        synchronized (currentTelemetryState) {
            for (String key : currentTelemetryState.keySet()) {
                if (key.startsWith("job.") || key.startsWith("cargo.")) {
                    keysToRemove.add(key);
                }
            }
            for (String key : keysToRemove) {
                currentTelemetryState.remove(key);
            }
        }
    }

    public void saveStateToFile() {
        if (this.currentJobId != null) {
            new File(state_file).delete(); // Alte Datei löschen
            System.out.println("No active job, removing old state file...");
            return;
        }

        System.out.println("Saving current Job-State");
        JobState state = new JobState();
        state.setJobId(currentJobId);
        state.setStartOdometer(jobStartOdometer);
        state.setMaxSpeed(maxSpeed);
        state.setUsedDiesel(usedDiesel);
        state.setUsedAdblue(usedAdblue);

        String jsonState = JobTracker.gson.toJson(state);
        String encryptedState = CryptoUtils.encrypt(jsonState);

        if (encryptedState != null) {
            try (FileWriter fw = new FileWriter(state_file)) {
                fw.write(encryptedState);
                System.out.println("Job-State saved successfully.");
            } catch (IOException e) {
                System.err.println("Error saving Job-State: " + e.getMessage());
            }
        }
    }

    public void loadStateFromFile() {
        File stateFile = new File(state_file);
        if (!stateFile.exists()) {
            System.out.println("Keine Speicherdatei gefunden. Starte mit einem sauberen Zustand.");
            return;
        }

        System.out.println("Gespeicherter Job-Zustand gefunden. Versuche zu laden...");
        try {
            String encryptedState = new String(Files.readAllBytes(Paths.get(state_file)));
            String jsonState = CryptoUtils.decrypt(encryptedState);

            if (jsonState != null && !jsonState.isEmpty()) {
                JobState loadedState = JobTracker.gson.fromJson(jsonState, JobState.class);

                // Stelle den Zustand des Trackers wieder her
                if (loadedState.getJobId() == null) {
                    System.out.println(
                            "Gespeicherter Job-Zustand enthält keine aktive Job-ID. Starte mit einem sauberen Zustand.");
                    return;
                }
                this.currentJobId = loadedState.getJobId();
                this.jobStartOdometer = loadedState.getStartOdometer();
                this.maxSpeed = loadedState.getMaxSpeed();
                this.usedDiesel = loadedState.getUsedDiesel();
                this.usedAdblue = loadedState.getUsedAdblue();
                // Lade hier weitere Felder, die du gespeichert hast.

                System.out.println("Job-Zustand erfolgreich geladen. Job-ID: " + this.currentJobId);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Speicherdatei: " + e.getMessage());
        }
    }

    public static TrackerMetaData getTrackerMetaData() {
        File file = new File("C:\\LTGJobTracker\\metadata.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            YamlFile cfg = YamlFile.loadConfiguration(file);
            TrackerMetaData meta = new TrackerMetaData();
            UUID uuid = UUID.randomUUID();
            cfg.set("version", meta.getVersion());
            cfg.addDefault("userId", 0); // MUST BE CHANGED BY USER
            cfg.addDefault("installationId", uuid.toString());
            cfg.addDefault("secured", false);
            cfg.addDefault("secureText", "none");
            cfg.options().copyDefaults(true);

            cfg.save(file);
            cfg.load();
            if (cfg.getBoolean("secured") && cfg.getString("secureText").equals("none")) {
                int uid = cfg.getInt("userId");
                String iid = cfg.getString("installationId");
                String bufText = "uid=" + uid + ";iid=" + iid;
                String encryptedText = CryptoUtils.encrypt(bufText);
                cfg.set("secureText", encryptedText);
            }
            cfg.save(file);

            if (cfg.getBoolean("secured") && !cfg.getString("secureText").equals("none")) {
                String secureText = cfg.getString("secureText");
                String decryptedText = CryptoUtils.decrypt(secureText);
                String[] parts = decryptedText.split(";");
                int uid = Integer.parseInt(parts[0].split("=")[1]);
                String iid = parts[1].split("=")[1];
                meta.setUserId(uid);
                meta.setInstallationId(UUID.fromString(iid));
            }
            return meta;
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void handleGamePlayEvent(JsonObject event) {
        String eventName = event.get("event_name").getAsString();
        System.out.println("Received Gameplay Event: " + eventName);
        JobTrackerUtils jtu = new JobTrackerUtils();
        switch (eventName) {
            case "job.delivered":
                if (jtu.getCurrentJobId() != null) {
                    int xp = event.get("attributes").getAsJsonObject().get("earned.xp").getAsInt();
                    int revenue = event.get("attributes").getAsJsonObject().get("revenue").getAsInt();
                    boolean autoLoad = event.get("attributes").getAsJsonObject().get("auto.load.used").getAsBoolean();
                    boolean autoPark = event.get("attributes").getAsJsonObject().get("auto.park.used").getAsBoolean();
                    String finishPayload = jtu.createFinishJobPayload("Delivered", revenue, xp, autoLoad, autoPark);
                    jtu.sendApiRequest("finish_job.php?id=" + jtu.getCurrentJobId(), finishPayload);
                    System.out.println("Job " + jtu.getCurrentJobId() + " erfolgreich beendet.");

                    jtu.setCurrentJobId(null);
                    jtu.setJobStartOdometer(0.0);
                    jtu.setJobFinishedTimestamp(System.currentTimeMillis());
                    jtu.setMaxSpeed(0.0);
                    jtu.setUsedDiesel(0.0);
                    jtu.setUsedAdblue(0.0);
                    jtu.setDrivenDistance(0.0);
                    TelemetryClient.resetSnapshots();
                    jtu.clearJobData();
                }
                break;

            case "job.cancelled":
                if (jtu.getCurrentJobId() != null) {
                    String cancelPayload = jtu.createFinishJobPayload("Cancelled", 0, 0, false, false);
                    jtu.sendApiRequest("finish_job.php?id=" + jtu.getCurrentJobId(), cancelPayload);
                    System.out.println("Job " + jtu.getCurrentJobId() + " wurde abgebrochen.");
                    jtu.setCurrentJobId(null);
                    jtu.setJobStartOdometer(0.0);
                    jtu.setJobFinishedTimestamp(System.currentTimeMillis());
                    jtu.setMaxSpeed(0.0);
                    jtu.setUsedDiesel(0.0);
                    jtu.setUsedAdblue(0.0);
                    jtu.setDrivenDistance(0.0);
                    TelemetryClient.resetSnapshots();
                    jtu.clearJobData();
                }
                break;
            case "player.use.ferry":
                if (jtu.getCurrentJobId() != null) {
                    int amount = event.get("attributes").getAsJsonObject().get("pay.amount").getAsInt();
                    String source = event.get("attributes").getAsJsonObject().get("source.name").getAsString();
                    String target = event.get("attributes").getAsJsonObject().get("target.name").getAsString();
                    String ferryPayload = jtu.createTransportsJobPayload("ferry", amount, source, target,
                            jtu.getCurrentJobId());
                    jtu.sendApiRequest("record_transport.php", ferryPayload);
                }
                break;
            case "player.use.train":
                if (jtu.getCurrentJobId() != null) {
                    int amount = event.get("attributes").getAsJsonObject().get("pay.amount").getAsInt();
                    String source = event.get("attributes").getAsJsonObject().get("source.name").getAsString();
                    String target = event.get("attributes").getAsJsonObject().get("target.name").getAsString();
                    String trainPayload = jtu.createTransportsJobPayload("train", amount, source, target,
                            jtu.getCurrentJobId());
                    jtu.sendApiRequest("record_transport.php", trainPayload);
                }
                break;

            case "player.tollgate.paid":
                if (jtu.getCurrentJobId() != null) {
                    int amount = event.get("attributes").getAsJsonObject().get("pay.amount").getAsInt();
                    String tollPayload = jtu.createTollJobPayload(amount, jtu.getCurrentJobId());
                    jtu.sendApiRequest("record_toll.php", tollPayload);
                }
                break;

            case "player.fined":
                if (jtu.getCurrentJobId() != null) {
                    int amount = event.get("attributes").getAsJsonObject().get("fine.amount").getAsInt();
                    String offence = event.get("attributes").getAsJsonObject().get("fine.offence").getAsString();
                    String finePayload = jtu.createFineJobPayload(amount, offence, jtu.getCurrentJobId());
                    jtu.sendApiRequest("record_fine.php", finePayload);
                }
                break;
        }
    }
}