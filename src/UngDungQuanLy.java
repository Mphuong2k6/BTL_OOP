import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class UngDungQuanLy {

    private static final Scanner SC = new Scanner(System.in);

    private static void pause() {
        System.out.println("\nNhấn Enter để tiếp tục...");
        SC.nextLine();
    }

    private static void printHeader(String title) {
        System.out.println("\n==============================");
        System.out.println(title);
        System.out.println("==============================");
    }

    public static void main(String[] args) {
        // Nạp dữ liệu từ CSV nếu có; nếu trống thì seed mẫu cho đủ data theo đề
        AppointmentManager manager = AppointmentManager.loadAll();
        if (manager.getPatients().isEmpty() && manager.getDoctors().isEmpty() && manager.getServices().isEmpty()) {
            manager = DataSeeder.seedMinimum();
            manager.saveAll();
        }

        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════╗");
            System.out.println("║ QUẢN LÝ HỒ SƠ BỆNH NHÂN & LỊCH KHÁM                 ║");
            System.out.println("╠══════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Danh sách bệnh nhân                              ║");
            System.out.println("║ 2. Thêm bệnh nhân                                    ║");
            System.out.println("║ 3. Danh sách bác sĩ                                  ║");
            System.out.println("║ 4. Thêm bác sĩ                                        ║");
            System.out.println("║ 5. Danh sách dịch vụ                                  ║");
            System.out.println("║ 6. Đặt lịch khám                                      ║");
            System.out.println("║ 7. Xem lịch của bác sĩ                                ║");
            System.out.println("║ 8. Hủy/Hoàn tất lịch                                  ║");
            System.out.println("║ 9. Lưu tất cả ra CSV                                  ║");
            System.out.println("║ 10. Báo cáo: Top 3 bác sĩ nhiều lịch                  ║");
            System.out.println("║ 11. Báo cáo: Doanh thu tháng hiện tại                 ║");
            System.out.println("║ 0. Thoát                                              ║");
            System.out.println("╚══════════════════════════════════════════════════════╝");
            System.out.print("Nhập lựa chọn: ");

            int choice;
            try { choice = Integer.parseInt(SC.nextLine().trim()); } catch (Exception e) { choice = -1; }
            if (choice == 0) break;

            switch (choice) {
                case 1 -> { // danh sách bệnh nhân
                    printHeader("Danh sách bệnh nhân");
                    for (Patient p : manager.getPatients()) {
                        System.out.printf("- %s | %s | %s | BHYT: %s | ID=%s%n",
                                p.getFullName(), p.getPhone(), p.getAddress(), p.getInsuranceNumber(), p.getId());
                    }
                    pause();
                }
                case 2 -> { // thêm bệnh nhân
                    printHeader("Thêm bệnh nhân");
                    System.out.print("Họ tên: "); String name = SC.nextLine();
                    System.out.print("SĐT: "); String phone = SC.nextLine();
                    System.out.print("Địa chỉ: "); String addr = SC.nextLine();
                    System.out.print("Mã BHYT (để trống nếu không có): "); String bhyt = SC.nextLine();
                    Patient p = new Patient(null, name, phone, addr, bhyt);
                    manager.getPatients().add(p);
                    System.out.println(">> Đã thêm bệnh nhân " + name + ". ID=" + p.getId());
                    pause();
                }
                case 3 -> { // danh sách bác sĩ
                    printHeader("Danh sách bác sĩ");
                    for (Doctor d : manager.getDoctors()) {
                        System.out.printf("- %s | Khoa: %s | SĐT: %s | ID=%s%n", d.getFullName(), d.getDepartment(), d.getPhone(), d.getId());
                    }
                    pause();
                }
                case 4 -> { // thêm bác sĩ
                    printHeader("Thêm bác sĩ");
                    System.out.print("Họ tên: "); String name = SC.nextLine();
                    System.out.print("SĐT: "); String phone = SC.nextLine();
                    System.out.print("Địa chỉ: "); String addr = SC.nextLine();
                    System.out.println("Chọn khoa:");
                    Department[] deps = Department.values();
                    for (int i = 0; i < deps.length; i++) System.out.printf("%d) %s%n", i + 1, deps[i]);
                    int idx = Integer.parseInt(SC.nextLine().trim());
                    Department dep = deps[Math.max(1, Math.min(idx, deps.length)) - 1];
                    Doctor d = new Doctor(null, name, phone, addr, dep);
                    manager.getDoctors().add(d);
                    System.out.println(">> Đã thêm bác sĩ " + name + ". ID=" + d.getId());
                    pause();
                }
                case 5 -> { // danh sách dịch vụ
                    printHeader("Danh sách dịch vụ");
                    for (MedicalService s : manager.getServices()) {
                        System.out.printf("- [%s] %s | base=%.0f | cost=%.0f | %d phút | ID=%s%n",
                                s.getClass().getSimpleName(), s.getName(), s.getBaseCost(), s.getCost(), s.getDurationMinutes(), s.getId());
                    }
                    pause();
                }
                case 6 -> { // đặt lịch
                    printHeader("Đặt lịch khám");
                    UUID pid = pickPatient(manager.getPatients());
                    UUID did = pickDoctor(manager.getDoctors());
                    UUID sid = pickService(manager.getServices());
                    System.out.print("Nhập thời gian bắt đầu (yyyy-MM-dd HH:mm): ");
                    String txt = SC.nextLine();
                    LocalDateTime start = DateTimeUtil.parse(txt, Constants.DATE_TIME_PATTERN);
                    try {
                        Appointment a = manager.book(pid, did, sid, start);
                        System.out.println(">> Đặt lịch thành công. Appointment ID=" + a.getId());
                    } catch (AppointmentConflictException ex) {
                        System.out.println("!! Không thể đặt lịch: " + ex.getMessage());
                    } catch (RuntimeException ex) {
                        System.out.println("!! Lỗi: " + ex.getMessage());
                    }
                    pause();
                }
                case 7 -> { // xem lịch theo bác sĩ
                    printHeader("Xem lịch của bác sĩ");
                    UUID did = pickDoctor(manager.getDoctors());
                    List<Appointment> list = manager.listAppointmentsByDoctor(did);
                    if (list.isEmpty()) System.out.println("Không có lịch.");
                    else for (Appointment a : list) {
                        System.out.printf("- [%s] %s -> %s | Trạng thái: %s | ApptID=%s%n",
                                a.getServiceId(),
                                DateTimeUtil.format(a.getStart(), Constants.DATE_TIME_PATTERN),
                                DateTimeUtil.format(a.getEnd(), Constants.DATE_TIME_PATTERN),
                                a.getStatus(), a.getId());
                    }
                    pause();
                }
                case 8 -> { // hủy/hoàn tất lịch
                    printHeader("Hủy/Hoàn tất lịch");
                    List<Appointment> recent = manager.getAppointments().stream().limit(10).collect(Collectors.toList());
                    for (int i = 0; i < recent.size(); i++) {
                        Appointment a = recent.get(i);
                        System.out.printf("%d) %s | %s -> %s | %s | ID=%s%n", i + 1,
                                a.getServiceId(),
                                DateTimeUtil.format(a.getStart(), Constants.DATE_TIME_PATTERN),
                                DateTimeUtil.format(a.getEnd(), Constants.DATE_TIME_PATTERN),
                                a.getStatus(), a.getId());
                    }
                    System.out.print("Chọn số thứ tự: ");
                    int idx = Integer.parseInt(SC.nextLine().trim());
                    Appointment chosen = recent.get(Math.max(1, Math.min(idx, recent.size())) - 1);
                    System.out.print("Nhập hành động (C=Cancel, D=Done): ");
                    String act = SC.nextLine().trim().toUpperCase();
                    if ("C".equals(act)) { manager.cancel(chosen.getId()); System.out.println(">> Đã hủy lịch."); }
                    else if ("D".equals(act)) { manager.complete(chosen.getId()); System.out.println(">> Đã hoàn tất lịch."); }
                    else System.out.println("Hành động không hợp lệ.");
                    pause();
                }
                case 9 -> { // lưu CSV
                    manager.saveAll();
                    System.out.println(">> Đã lưu tất cả dữ liệu ra CSV.");
                    pause();
                }
                case 10 -> { // báo cáo top 3
                    printHeader("Báo cáo: Top 3 bác sĩ nhiều lịch");
                    var top3 = DataSeeder.top3DoctorsByAppointments(manager);
                    for (var e : top3) {
                        Doctor d = manager.getDoctors().stream().filter(x -> x.getId().equals(e.getKey())).findFirst().orElse(null);
                        if (d != null) System.out.printf("- %s (%s): %d lịch%n", d.getFullName(), d.getDepartment(), e.getValue());
                    }
                    pause();
                }
                case 11 -> { // doanh thu tháng hiện tại
                    printHeader("Báo cáo: Doanh thu tháng hiện tại");
                    LocalDateTime now = LocalDateTime.now();
                    double revenue = DataSeeder.monthlyRevenue(manager, now.getYear(), now.getMonthValue());
                    System.out.printf(">> Doanh thu %d-%02d: %.0f VND%n", now.getYear(), now.getMonthValue(), revenue);
                    pause();
                }
                default -> System.out.println("Lựa chọn không hợp lệ!");
            }
        }
        System.out.println("Thoát chương trình.");
    }

    // ======= Helpers chọn ID theo danh sách (giữ style gọn, giống cách bạn làm) =======
    private static UUID pickPatient(List<Patient> patients) {
        System.out.println("Chọn bệnh nhân (nhập số):");
        for (int i = 0; i < patients.size(); i++)
            System.out.printf("%d) %s | ID=%s%n", i + 1, patients.get(i).getFullName(), patients.get(i).getId());
        int idx = Integer.parseInt(SC.nextLine().trim());
        return patients.get(Math.max(1, Math.min(idx, patients.size())) - 1).getId();
    }
    private static UUID pickDoctor(List<Doctor> doctors) {
        System.out.println("Chọn bác sĩ (nhập số):");
        for (int i = 0; i < doctors.size(); i++)
            System.out.printf("%d) %s (%s) | ID=%s%n", i + 1, doctors.get(i).getFullName(), doctors.get(i).getDepartment(), doctors.get(i).getId());
        int idx = Integer.parseInt(SC.nextLine().trim());
        return doctors.get(Math.max(1, Math.min(idx, doctors.size())) - 1).getId();
    }
    private static UUID pickService(List<MedicalService> services) {
        System.out.println("Chọn dịch vụ (nhập số):");
        for (int i = 0; i < services.size(); i++) {
            MedicalService s = services.get(i);
            System.out.printf("%d) [%s] %s | cost=%.0f | %d phút | ID=%s%n",
                    i + 1, s.getClass().getSimpleName(), s.getName(), s.getCost(), s.getDurationMinutes(), s.getId());
        }
        int idx = Integer.parseInt(SC.nextLine().trim());
        return services.get(Math.max(1, Math.min(idx, services.size())) - 1).getId();
    }
}