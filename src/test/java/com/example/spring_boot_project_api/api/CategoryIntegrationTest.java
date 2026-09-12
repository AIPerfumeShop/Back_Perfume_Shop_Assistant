package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        admin = user(Role.ADMIN, "admin@category.test");
        customer = user(Role.CUSTOMER, "customer@category.test");
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setName(role == Role.ADMIN ? "Admin" : "Customer");
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(name + " description");
        return categoryRepository.save(category);
    }

    @Test
    void searchCategories_returnsActiveCategoriesPaginated() throws Exception {
        category("Floral Perfume");
        category("Woody Perfume");
        category("Citrus Perfume");

        mockMvc.perform(get("/api/categories/search?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void searchCategories_filtersByNameIgnoringCase() throws Exception {
        category("Floral Perfume");
        category("Woody Perfume");

        mockMvc.perform(get("/api/categories/search?search=woody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Woody Perfume"));
    }

    @Test
    void searchCategories_excludesDeactivatedCategories() throws Exception {
        category("Visible");
        Category hidden = category("Hidden");
        mockMvc.perform(delete("/api/categories/" + hidden.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/search?search=hidden"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void searchCategories_isPublic() throws Exception {
        mockMvc.perform(get("/api/categories/search"))
                .andExpect(status().isOk());
    }

    @Test
    void createCategory_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aquatic\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_requiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aquatic\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_asAdmin_createdAndSearchable() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aquatic\",\"description\":\"Oceanic scents\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Aquatic"))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(get("/api/categories/search?search=aqua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Aquatic"));
    }
}