package at.kitsoft.jobtracker.main;

import java.net.URI;
import java.text.DecimalFormat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import at.kitsoft.jobtracker.util.JobTrackerUtils;

public class TelemetryClient extends WebSocketClient {

    private static double usedDieselSnapshot = 0.0, usedAdblueSnapshot = 0.0, drivenDistanceSnapshot = 0.0, drivenDistanceSessionSnapshot = 0.0;
    private static final String live_token = "Rxw3V57IXIVC35ODOP2yIULzrGJoVD6VmfBwBjj5qZ2YITbgAJPGAYb3OgJhBkoYaoUzRQPI3j4W5fLpF9hQliB4eQdeRIlTDA1FZrhupQguWWotK2wMkdC5T7cGZK12";
    private static int ctr = 0;
    public JobTrackerUtils jtu;

    {
        jtu = new JobTrackerUtils();
    }

    public TelemetryClient(String serverUri) {
        super(URI.create(serverUri));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[TELEMETRY] WS-Connection opened.");
    }

    @Override
    public void onMessage(String message) {
        DecimalFormat df = new DecimalFormat("#.##");
        try {
            JsonObject newTelemetryState = JobTracker.gson.fromJson(message, JsonObject.class);
            jtu.updateCurrentTelemetryState(newTelemetryState);
            if (newTelemetryState.has("type") && "gameplay".equals(newTelemetryState.get("type").getAsString())) {
                jtu.handleGamePlayEvent(newTelemetryState);
            }

            double currentDrivenDistanceSession = jtu.getDrivenDistanceSession();
            double newDrivenDistanceSession = jtu.getDoubleValue(newTelemetryState, "truck.odometer");
            if (drivenDistanceSessionSnapshot == 0.0) {
                drivenDistanceSessionSnapshot = newDrivenDistanceSession;
            } else {
                if (newDrivenDistanceSession > drivenDistanceSessionSnapshot) {
                    double drivenSinceLast = newDrivenDistanceSession - drivenDistanceSessionSnapshot;
                    if (drivenSinceLast >= 15.0) {
                        // Vermutung: Messfehler, ignoriere
                        System.out.println("Messfehler bei gefahrene Distanz erkannt, ignoriere Änderung von "
                                + df.format(drivenSinceLast) + " km");
                        return;
                    }
                    currentDrivenDistanceSession += drivenSinceLast;
                    jtu.setDrivenDistanceSession(currentDrivenDistanceSession);
                    drivenDistanceSessionSnapshot = newDrivenDistanceSession;
                    System.out.println(
                            "Updated driven distance for current job: " + df.format(currentDrivenDistanceSession) + " km");
                }
            }

            if (jtu.getCurrentJobId() == null) {
                String cargoId = jtu.getCargoIdFromCurrentState();

                boolean connectedTrailer = newTelemetryState.has("trailer.connected")
                        ? newTelemetryState.get("trailer.connected").getAsBoolean()
                        : false;
                boolean loadedCargo = newTelemetryState.has("job.cargo.loaded")
                        ? newTelemetryState.get("job.cargo.loaded").getAsBoolean()
                        : false;
                if (cargoId != null && !cargoId.isEmpty() && !cargoId.equals("null") && connectedTrailer
                        && loadedCargo) {
                    jtu.startNewJob();
                }
            } else {
                double speed = jtu.getMaxSpeed();
                double currentSpeed = jtu.getDoubleValue(newTelemetryState, "truck.speed");
                if (currentSpeed > speed) {
                    jtu.setMaxSpeed(currentSpeed);
                }

                double currentDrivenDistance = jtu.getDrivenDistance();
                double newDrivenDistance = jtu.getDoubleValue(newTelemetryState, "truck.odometer");
                if (drivenDistanceSnapshot == 0.0) {
                    drivenDistanceSnapshot = newDrivenDistance;
                } else {
                    if (newDrivenDistance > drivenDistanceSnapshot) {
                        double drivenSinceLast = newDrivenDistance - drivenDistanceSnapshot;
                        if (drivenSinceLast >= 15.0) {
                            // Vermutung: Messfehler, ignoriere
                            System.out.println("Messfehler bei gefahrene Distanz erkannt, ignoriere Änderung von "
                                    + df.format(drivenSinceLast) + " km");
                            return;
                        }
                        currentDrivenDistance += drivenSinceLast;
                        jtu.setDrivenDistance(currentDrivenDistance);
                        drivenDistanceSnapshot = newDrivenDistance;
                        System.out.println(
                                "Updated driven distance for current job: " + df.format(currentDrivenDistance) + " km");
                    }
                }

                double currentUsedDiesel = jtu.getUsedDiesel();
                double dieselAmount = jtu.getDoubleValue(newTelemetryState, "truck.fuel.amount");
                if (usedDieselSnapshot == 0.0) {
                    usedDieselSnapshot = dieselAmount;
                } else {
                    if (dieselAmount < usedDieselSnapshot) {
                        double usedSinceLast = usedDieselSnapshot - dieselAmount;
                        if (usedSinceLast >= 7.5) {
                            // Vermutung: Messfehler, ignoriere
                            System.out.println("Messfehler bei Dieselverbrauch erkannt, ignoriere Änderung von "
                                    + df.format(usedSinceLast) + " liters");
                            return;
                        }
                        currentUsedDiesel += usedSinceLast;
                        jtu.setUsedDiesel(currentUsedDiesel);
                        usedDieselSnapshot = dieselAmount;
                        System.out.println(
                                "Updated used diesel for current job: " + df.format(currentUsedDiesel) + " liters");
                    } else if (dieselAmount > usedDieselSnapshot) {
                        // Tankvorgang erkannt
                        usedDieselSnapshot = dieselAmount;
                        System.out.println("[DIESEL] Tankvorgang erkannt, aktualisiere Snapshot auf: "
                                + df.format(usedDieselSnapshot) + " liters");
                    }
                }

                double currentUsedAdblue = jtu.getUsedAdblue();
                double adblueAmount = jtu.getDoubleValue(newTelemetryState, "truck.adblue");
                if (usedAdblueSnapshot == 0.0) {
                    usedAdblueSnapshot = adblueAmount;
                } else {
                    if (adblueAmount < usedAdblueSnapshot) {
                        double usedSinceLast = usedAdblueSnapshot - adblueAmount;
                        if (usedSinceLast >= 1.0) {
                            // Vermutung: Messfehler, ignoriere
                            System.out.println("Messfehler bei Adblueverbrauch erkannt, ignoriere Änderung von "
                                    + df.format(usedSinceLast) + " liters");
                            return;
                        }
                        currentUsedAdblue += usedSinceLast;
                        jtu.setUsedAdblue(currentUsedAdblue);
                        usedAdblueSnapshot = adblueAmount;
                        System.out.println(
                                "Updated used adblue for current job: " + df.format(currentUsedAdblue) + " liters");
                    } else if (adblueAmount > usedAdblueSnapshot) {
                        // Tankvorgang erkannt
                        usedAdblueSnapshot = adblueAmount;
                        System.out.println("[ADBLUE] Tankvorgang erkannt, aktualisiere Snapshot auf: "
                                + df.format(usedAdblueSnapshot) + " liters");
                    }
                }
            }

            {
                // HANDLE LIVE DATA
                int userId = jtu.getUserId();
                String cargoName = "";
                String sourceCity = "";
                String sourceCompany = "";
                String destinationCity = "";
                String destinationCompany = "";
                int plannedDistance = 0;
                int remainingDistance = 0;
                int drivenDistance = drivenDistanceSnapshot == 0.0 ? 0 : (int) Math.round(drivenDistanceSnapshot);
                int drivenDistanceSession = drivenDistanceSessionSnapshot == 0.0 ? 0 : (int) Math.round(drivenDistanceSessionSnapshot);
                int dieselValue = 0;
                int adblueValue = 0;
                String truck;
                int speed = 0;
                int speedLimit = 0;
                String game = "";
                String liveToken = live_token;

                if (newTelemetryState.has("cargo")) {
                    cargoName = newTelemetryState.get("cargo").getAsString();
                }

                if (newTelemetryState.has("job.source.city")) {
                    sourceCity = newTelemetryState.get("job.source.city").getAsString();
                }

                if (newTelemetryState.has("job.source.company")) {
                    sourceCompany = newTelemetryState.get("job.source.company").getAsString();
                }

                if (newTelemetryState.has("job.destination.city")) {
                    destinationCity = newTelemetryState.get("job.destination.city").getAsString();
                }

                if (newTelemetryState.has("job.destination.company")) {
                    destinationCompany = newTelemetryState.get("job.destination.company").getAsString();
                }

                if (newTelemetryState.has("job.planned_distance.km")) {
                    plannedDistance = (int) Math.round(newTelemetryState.get("job.planned_distance.km").getAsDouble());
                }

                if (newTelemetryState.has("truck.navigation.distance")) {
                    remainingDistance = (int) (Math
                            .round(newTelemetryState.get("truck.navigation.distance").getAsDouble()) / 1000);
                }

                if (newTelemetryState.has("truck.adblue") && newTelemetryState.has("truck.adblue.capacity")) {
                    double adblueAmount = newTelemetryState.get("truck.adblue").getAsDouble();
                    double adblueCapacity = newTelemetryState.get("truck.adblue.capacity").getAsDouble();
                    adblueValue = (int) Math.round((adblueAmount / adblueCapacity) * 100);
                }

                if (newTelemetryState.has("truck.fuel.amount") && newTelemetryState.has("truck.fuel.capacity")) {
                    double fuelAmount = newTelemetryState.get("truck.fuel.amount").getAsDouble();
                    double fuelCapacity = newTelemetryState.get("truck.fuel.capacity").getAsDouble();
                    dieselValue = (int) Math.round((fuelAmount / fuelCapacity) * 100);
                }

                if (newTelemetryState.has("truck.brand") && newTelemetryState.has("truck.name")) {
                    truck = newTelemetryState.get("truck.brand").getAsString() + " "
                            + newTelemetryState.get("truck.name").getAsString();
                } else {
                    truck = "Generic Truck";
                }

                if (newTelemetryState.has("truck.navigation.speed.limit")) {
                    speedLimit = (int) Math
                            .round(newTelemetryState.get("truck.navigation.speed.limit").getAsDouble() * 3.6);
                }

                if (newTelemetryState.has("truck.speed")) {
                    speed = (int) Math.round(newTelemetryState.get("truck.speed").getAsDouble());
                }

                if (newTelemetryState.has("game")) {
                    game = newTelemetryState.get("game").getAsString();
                }

                ctr++;
                if (ctr >= 5) {
                    ctr = 0;
                    sendLiveData(userId, cargoName, sourceCity, sourceCompany, destinationCity, destinationCompany,
                            plannedDistance, remainingDistance, drivenDistance, drivenDistanceSession, dieselValue,
                            adblueValue, truck, speed, speedLimit, game, liveToken);
                }

            }

        } catch (JsonSyntaxException e) {
            System.err.println("[TELEMETRY] Error whilst processing WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[TELEMETRY] WebSocket connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[TELEMETRY] WebSocket error: " + ex.getMessage());
    }

    public static void resetSnapshots() {
        usedDieselSnapshot = 0.0;
        usedAdblueSnapshot = 0.0;
        drivenDistanceSnapshot = 0.0;
    }

    private void sendLiveData(int userId, String cargoName, String sourceCity, String sourceCompany,
            String destinationCity, String destinationCompany,
            int plannedDistance, int remainingDistance, int drivenDistance, int drivenDistanceSession, int dieselValue,
            int adblueValue, String truck, int speed, int speedLimit, String game, String liveToken) {
        JsonObject liveData = new JsonObject();
        liveData.addProperty("livedata", true);
        liveData.addProperty("userId", userId);
        liveData.addProperty("cargoName", cargoName);
        liveData.addProperty("sourceCity", sourceCity);
        liveData.addProperty("sourceCompany", sourceCompany);
        liveData.addProperty("destinationCity", destinationCity);
        liveData.addProperty("destinationCompany", destinationCompany);
        liveData.addProperty("plannedDistance", plannedDistance);
        liveData.addProperty("remainingDistance", remainingDistance);
        liveData.addProperty("drivenDistance", drivenDistance);
        liveData.addProperty("drivenDistanceSession", drivenDistanceSession);
        liveData.addProperty("dieselValue", dieselValue);
        liveData.addProperty("adblueValue", adblueValue);
        liveData.addProperty("truck", truck);
        liveData.addProperty("speed", speed);
        liveData.addProperty("speedLimit", speedLimit);
        liveData.addProperty("game", game);
        liveData.addProperty("token", liveToken);
        String message = JobTracker.gson.toJson(liveData);
        new JobTracker().wsSender.send(message);
    }
}
