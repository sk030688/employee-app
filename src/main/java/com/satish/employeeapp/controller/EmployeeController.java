package com.satish.employeeapp.controller;

import com.satish.employeeapp.model.Employee;
import com.satish.employeeapp.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Value("${app.environment:development}")
    private String environment;

    @Value("${app.version:1.0.0}")
    private String version;

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployee(
            @PathVariable Long id) {
        Employee emp = employeeService.getById(id);
        if (emp == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(emp);
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(
            @RequestBody Employee emp) {
        return ResponseEntity.ok(
            employeeService.createEmployee(emp));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee emp) {
        return ResponseEntity.ok(
            employeeService.updateEmployee(id, emp));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("application", "Employee Management App");
        info.put("environment", environment);
        info.put("version", version);
        info.put("developer", "Satish Sabbavarapu");
        info.put("hostname",
            System.getenv().getOrDefault("HOSTNAME", "localhost"));
        return ResponseEntity.ok(info);
    }
}
