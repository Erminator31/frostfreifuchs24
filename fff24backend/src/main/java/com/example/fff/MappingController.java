    package com.example.fff;

    import com.example.fff.api.ProductManager;
    import com.example.fff.databse.PostgresDBProductManagement;
    import model.Product;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.util.*;
    import java.util.function.Predicate;
    import java.util.logging.Level;
    import java.util.logging.Logger;


    // Allow cross-origin requests from any source
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    // Indicates that the class is a REST controller
    @RestController
    // Base path for all endpoints in this controller
    @RequestMapping("/api/v1.0")
    public class MappingController {

        ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();

        private static final Logger LOGGER = Logger.getLogger(MappingController.class.getName());

        /**
         * Provides a simple authentication check by returning "ok".
         *
         * @param name A sample name parameter for testing.
         * @return A string response "ok".
         */
        @GetMapping("/auth")
        public String getInfo(@RequestParam(value = "name", defaultValue = "Name") String name) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController auth " + name);
            return "ok";
        }


        @GetMapping("/create-user-table")
        public String creatProductTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-product-table ");

            // Check token

            productManager.createProductTable();

            return "ok";
        }

        @PostMapping(path = "/product/add", consumes = {MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE})
        public ResponseEntity<?> addProduct(@RequestBody Product product) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController POST /products/add " + product.getProductName());
            try {
                productManager.addProduct(
                        product.getProductName(),
                        product.getProductType()
                );

                Map<String, String> response = new HashMap<>();
                response.put("message", "Product " + product.getProductName() + " added successfully.");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                ResponseEntity.badRequest();
            }
            return null;
        }

        @GetMapping("/api/inventory")
        public ResponseEntity<List<Product>> getAllProducts() {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController /users/all");

            List<Product> productsFromFile = productManager.readAllProducts();
            List<Product> myProducts = new ArrayList<>();

            for (Product p : productsFromFile) {
                myProducts.add(new Product(p.getProductId(), p.getProductName(), p.getProductType()));
            }

            if (myProducts.isEmpty()) {
                // Falls keine Produkte vorhanden sind, könnte man z. B. einen 204 zurückgeben
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(myProducts);
        }

    }
