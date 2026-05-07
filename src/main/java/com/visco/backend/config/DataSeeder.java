package com.visco.backend.config;

import com.visco.backend.models.entities.*;
import com.visco.backend.repositories.*;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataSeeder {

  @Bean
  @Transactional // Usamos Transactional para manejar bien las relaciones bidireccionales y guardados en cascada
  CommandLineRunner initDatabase(
    SupplierRepository supplierRepository,
    WarehouseRepository warehouseRepository,
    ProductRepository productRepository, // Tu repositorio de productos actual
    StockLevelRepository stockLevelRepository, // El nuevo repositorio que creamos
    UserRepository userRepository
  ) {
    return args -> {
      // 1. Crear Proveedor (Si no existe)
      Supplier mainSupplier;
      if (supplierRepository.count() == 0) {
        Supplier newSupplier = new Supplier();
        newSupplier.setName("Ferretería Industrial Guayana");
        newSupplier.setAddress("Zona Industrial Matanzas");
        newSupplier.setEmail("ventas@ferreindustrial.com");
        newSupplier.setPhone("0414-1234567");
        newSupplier.setDescription("Proveedor principal de tuberías");
        newSupplier.setActive(true);
        mainSupplier = supplierRepository.save(newSupplier);
        System.out.println("✅ Proveedor sembrado.");
      } else {
        mainSupplier = supplierRepository.findAll().get(0);
      }

      // 2. Crear Almacén y su Ubicación Interna (Si no existe)
      Warehouse mainWarehouse;
      Location pasilloA = null;
      if (warehouseRepository.count() == 0) {
        mainWarehouse = Warehouse.builder()
          .name("Almacén Principal Matanzas")
          .physicalAddress("Calle 2, Galpón 4, Puerto Ordaz")
          .description("Almacén central de repuestos e insumos")
          .build();

        // Instanciamos una ubicación (Ajusta los campos según como tengas tu clase Location.java)
        pasilloA = new Location();
        // Asumiendo que Location tiene un campo de código/nombre y la relación al almacén
        // pasilloA.setCode("PAS-A-EST-1");
        // pasilloA.setWarehouse(mainWarehouse);

        // Agregamos la ubicación al almacén (gracias al CascadeType.ALL, se guardará sola)
        mainWarehouse.getStorageLocations().add(pasilloA);

        mainWarehouse = warehouseRepository.save(mainWarehouse);
        // Recuperamos la ubicación recién guardada para usarla en el stock
        pasilloA = mainWarehouse.getStorageLocations().iterator().next();
        System.out.println("✅ Almacén y Ubicación sembrados.");
      }

      // 3. Crear Producto en el Catálogo (Si no existe)
      Product product1;
      if (productRepository.count() == 0) {
        product1 = Product.builder()
          .internalCode("VIS-001")
          .sku("SKU-TUB-001")
          .name("Tubo de Acero al Carbono 4 pulgadas")
          .sapCode("SAP-998877")
          .uom(Uom.METRO) //
          .reorderPoint(new BigDecimal("20.00")) // Solo conservamos el punto de reorden
          .active(true)
          .supplier(mainSupplier)
          .build();

        product1 = productRepository.save(product1);
        System.out.println("✅ Producto (Catálogo) sembrado.");
      } else {
        product1 = productRepository.findAll().get(0);
      }

      // 4. Asignar el Stock Físico del Producto a la Ubicación
      if (stockLevelRepository.count() == 0 && pasilloA != null) {
        StockLevel stockLevel = new StockLevel();
        stockLevel.setProduct(product1);
        stockLevel.setLocation(pasilloA);
        stockLevel.setCurrentStock(new BigDecimal("150.00")); // Aquí declaramos que hay 150 metros
        stockLevel.setPendingStock(new BigDecimal("0.00")); // Inicialmente no hay stock pendiente
        stockLevelRepository.save(stockLevel);
        System.out.println("✅ Nivel de Stock sembrado. ¡Todo listo!");
      }

      if (userRepository.count() == 0) {
        User admin = new User();
        admin.setName("admin");
        admin.setPassword("admin123"); // En producción, asegúrate de encriptar
        admin.setRole(UserRole.ADMIN);
        admin.setArea(RequestingArea.PR);
        userRepository.save(admin);
        System.out.println("✅ Admin sembrado.");
      }
    };
  }
}
