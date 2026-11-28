package com.unip.api.sector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SectorControllerTest {

    @Autowired MockMvc mvc;

    @Test
    @WithUserDetails("employee1@demo.com")
    void authenticatedUserListsSectors() throws Exception {
        mvc.perform(get("/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mvc.perform(get("/sectors")).andExpect(status().isUnauthorized());
    }
}