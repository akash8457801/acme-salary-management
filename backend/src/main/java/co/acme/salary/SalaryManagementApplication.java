package co.acme.salary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SalaryManagementApplication {

    private static final String DEFAULT_DB_PATH = "./data/salary.db";

    public static void main(String[] args) {
        ensureDatabaseDirectoryExists();
        SpringApplication.run(SalaryManagementApplication.class, args);
    }

    /**
     * SQLite creates the database file but not the directory holding it, and the datasource is
     * built before any bean of ours runs. Creating it here is what makes {@code java -jar} work in
     * a fresh checkout, or in a container with a mounted volume, without a setup step.
     */
    private static void ensureDatabaseDirectoryExists() {
        String configured = System.getenv().getOrDefault("SALARY_DB_PATH", DEFAULT_DB_PATH);
        Path directory = Paths.get(configured).toAbsolutePath().getParent();
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the database directory at " + directory, e);
        }
    }
}
