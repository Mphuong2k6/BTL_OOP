import java.util.UUID;

abstract class MedicalService implements Persistable {
    protected UUID id;
    protected String name;
    protected double baseCost;      // chi phí cơ bản
    protected int durationMinutes;  // thời lượng dự kiến

    protected MedicalService(){ this.id = UUID.randomUUID(); }
    protected MedicalService(UUID id, String name, double baseCost, int durationMinutes){
        this.id = (id == null ? UUID.randomUUID() : id);
        this.name = name; this.baseCost = baseCost; this.durationMinutes = durationMinutes;
    }

    public UUID getId(){ return id; }
    public String getName(){ return name; }
    public double getBaseCost(){ return baseCost; }
    public int getDurationMinutes(){ return durationMinutes; }

    /** Mỗi loại dịch vụ tự tính chi phí chi tiết */
    public abstract double getCost();

    @Override
    public String toCSV() {
        // type, id, name, baseCost, durationMinutes, cost
        return getClass().getSimpleName() + "," + id + "," + esc(name) + "," + baseCost + "," + durationMinutes + "," + getCost();
    }
    static String esc(String s){ return s==null? "" : s.replace(",", "\\,"); }
    static String unesc(String s){ return s.replace("\\,", ","); }

    public static MedicalService fromCSV(String line){
        String[] p = line.split(",", -1);
        String type = p[0];
        UUID id = UUID.fromString(p[1]);
        String name = unesc(p[2]);
        double base = Double.parseDouble(p[3]);
        int dur = Integer.parseInt(p[4]);
        switch (type){
            case "ExaminationService": return new ExaminationService(id, name, base, dur);
            case "SurgeryService":     return new SurgeryService(id, name, base, dur);
            case "TestService":        return new TestService(id, name, base, dur);
            default: throw new IllegalArgumentException("Unknown service type: " + type);
        }
    }
}

/** Khám bệnh thông thường */
class ExaminationService extends MedicalService {
    public ExaminationService(){ super(); }
    public ExaminationService(UUID id, String name, double baseCost, int durationMinutes){
        super(id, name, baseCost, durationMinutes);
    }
    @Override public double getCost(){ return baseCost; }
}

/** Phẫu thuật: phụ phí theo thời lượng */
class SurgeryService extends MedicalService {
    public SurgeryService(){ super(); }
    public SurgeryService(UUID id, String name, double baseCost, int durationMinutes){
        super(id, name, baseCost, durationMinutes);
    }
    @Override public double getCost(){
        double blocks = Math.ceil(durationMinutes / 30.0); // 500k mỗi 30'
        return baseCost + blocks * 500_000;
    }
}

/** Xét nghiệm: phụ phí vật tư 15% */
class TestService extends MedicalService {
    public TestService(){ super(); }
    public TestService(UUID id, String name, double baseCost, int durationMinutes){
        super(id, name, baseCost, durationMinutes);
    }
    @Override public double getCost(){ return baseCost * 1.15; }
}