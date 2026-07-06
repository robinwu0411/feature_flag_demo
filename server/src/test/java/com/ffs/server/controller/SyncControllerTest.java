package com.ffs.server.controller;

import com.ffs.server.TestConfig;
import com.ffs.server.mapper.FlagMapper;
import com.ffs.server.model.entity.Flag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlagMapper flagMapper;

    @BeforeEach
    void setUp() {
        Flag flag = new Flag();
        flag.setKey("feature_x");
        flag.setName("Feature X");
        flag.setFlagType("BOOLEAN");
        flag.setDefaultValue("false");
        flag.setEnabled(true);
        flag.setAppName("test-app");
        flagMapper.insert(flag);
    }

    @Test
    void shouldSyncAllFlagsForApp() throws Exception {
        mockMvc.perform(get("/api/v1/eval/sync").param("appId", "test-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.length()").value(1))
                .andExpect(jsonPath("$.flags[0].key").value("feature_x"));
    }

    @Test
    void shouldReturnEmptyForOtherApp() throws Exception {
        mockMvc.perform(get("/api/v1/eval/sync").param("appId", "other-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.length()").value(0));
    }
}
