package com.itsm.itsm.config;

import com.itsm.itsm.model.*;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.SlaPolicyRepository;
import com.itsm.itsm.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// Runs once at startup and fills an empty database with sample data.
// Test accounts (all use DEMO_PASSWORD):
//   admin@itsm.local                                  - Admin
//   ravi@itsm.local (team lead), meera@itsm.local, karan@itsm.local - Technicians
//   asha@itsm.local, vikram@itsm.local                - Employees
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "demo1234";

    private final CategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoryRepository categoryRepository, ITServiceRepository serviceRepository,
                      UserRepository userRepository, SlaPolicyRepository slaPolicyRepository,
                      PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Each part is only added if its table is still empty
    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            seedServicesCategoriesAndUsers();
        }
        if (slaPolicyRepository.count() == 0) {
            seedSlaPolicies();
        }
    }

    private void seedSlaPolicies() {
        // Defaults for every service (service = null), from the PDF's table
        slaPolicyRepository.save(new SlaPolicy(null, Priority.P1, 5, 30));
        slaPolicyRepository.save(new SlaPolicy(null, Priority.P2, 15, 120));
        slaPolicyRepository.save(new SlaPolicy(null, Priority.P3, 60, 480));
        slaPolicyRepository.save(new SlaPolicy(null, Priority.P4, 240, 1440));

        // Payments are business-critical, so a P2 there gets a stricter deadline
        serviceRepository.findByName("Payment Application").ifPresent(payment ->
                slaPolicyRepository.save(new SlaPolicy(payment, Priority.P2, 10, 60)));
    }

    private void seedServicesCategoriesAndUsers() {

        Map<String, Category> categories = new HashMap<>();
        addCategory(categories, "Hardware", "laptop,monitor,keyboard,mouse,printer,screen");
        addCategory(categories, "Software", "install,license,update,crash,excel,word");
        addCategory(categories, "Network", "wifi,internet,vpn,router,network,slow");
        addCategory(categories, "Database", "database,sql,query,table,records");
        addCategory(categories, "Cloud", "cloud,aws,azure,server,vm");
        addCategory(categories, "Security", "virus,malware,phishing,breach,hack");
        addCategory(categories, "Authentication", "login,password,otp,locked,sso");
        addCategory(categories, "Application", "app,portal,payment,website,error");
        addCategory(categories, "Storage", "disk,storage,drive,backup,quota");

        serviceRepository.save(new ITService("Payment Application", 3, "Fee and salary payments"));
        serviceRepository.save(new ITService("Student Database", 3, "Student records system"));
        serviceRepository.save(new ITService("Email", 2, "Campus email"));
        serviceRepository.save(new ITService("Campus Wi-Fi", 2, "Wireless network"));
        serviceRepository.save(new ITService("Cloud Storage", 2, "Shared drives"));
        serviceRepository.save(new ITService("HR Portal", 1, "Leave and HR requests"));
        serviceRepository.save(new ITService("Printing", 1, "Printers and print queue"));

        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        userRepository.save(new Admin("Admin", "admin@itsm.local", hash));

        Technician ravi = new Technician("Ravi Kumar", "ravi@itsm.local", hash, 5, true);
        ravi.addSkill(categories.get("Network"));
        ravi.addSkill(categories.get("Security"));
        ravi.addSkill(categories.get("Cloud"));
        userRepository.save(ravi);

        Technician meera = new Technician("Meera Iyer", "meera@itsm.local", hash, 5, false);
        meera.addSkill(categories.get("Application"));
        meera.addSkill(categories.get("Database"));
        meera.addSkill(categories.get("Authentication"));
        userRepository.save(meera);

        Technician karan = new Technician("Karan Singh", "karan@itsm.local", hash, 4, false);
        karan.addSkill(categories.get("Hardware"));
        karan.addSkill(categories.get("Software"));
        karan.addSkill(categories.get("Storage"));
        userRepository.save(karan);

        userRepository.save(new Employee("Asha Rao", "asha@itsm.local", hash, "Finance"));
        userRepository.save(new Employee("Vikram Shah", "vikram@itsm.local", hash, "Academics"));
    }

    private void addCategory(Map<String, Category> categories, String name, String keywords) {
        categories.put(name, categoryRepository.save(new Category(name, keywords)));
    }
}
