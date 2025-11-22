import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final class Constants {
    private Constants() {}
    static final String PATIENTS_CSV = "patients.csv";
    static final String DOCTORS_CSV = "doctors.csv";
    static final String SERVICES_CSV = "services.csv";
    static final String APPOINTMENTS_CSV = "appointments.csv";
    static final String INVOICES_CSV = "invoices.csv";
    static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";
}

final class DateTimeUtil {
    private DateTimeUtil() {}
    static LocalDateTime parse(String text, String pattern) {
        return LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ofPattern(pattern));
    }
    static String format(LocalDateTime dt, String pattern) {
        return dt.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
    }
}

final class CsvStorage {
    private CsvStorage(){}
    static <T extends Persistable> void save(String path, List<T> items){
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(path))) {
            for (T t : items) { bw.write(t.toCSV()); bw.newLine(); }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    static <T> List<T> load(String path, Function<String, T> mapper){
        Path p = Paths.get(path);
        if (!Files.exists(p)) return new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(p)) {
            List<T> result = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                result.add(mapper.apply(line));
            }
            return result;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}

class AppointmentManager implements Schedulable {

    private final List<Patient> patients;
    private final List<Doctor> doctors;
    private final List<MedicalService> services;
    private final List<Appointment> appointments;
    private final List<Invoice> invoices;

    AppointmentManager(List<Patient> patients,
                       List<Doctor> doctors,
                       List<MedicalService> services,
                       List<Appointment> appointments,
                       List<Invoice> invoices) {
        this.patients = patients;
        this.doctors = doctors;
        this.services = services;
        this.appointments = appointments;
        this.invoices = invoices;
    }

    @Override
    public Appointment book(UUID patientId, UUID doctorId, UUID serviceId, LocalDateTime start) {
        Patient patient = patients.stream().filter(p -> p.getId().equals(patientId)).findFirst()
                .orElseThrow(() -> new PatientNotFoundException("Không tìm thấy bệnh nhân: " + patientId));
        Doctor doctor = doctors.stream().filter(d -> d.getId().equals(doctorId)).findFirst()
                .orElseThrow(() -> new DoctorNotFoundException("Không tìm thấy bác sĩ: " + doctorId));
        MedicalService service = services.stream().filter(s -> s.getId().equals(serviceId)).findFirst()
                .orElseThrow(() -> new ServiceNotFoundException("Không tìm thấy dịch vụ: " + serviceId));

        LocalDateTime end = start.plusMinutes(service.getDurationMinutes());

        if (!isAvailable(doctorId, start, end)) {
            throw new AppointmentConflictException("Bác sĩ " + doctor.getFullName() + " đã có lịch trong khoảng thời gian này.");
        }

        Appointment appt = new Appointment(UUID.randomUUID(), patient.getId(), doctor.getId(), service.getId(),
                start, end, AppointmentStatus.SCHEDULED);
        appointments.add(appt);

        // tạo hóa đơn theo chi phí dịch vụ
        Invoice inv = new Invoice(UUID.randomUUID(), appt.getId(), service.getCost(), LocalDateTime.now(), false);
        invoices.add(inv);

        return appt;
    }

    @Override public void cancel(UUID appointmentId)   { findAppt(appointmentId).setStatus(AppointmentStatus.CANCELLED); }
    @Override public void complete(UUID appointmentId) { findAppt(appointmentId).setStatus(AppointmentStatus.COMPLETED); }

    @Override
    public List<Appointment> listAppointmentsByDoctor(UUID doctorId) {
        return appointments.stream().filter(a -> a.getDoctorId().equals(doctorId)).collect(Collectors.toList());
    }

    @Override
    public boolean isAvailable(UUID doctorId, LocalDateTime start, LocalDateTime end) {
        return appointments.stream()
                .filter(a -> a.getDoctorId().equals(doctorId))
                .noneMatch(a -> a.getStart().isBefore(end) && start.isBefore(a.getEnd()));
    }

    private Appointment findAppt(UUID id) {
        return appointments.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn: " + id));
    }

    // Lưu/đọc CSV cho toàn bộ tập dữ liệu
    void saveAll() {
        CsvStorage.save(Constants.PATIENTS_CSV, patients);
        CsvStorage.save(Constants.DOCTORS_CSV, doctors);
        CsvStorage.save(Constants.SERVICES_CSV, services);
        CsvStorage.save(Constants.APPOINTMENTS_CSV, appointments);
        CsvStorage.save(Constants.INVOICES_CSV, invoices);
    }

