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
  @Transactional
  CommandLineRunner initDatabase(
    SupplierRepository supplierRepository,
    WarehouseRepository warehouseRepository,
    ProductRepository productRepository,
    StockLevelRepository stockLevelRepository,
    UserRepository userRepository,
    RequestingAreaRepository areaRepository
  ) {
    return args -> {
      
      // 1. Crear Área Solicitante (Requisito para el Usuario)
      RequestingArea adminArea;
      if (areaRepository.count() == 0) {
          adminArea = new RequestingArea();
          adminArea.setName("Departamento de Almacén");
          adminArea.setDescription("Gestión de inventarios y logística");
          adminArea.setCostCenter("CC-ALM-01");
          adminArea.setActive(true);
          adminArea = areaRepository.save(adminArea);
          System.out.println("✅ Área sembrada.");
      } else {
          adminArea = areaRepository.findAll().get(0);
      }

      // 2. Crear Proveedor (Con código real de SAP)
      Supplier mainSupplier;
      if (supplierRepository.count() == 0) {
        Supplier newSupplier = new Supplier();
        newSupplier.setName("Proveedor ZSUM"); // Asignado genérico por el Tipo Mat en tu tabla
        newSupplier.setSapCode("20005240"); // Mapeado de tu columna "PROVEEDOR"
        newSupplier.setAddress("Sin dirección registrada");
        newSupplier.setEmail("contacto@proveedor.com");
        newSupplier.setPhone("0000-0000000");
        newSupplier.setDescription("Importado desde matriz SAP");
        newSupplier.setActive(true);
        mainSupplier = supplierRepository.save(newSupplier);
        System.out.println("✅ Proveedor SAP sembrado.");
      } else {
        mainSupplier = supplierRepository.findAll().get(0);
      }

      // 3. Crear Almacén y su Ubicación (Con código de Centro SAP)
      Warehouse mainWarehouse;
      Location pasilloA = null;
      if (warehouseRepository.count() == 0) {
        mainWarehouse = Warehouse.builder()
          .name("Centro SAP CP20")
          .sapCenterCode("CP20") // Mapeado de tu columna "Centro"
          .physicalAddress("Planta Visco Orinoco")
          .description("Almacén principal sincronizado")
          .build();

        pasilloA = new Location();
        pasilloA.setAisle("A"); 
        pasilloA.setShelf("1"); 
        pasilloA.setLocationCode("CP20-A-1");
        pasilloA.setWarehouse(mainWarehouse); 

        mainWarehouse.getStorageLocations().add(pasilloA);

        mainWarehouse = warehouseRepository.save(mainWarehouse);
        pasilloA = mainWarehouse.getStorageLocations().iterator().next();
        System.out.println("✅ Almacén SAP sembrado.");
      }

      // 4. Crear Producto en el Catálogo (Mapeo exacto de tu fila de Excel)
      Product product1;
      if (productRepository.count() == 0) {
        product1 = Product.builder()
          .internalCode("VIS-CP20-001")
          .sku("10000250") // Mapeado de "Pieza Fabricante"
          .sapCode("4000000000") // Mapeado de "CODIGO"
          .name("CEMENTO ESPECIAL 250 CC PARA PARCHES") // Mapeado de "DESCRIPCION"
          .description("PEGA PARA PARCHES DE NEUMATICOS CEMENTO SPECIAL 250CC (REF.515-0340).COD.SIM: 010000250") // Mapeado de "TEXTO LARGO MATERIAL"
          .uom(Uom.UNIDAD) // Asumiendo que agregaste 'UNIDAD' o 'EA' a tu Enum
          .reorderPoint(new BigDecimal("5.00")) // Límite de alerta
          .active(true)
          .supplier(mainSupplier)
          .build();

        product1 = productRepository.save(product1);
        System.out.println("✅ Material SAP sembrado.");
      } else {
        product1 = productRepository.findAll().get(0);
      }

      // 5. Asignar el Stock Físico Total
      if (stockLevelRepository.count() == 0 && pasilloA != null) {
        StockLevel stockLevel = new StockLevel();
        stockLevel.setProduct(product1);
        stockLevel.setLocation(pasilloA);
        stockLevel.setCurrentStock(new BigDecimal("9.00")); // Mapeado exacto de tu columna "Stock total"
        stockLevel.setPendingStock(new BigDecimal("0.00")); 
        stockLevelRepository.save(stockLevel);
        System.out.println("✅ Stock físico sembrado.");
      }

      // 6. Crear Usuario Admin
      if (userRepository.count() == 0) {
        User admin = new User();
        admin.setName("Administrador Visco");
        admin.setEmail("admin@visco.com"); 
        admin.setPassword("admin123"); 
        admin.setRole(UserRole.ADMIN);
        admin.setArea(adminArea); 
        userRepository.save(admin);
        System.out.println("✅ Configuración inicial completada con éxito.");
      }
    };
  }
}