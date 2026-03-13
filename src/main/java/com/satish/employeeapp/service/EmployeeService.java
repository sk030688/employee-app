package com.satish.employeeapp.service;

import com.satish.employeeapp.model.Employee;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class EmployeeService {

    private Map<Long, Employee> employees = new HashMap<>();
    private Long nextId = 1L;

    public EmployeeService() {
        employees.put(1L, new Employee(1L, "Satish",
            "DevOps", "satish@company.com", 85000.0));
        employees.put(2L, new Employee(2L, "Rahul",
            "Development", "rahul@company.com", 75000.0));
        employees.put(3L, new Employee(3L, "Priya",
            "Testing", "priya@company.com", 70000.0));
        nextId = 4L;
    }

    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employees.values());
    }

    public Employee getById(Long id) {
        return employees.get(id);
    }

    public Employee createEmployee(Employee emp) {
        emp.setId(nextId++);
        employees.put(emp.getId(), emp);
        return emp;
    }

    public Employee updateEmployee(Long id, Employee emp) {
        emp.setId(id);
        employees.put(id, emp);
        return emp;
    }

    public void deleteEmployee(Long id) {
        employees.remove(id);
    }
}