    static AppointmentManager loadAll() {
        List<Patient> patients = CsvStorage.load(Constants.PATIENTS_CSV, Patient::fromCSV);
        List<Doctor> doctors = CsvStorage.load(Constants.DOCTORS_CSV, Doctor::fromCSV);
        List<MedicalService> services = CsvStorage.load(Constants.SERVICES_CSV, MedicalService::fromCSV);
        List<Appointment> appointments = CsvStorage.load(Constants.APPOINTMENTS_CSV, Appointment::fromCSV);
        List<Invoice> invoices = CsvStorage.load(Constants.INVOICES_CSV, Invoice::fromCSV);
        return new AppointmentManager(patients, doctors, services, appointments, invoices);
    }

    // getters
    List<Patient> getPatients(){ return patients; }
    List<Doctor> getDoctors(){ return doctors; }
    List<MedicalService> getServices(){ return services; }
    List<Appointment> getAppointments(){ return appointments; }
    List<Invoice> getInvoices(){ return invoices; }
}

final class DataSeeder {
    private DataSeeder(){}

    static AppointmentManager seedMinimum() {
        List<Patient> patients = new ArrayList<>();
        List<Doctor> doctors = new ArrayList<>();
        List<MedicalService> services = new ArrayList<>();
        List<Appointment> appointments = new ArrayList<>();
        List<Invoice> invoices = new ArrayList<>();

        // ≥ 20 bệnh nhân
        for (int i = 1; i <= 20; i++) {
            patients.add(new Patient(null, "Patient " + i, "090" + i, "Address " + i, "BHYT-" + i));
        }

        // ≥ 10 bác sĩ, nhiều khoa
        Department[] deps = Department.values();
        for (int i = 1; i <= 10; i++) {
            doctors.add(new Doctor(null, "Dr " + i, "098" + i, "Clinic " + i, deps[i % deps.length]));
        }

        // ≥ 15 dịch vụ
        for (int i = 1; i <= 5; i++) services.add(new ExaminationService(null, "Khám tổng quát " + i, 150_000, 20));
        for (int i = 1; i <= 5; i++) services.add(new TestService(null, "Xét nghiệm " + i, 200_000, 15));
        for (int i = 1; i <= 5; i++) services.add(new SurgeryService(null, "Phẫu thuật " + i, 5_000_000, 120));

        // ≥ 30 lịch khám ngẫu nhiên, tránh trùng lịch theo bác sĩ
        Random rnd = new Random(7);
        for (int i = 0; i < 30; i++) {
            Patient p = patients.get(rnd.nextInt(patients.size()));
            Doctor d = doctors.get(rnd.nextInt(doctors.size()));
            MedicalService s = services.get(rnd.nextInt(services.size()));

            LocalDateTime start = LocalDateTime.now().plusDays(rnd.nextInt(10)).withHour(8 + rnd.nextInt(9)).withMinute(0);
            LocalDateTime end = start.plusMinutes(s.getDurationMinutes());

            boolean conflict = appointments.stream()
                    .filter(a -> a.getDoctorId().equals(d.getId()))
                    .anyMatch(a -> start.isBefore(a.getEnd()) && a.getStart().isBefore(end));
            if (conflict) { i--; continue; }

            Appointment a = new Appointment(null, p.getId(), d.getId(), s.getId(), start, end, AppointmentStatus.SCHEDULED);
            appointments.add(a);
            invoices.add(new Invoice(null, a.getId(), s.getCost(), LocalDateTime.now(), false));
        }

        return new AppointmentManager(patients, doctors, services, appointments, invoices);
    }

    // Báo cáo: Top 3 bác sĩ nhiều lịch nhất
    static List<Map.Entry<UUID, Long>> top3DoctorsByAppointments(AppointmentManager mgr) {
        Map<UUID, Long> count = new HashMap<>();
        for (Appointment a : mgr.getAppointments()) {
            count.put(a.getDoctorId(), count.getOrDefault(a.getDoctorId(), 0L) + 1);
        }
        return count.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(3)
                .toList();
    }

    // Báo cáo: doanh thu theo tháng (tổng tiền hóa đơn trong tháng chỉ định)
    static double monthlyRevenue(AppointmentManager mgr, int year, int month) {
        return mgr.getInvoices().stream()
                .filter(inv -> inv.getCreatedAt().getYear() == year && inv.getCreatedAt().getMonthValue() == month)
                .mapToDouble(Invoice::getAmount)
                .sum();
    }
}