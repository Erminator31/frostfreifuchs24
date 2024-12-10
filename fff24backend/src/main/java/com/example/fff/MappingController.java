package com.example.fff;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Allow cross-origin requests from any source
@CrossOrigin(origins = "*", allowedHeaders = "*")
// Indicates that the class is a REST controller
@RestController
// Base path for all endpoints in this controller
@RequestMapping("/api/v1.0")
public class MappingController {

}
