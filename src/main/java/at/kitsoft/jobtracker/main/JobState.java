package at.kitsoft.jobtracker.main;

public class JobState {

    private Long jobId;
    private double startOdometer;
    private double maxSpeed, usedDiesel, usedAdblue;
    // Hier können später weitere Felder hinzugefügt werden, z.B. username.

    /**
     * Ein leerer Konstruktor ist für Bibliotheken wie Gson erforderlich,
     * um beim Deserialisieren (JSON -> Objekt) eine Instanz erstellen zu können.
     */
    public JobState() {
    }

    // --- Getter und Setter für jedes Feld ---
    // Gson verwendet diese Methoden, um auf die privaten Felder zuzugreifen.

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public double getStartOdometer() {
        return startOdometer;
    }

    public void setStartOdometer(double startOdometer) {
        this.startOdometer = startOdometer;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
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
}
