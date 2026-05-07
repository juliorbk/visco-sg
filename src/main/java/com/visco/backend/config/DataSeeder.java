package com.visco.backend.config;

import com.visco.backend.models.entities.*;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.repositories.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final RequestingAreaRepository areaRepository;
  private final SupplierRepository supplierRepository;
  private final WarehouseRepository warehouseRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final StockLevelRepository stockLevelRepository;
  private final UserRepository userRepository;
  private final PurchaseOrderRepository orderRepository;

  @Override
  @Transactional
  public void run(String... args) {
    if (areaRepository.count() > 0) return; // Evita duplicados

    // 1. ÁREAS SOLICITANTES (10 registros)
    List<RequestingArea> areas = new ArrayList<>();
    String[] areaNames = {
      "Mantenimiento Mecánico",
      "Electricidad",
      "Operaciones de Planta",
      "Seguridad Industrial",
      "Logística",
      "Recursos Humanos",
      "Finanzas",
      "Procura",
      "Ingeniería",
      "Control de Calidad",
    };
    for (int i = 0; i < 10; i++) {
      RequestingArea area = new RequestingArea();
      area.setName(areaNames[i]);
      area.setDescription("Gestión operativa de " + areaNames[i]);
      area.setCostCenter("CC-VISCO-" + (100 + i));
      area.setActive(true);
      areas.add(areaRepository.save(area));
    }

    // 2. PROVEEDORES (10 registros)
    List<Supplier> suppliers = new ArrayList<>();
    String[] supNames = {
      "Suministros Guayana C.A.",
      "Ferretería Bolívar",
      "Rodamientos del Sur",
      "Metalmecánica Orinoco",
      "Lubricantes Ven",
      "Protección Civil C.A.",
      "Válvulas Guayana",
      "Herramientas S.A.",
      "Químicos Caroní",
      "Motores Orientales",
    };

    // Mezclamos USD, VED y EUR para hacer el ambiente más realista
    Currency[] currencies = {
      Currency.VED,
      Currency.USD,
      Currency.USD,
      Currency.EUR,
      Currency.UYU,
      Currency.VED,
      Currency.EUR,
      Currency.ZAR,
      Currency.VED,
      Currency.USD,
    };

    for (int i = 0; i < 10; i++) {
      Supplier s = new Supplier();
      s.setName(supNames[i]);
      s.setSapCode("V-2000" + i);
      s.setAddress("Zona Industrial Matanzas, Puerto Ordaz");

      // Limpieza robusta para generar un formato de email válido
      String domain = supNames[i].toLowerCase().replaceAll("[ .]", "");
      s.setEmail("contacto" + i + "@" + domain + ".com");

      s.setPhone("0286-99400" + i);
      s.setDescription("Proveedor industrial certificado");
      s.setActive(true);

      // Corrección de sintaxis aquí:
      s.setCurrency(currencies[i]);

      suppliers.add(supplierRepository.save(s));
    }

    // 3. ALMACENES Y UBICACIONES (10 Almacenes)
    List<Warehouse> warehouses = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Warehouse w = Warehouse.builder()
        .name("Almacén " + (i + 1))
        .sapCenterCode("CP" + (20 + i))
        .physicalAddress("Planta Visco Orinoco, Galpón " + i)
        .description("Almacén especializado")
        .active(true)
        .build();

      // Agregar ubicación por defecto a cada almacén
      Location loc = new Location();
      loc.setAisle("A");
      loc.setShelf("1");
      loc.setLocationCode("CP" + (20 + i) + "-A1");
      loc.setWarehouse(w);
      w.setStorageLocations(new HashSet<>(Collections.singletonList(loc)));

      warehouses.add(warehouseRepository.save(w));
    }

    // 4. CATEGORÍAS (10 registros)
    List<Category> categories = new ArrayList<>();
    String[] catNames = {
      "Válvulas",
      "Rodamientos",
      "EPP",
      "Lubricantes",
      "Soldadura",
      "Herramientas",
      "Filtros",
      "Correas",
      "Empacaduras",
      "Cables",
    };
    for (String name : catNames) {
      Category cat = new Category();
      cat.setName(name);
      categories.add(categoryRepository.save(cat));
    }

    // 5. PRODUCTOS (10 registros basados en tu Excel real)
    List<Product> products = new ArrayList<>();
    String[] productNames = {
      "Válvula de Bola 2\"",
      "Rodamiento SKF 6205",
      "Casco de Seguridad",
      "Aceite ISO 68",
      "Electrodos 6013",
      "Guantes de Vaqueta",
      "Filtro de Aire",
      "Correa B-55",
      "Empacadura Neopreno",
      "Cable THW #12",
    };
    for (int i = 0; i < 10; i++) {
      Product p = Product.builder()
        .internalCode("VIS-MAT-" + (1000 + i))
        .sku("SKU-" + (5000 + i))
        .sapCode("40000000" + i)
        .name(productNames[i])
        .description("Insumo industrial para " + categories.get(i).getName())
        .uom(Uom.UNIDAD)
        .reorderPoint(new BigDecimal("10.00"))
        .active(true)
        .supplier(suppliers.get(i))
        .category(categories.get(i))
        .build();
      products.add(productRepository.save(p));
    }

    // 6. STOCK LEVELS (10 registros)
    for (int i = 0; i < 10; i++) {
      StockLevel sl = new StockLevel();
      sl.setProduct(products.get(i));
      sl.setLocation(warehouses.get(i).getStorageLocations().iterator().next());
      sl.setCurrentStock(new BigDecimal("100.00"));
      sl.setPendingStock(new BigDecimal("20.00"));
      stockLevelRepository.save(sl);
    }

    // 7. USUARIOS (10 registros con Roles)
    for (int i = 0; i < 10; i++) {
      User user = User.builder()
        .name("Usuario " + i)
        .email("user" + i + "@visco.com")
        .password("admin123") // En producción usar BCrypt
        .role(i == 0 ? UserRole.ADMIN : UserRole.USER)
        .area(areas.get(i))
        .active(true)
        .build();
      userRepository.save(user);
    }

    // 8. ÓRDENES DE COMPRA (10 registros)
    for (int i = 0; i < 10; i++) {
      PurchaseOrder po = PurchaseOrder.builder()
        .orderNumber("PO-2026-" + (500 + i))
        .description("Reposición de stock " + products.get(i).getName())
        .status(PurchaseOrderStatus.PENDING)
        .supplier(suppliers.get(i))
        .build();

      PurchaseOrderItem item = PurchaseOrderItem.builder()
        .purchaseOrder(po)
        .product(products.get(i))
        .quantity(50)
        .unitPrice(new BigDecimal("25.50"))
        .build();

      po.setItems(new ArrayList<>(Collections.singletonList(item)));
      orderRepository.save(po);
    }

    System.out.println(
      "✅ Base de Datos Sembrada: 80 registros cargados exitosamente."
    );
  }
}
