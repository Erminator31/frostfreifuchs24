    package com.example.fff;

    import com.example.fff.api.OrderManager;
    import com.example.fff.api.ProductManager;
    import com.example.fff.databse.PostgresDBOrderManagement;
    import com.example.fff.databse.PostgresDBProductManagement;
    import model.Order;
    import model.Product;
    import org.springframework.http.HttpStatus;
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
    @RequestMapping("/api")
    public class MappingController {

        ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
        OrderManager orderManager = PostgresDBOrderManagement.getInstance();

        private static final Logger LOGGER = Logger.getLogger(MappingController.class.getName());


        /**
         * Retrieves information based on the provided name.
         *
         * @param name the name parameter for the info retrieval
         * @return a string indicating the success of the operation
         */
        @GetMapping("/auth")
        public String getInfo(@RequestParam(value = "name", defaultValue = "Name") String name) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController auth " + name);
            return "ok";
        }


        /**
         * Method to create a product table. This method logs the action, checks for token authentication, and then calls the createProductTable method in the ProductManager.
         *
         * @return a string "ok" indicating the success of the operation
         * @throws Exception if an error occurs during table creation
         */
        @GetMapping("/create-products-table")
        public String createProductTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-product-table ");

            // Check token

            productManager.createProductTable();

            return "ok";
        }
        @GetMapping("/create-orders-table")
        public String createOrdersTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-orders-table ");

            // Check token

            orderManager.createOrderTable();

            return "ok";
        }

        @GetMapping("/create-orderitems-table")
        public String createOrderItemsTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-orders-table ");

            // Check token

            orderManager.createOrderItemTable();

            return "ok";
        }

        /**
         * Adds a new product to the system.
         *
         * @param product the Product object to be added
         * @return ResponseEntity containing a success message upon successful addition of the product
         */
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

        /**
         * Retrieves a list of products based on the provided product name and type.
         *
         * @param productName the name of the product to filter by (optional)
         * @param productType the type of the product to filter by (optional)
         * @return ResponseEntity<List < Product>> containing the list of products meeting the specified criteria
         */
        @GetMapping("/products")
        public ResponseEntity<List<Product>> getProducts(
                @RequestParam(value = "productName", required = false) String productName,
                @RequestParam(value = "productType", required = false) String productType) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController /api/inventory");

            // Produkte gefiltert aus der DB laden
            List<Product> products = productManager.readProducts(productName, productType);

            if (products.isEmpty()) {
                // Wenn keine Produkte vorhanden, 204 No Content zurückgeben
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(products);
        }

        /**
         * Removes a product from the system based on the provided product ID.
         *
         * @param productId the ID of the product to be removed
         * @return ResponseEntity containing a success message if the product is removed successfully,
         *         ResponseEntity with HTTP status 404 if the product is not found,
         *         or ResponseEntity with HTTP status 500 if an error occurs during the removal process
         */
        @DeleteMapping("/product/delete/{id}")
        public ResponseEntity<?> removeProduct(@PathVariable("id") int productId) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController DELETE /product/" + productId);
            try {
                boolean removed = productManager.removeProduct(productId);
                if (removed) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Product with ID " + productId + " removed successfully.");
                    return ResponseEntity.ok(response);
                } else {

                    return ResponseEntity.notFound().build();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing product: " + e.getMessage());
            }
        }

        @GetMapping("/delete-products-table")
        public ResponseEntity<String> deleteProductsTable() {
            try {
                productManager.deleteProductsTable();
                return ResponseEntity.ok("Products table deleted successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting products table: " + e.getMessage());
            }
        }
        @GetMapping("/delete-orders-table")
        public ResponseEntity<String> deleteOrderTable() {
            try {
                orderManager.deleteOrderTable();
                return ResponseEntity.ok("Products table order successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting order table: " + e.getMessage());
            }
        }

        @GetMapping("/delete-orders-table")
        public ResponseEntity<String> deleteOrderItemsTable() {
            try {
                orderManager.deleteOrderItemsTable();
                return ResponseEntity.ok("orderitems table order successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting orderitems table: " + e.getMessage());
            }
        }


        /**
         * Bestell-Endpoint: Hier kann eine Bestellung angelegt werden.
         * Der Request-Body enthält den Kundenname und die bestellten Items.
         */
        @PostMapping("/order")
        public ResponseEntity<?> createOrder(@RequestBody Order orderRequest) {
            LOGGER.log(Level.INFO, "Creating order for customer: " + orderRequest.getCustomerName());

            try {
                Order createdOrder = orderManager.createOrder(orderRequest.getCustomerName(), orderRequest.getItems());
                return ResponseEntity.ok(createdOrder);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Could not create order: " + e.getMessage());
            }
        }

        /**
         * Einzelne Bestellung abrufen
         */
        @GetMapping("/order/{id}")
        public ResponseEntity<?> getOrder(@PathVariable("id") int orderId) {
            try {
                Order order = orderManager.getOrder(orderId);
                if (order == null) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(order);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error retrieving order: " + e.getMessage());
            }
        }

        /**
         * Alle Bestellungen abrufen
         */
        @GetMapping("/orders")
        public ResponseEntity<?> getAllOrders() {
            try {
                List<Order> orders = orderManager.getAllOrders();
                if (orders.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.ok(orders);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error retrieving orders: " + e.getMessage());
            }
        }

    }



