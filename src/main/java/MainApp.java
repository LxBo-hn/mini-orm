import com.t2308e.config.DataSourceConfig;
import com.t2308e.core.MyRepositoryFactory;
import com.t2308e.entity.User;
import com.t2308e.repository.UserRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class MainApp {

    // Cấu hình H2 in-memory database
    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"; // DB_CLOSE_DELAY=-1 giữ DB tồn tại
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";
    private static final String H2_DRIVER = "org.h2.Driver";

    public static void main(String[] args) {
        // 1. Cấu hình DataSource
        DataSourceConfig dataSourceConfig = new DataSourceConfig(H2_URL, H2_USER, H2_PASSWORD, H2_DRIVER);

        // 2. Khởi tạo bảng (chỉ cho mục đích demo)
        setupDatabase(dataSourceConfig);

        // 3. Khởi tạo Repository Factory
        MyRepositoryFactory repositoryFactory = new MyRepositoryFactory(dataSourceConfig);

        // 4. Lấy UserRepository
        UserRepository userRepository = repositoryFactory.createRepository(UserRepository.class);

        // 5. Thực hiện các thao tác CRUD
        System.out.println("--- Testing Mini ORM ---");

        // Save new users
        System.out.println("\n[SAVE] Creating users...");
        User user1 = new User("Alice Wonderland", "alice@example.com", 30);
        User user2 = new User("Bob The Builder", "bob@example.com", 45);

        user1 = userRepository.save(user1);
        System.out.println("Saved: " + user1);
        user2 = userRepository.save(user2);
        System.out.println("Saved: " + user2);

        // Count users
        System.out.println("\n[COUNT] Total users: " + userRepository.count());

        // Find by ID
        System.out.println("\n[FIND BY ID] Finding user with ID 1...");
        Optional<User> foundUserOpt = userRepository.findById(1L);
        foundUserOpt.ifPresent(u -> System.out.println("Found: " + u));

        if (user1.getId() != null) {
            Optional<User> foundUser1Opt = userRepository.findById(user1.getId());
            foundUser1Opt.ifPresent(u -> System.out.println("Found User1 by retrieved ID: " + u));
        }


        // Find all users
        System.out.println("\n[FIND ALL] All users:");
        List<User> allUsers = userRepository.findAll();
        allUsers.forEach(System.out::println);

        // Update a user
        System.out.println("\n[UPDATE] Updating Alice...");
        if (foundUserOpt.isPresent()) {
            User userToUpdate = foundUserOpt.get();
            userToUpdate.setAge(32);
            userToUpdate.setEmail("alice.updated@example.com");
            userRepository.save(userToUpdate);
            System.out.println("Updated: " + userToUpdate);

            // Verify update
            userRepository.findById(userToUpdate.getId()).ifPresent(u -> System.out.println("Verified update: " + u));
        }


        // Delete a user
        if (user2.getId() != null) {
            System.out.println("\n[DELETE] Deleting Bob (ID: " + user2.getId() + ")");
            userRepository.deleteById(user2.getId());
            System.out.println("Bob deleted.");
            System.out.println("Total users after delete: " + userRepository.count());

            System.out.println("\n[FIND ALL] All users after delete:");
            userRepository.findAll().forEach(System.out::println);
        } else {
            System.out.println("\n[DELETE] Cannot delete Bob, ID is null.");
        }


        // Test find non-existent user
        System.out.println("\n[FIND BY ID] Finding non-existent user (ID 999)...");
        Optional<User> nonExistentUser = userRepository.findById(999L);
        if (nonExistentUser.isEmpty()) {
            System.out.println("User with ID 999 not found, as expected.");
        }

        System.out.println("\n--- Mini ORM Test Complete ---");
    }

    private static void setupDatabase(DataSourceConfig config) {
        try (Connection conn = config.getConnection();
             Statement stmt = conn.createStatement()) {
            // Xóa bảng nếu tồn tại (để mỗi lần chạy demo là mới)
            stmt.execute("DROP TABLE IF EXISTS users");

            // Tạo bảng users
            // Chú ý: tên cột phải khớp với @MyColumn hoặc tên trường
            // INTEGER PRIMARY KEY AUTO_INCREMENT for H2
            // BIGINT AUTO_INCREMENT PRIMARY KEY for MySQL
            // SQLite: INTEGER PRIMARY KEY AUTOINCREMENT
            stmt.execute("CREATE TABLE users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " + // H2, MySQL like
                    "user_name VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(255) UNIQUE, " +
                    "age INT" +
                    ")");
            System.out.println("Database table 'users' created successfully.");
        } catch (SQLException e) {
            System.err.println("Error setting up database: " + e.getMessage());
            throw new RuntimeException("Database setup failed", e);
        }
    }
}
