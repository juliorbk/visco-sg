import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductMigrationService {

    private final JdbcTemplate jdbcTemplate;
    
    // Tamaño del lote: Cada 5000 registros hace un INSERT masivo a la BD
    private static final int BATCH_SIZE = 5000;

    public ProductMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void importProductsFromCsv(MultipartFile file) {
        // Usamos ON CONFLICT para que si un código está duplicado en el Excel, no aborte los 270k registros
        String sql = """
            INSERT INTO products 
            (internal_code, sap_code, name, sku, uom, description, category_id, is_active, reorder_point) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (internal_code) DO NOTHING
            """;

        List<Object[]> batch = new ArrayList<>();
        int totalInserted = 0;

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, 
                     CSVFormat.DEFAULT.builder()
                             .setHeader() // Asume que la primera fila tiene los nombres de las columnas
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .build())) {

            for (CSVRecord csvRecord : csvParser) {
                // Mapeamos las columnas del CSV según el orden o el nombre del Header
                Object[] values = new Object[]{
                        csvRecord.get("internal_code"),
                        csvRecord.get("sap_code"),
                        csvRecord.get("name"),
                        csvRecord.get("sku"),
                        csvRecord.get("uom"),
                        csvRecord.get("description"),
                        Long.parseLong(csvRecord.get("category_id")), // ID que mapeamos previamente
                        true, // is_active
                        0.00  // reorder_point
                };

                batch.add(values);

                // Cuando el lote llega a 5000, inyectamos a PostgreSQL y limpiamos la lista
                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    totalInserted += batch.size();
                    System.out.println("Insertados hasta ahora: " + totalInserted);
                    batch.clear(); // Liberamos memoria
                }
            }

            // Insertar los registros restantes que no completaron el último lote de 5000
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
                totalInserted += batch.size();
                System.out.println("Carga finalizada. Total insertados: " + totalInserted);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el archivo CSV: " + e.getMessage());
        }
    }
}