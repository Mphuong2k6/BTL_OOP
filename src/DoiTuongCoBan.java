import java.util.UUID;

interface Persistable {
    /** Xuất 1 dòng CSV đại diện cho đối tượng */
    String toCSV();
}

enum Department {
    GENERAL,
    CARDIOLOGY,
    NEUROLOGY,
    ORTHOPEDICS,
    PEDIATRICS,
    DERMATOLOGY
}

enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}

abstract class Person implements Persistable {
    protected UUID id;
    protected String fullName;
    protected String phone;
    protected String address;

    protected Person() { this.id = UUID.randomUUID(); }
    protected Person(UUID id, String fullName, String phone, String address) {
        this.id = (id == null ? UUID.randomUUID() : id);
        this.fullName = fullName; this.phone = phone; this.address = address;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}

class Patient extends Person {
    private String insuranceNumber; // mã BHYT (có thể để trống)

    public Patient() { super(); }
    public Patient(UUID id, String fullName, String phone, String address, String insuranceNumber) {
        super(id, fullName, phone, address);
        this.insuranceNumber = insuranceNumber;
    }
    public String getInsuranceNumber() { return insuranceNumber; }
    public void setInsuranceNumber(String insuranceNumber) { this.insuranceNumber = insuranceNumber; }

    @Override
    public String toCSV() {
        return id + "," + esc(fullName) + "," + esc(phone) + "," + esc(address) + "," + esc(insuranceNumber);
    }
    public static Patient fromCSV(String line) {
        String[] p = line.split(",", -1);
        return new Patient(UUID.fromString(p[0]), unesc(p[1]), unesc(p[2]), unesc(p[3]), unesc(p[4]));
    }
    static String esc(String s){ return s==null? "" : s.replace(",", "\\,"); }
    static String unesc(String s){ return s.replace("\\,", ","); }
}

class Doctor extends Person {
    private Department department;

    public Doctor() { super(); }
    public Doctor(UUID id, String fullName, String phone, String address, Department department) {
        super(id, fullName, phone, address);
        this.department = department;
    }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    @Override
    public String toCSV() {
        return id + "," + esc(fullName) + "," + esc(phone) + "," + esc(address) + "," + department.name();
    }
    public static Doctor fromCSV(String line) {
        String[] p = line.split(",", -1);
        return new Doctor(UUID.fromString(p[0]), unesc(p[1]), unesc(p[2]), unesc(p[3]), Department.valueOf(p[4]));
    }
    static String esc(String s){ return s==null? "" : s.replace(",", "\\,"); }
    static String unesc(String s){ return s.replace("\\,", ","); }
}