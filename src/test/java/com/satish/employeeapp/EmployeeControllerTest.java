package com.satish.employeeapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetAllEmployees() throws Exception {
        mockMvc.perform(get("/api/employees"))
               .andExpect(status().isOk());
    }

    @Test
    public void testGetEmployeeById() throws Exception {
        mockMvc.perform(get("/api/employees/1"))
               .andExpect(status().isOk());
    }

    @Test
    public void testAppInfo() throws Exception {
        mockMvc.perform(get("/api/employees/info"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.application")
               .value("Employee Management App"));
    }
}
