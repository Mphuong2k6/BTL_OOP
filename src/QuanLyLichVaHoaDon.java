import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class Appointment implements Persistable {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID serviceId;
    private LocalDateTime start;
    private LocalDateTime end;
    private AppointmentStatus status;

    public Appointment() {}
    public Appointment(UUID id, UUID patientId, UUID doctorId, UUID serviceId,
                       LocalDateTime start, LocalDateTime end, AppointmentStatus status){
        this.id = (id == null ? UUID.randomUUID() : id);
        this.patientId = patientId; this.doctorId = doctorId; this.serviceId = serviceId;
        this.start = start; this.end = end; this.status = status==null? AppointmentStatus.SCHEDULED : status;
    }

    public UUID getId(){ return id; }
    public UUID getPatientId(){ return patientId; }
    public UUID getDoctorId(){ return doctorId; }
    public UUID getServiceId(){ return serviceId; }
    public LocalDateTime getStart(){ return start; }
    public LocalDateTime getEnd(){ return end; }
    public AppointmentStatus getStatus(){ return status; }
    public void setStatus(AppointmentStatus status){ this.status = status; }

    @Override
    public String toCSV(){
        return id + "," + patientId + "," + doctorId + "," + serviceId + ","
                + DateTimeUtil.format(start, Constants.DATE_TIME_PATTERN) + ","
                + DateTimeUtil.format(end, Constants.DATE_TIME_PATTERN) + ","
                + status.name();
    }
    public static Appointment fromCSV(String line){
        String[] p = line.split(",", -1);
        UUID id = UUID.fromString(p[0]);
        UUID pid = UUID.fromString(p[1]);
        UUID did = UUID.fromString(p[2]);
        UUID sid = UUID.fromString(p[3]);
        LocalDateTime st = DateTimeUtil.parse(p[4], Constants.DATE_TIME_PATTERN);
        LocalDateTime en = DateTimeUtil.parse(p[5], Constants.DATE_TIME_PATTERN);
        AppointmentStatus status = AppointmentStatus.valueOf(p[6]);
        return new Appointment(id, pid, did, sid, st, en, status);
    }
}

class Invoice implements Persistable {
    private UUID id;
    private UUID appointmentId;
    private double amount;
    private LocalDateTime createdAt;
    private boolean paid;

    public Invoice(){}
    public Invoice(UUID id, UUID appointmentId, double amount, LocalDateTime createdAt, boolean paid){
        this.id = (id == null ? UUID.randomUUID() : id);
        this.appointmentId = appointmentId; this.amount = amount;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.paid = paid;
    }
    public UUID getId(){ return id; }
    public UUID getAppointmentId(){ return appointmentId; }
    public double getAmount(){ return amount; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public boolean isPaid(){ return paid; }
    public void markPaid(){ this.paid = true; }

    @Override
    public String toCSV(){
        return id + "," + appointmentId + "," + amount + ","
                + DateTimeUtil.format(createdAt, Constants.DATE_TIME_PATTERN) + "," + paid;
    }
    public static Invoice fromCSV(String line){
        String[] p = line.split(",", -1);
        return new Invoice(UUID.fromString(p[0]), UUID.fromString(p[1]),
                Double.parseDouble(p[2]),
                DateTimeUtil.parse(p[3], Constants.DATE_TIME_PATTERN),
                Boolean.parseBoolean(p[4]));
    }
}

// ===== ngoại lệ tuỳ biến =====
class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message){ super(message); }
}
class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException(String message){ super(message); }
}
class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String message){ super(message); }
}
class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String message){ super(message); }
}

// ===== interface lịch hẹn =====
interface Schedulable {
    Appointment book(UUID patientId, UUID doctorId, UUID serviceId, LocalDateTime start);
    void cancel(UUID appointmentId);
    void complete(UUID appointmentId);
    List<Appointment> listAppointmentsByDoctor(UUID doctorId);
    boolean isAvailable(UUID doctorId, LocalDateTime start, LocalDateTime end);
}