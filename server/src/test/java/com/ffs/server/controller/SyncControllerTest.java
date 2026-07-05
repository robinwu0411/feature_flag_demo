package com.ffs.server.controller;

import com.ffs.server.TestConfig;
import com.ffs.server.mapper.*;
import com.ffs.server.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private TargetingRuleMapper ruleMapper;

    @BeforeEach
    void setUp() {
        Application app = new Application("test-app", "Test");
        applicationMapper.insert(app);

        Flag flag = new Flag();
        flag.setKey("feature_x");
        flag.setName("Feature X");
        flag.setFlagType("BOOLEAN");
        flag.setDefaultValue("false");
        flag.setEnabled(true);
        flagMapper.insert(flag);

        flagMapper.insertFlagApplication(flag.getId(), app.getId());

        TargetingRule rule = new TargetingRule();
        rule.setFlagId(flag.getId());
        rule.setPriority(1);
        rule.setAttribute("region");
        rule.setOperator("EQUALS");
        rule.setValue("\"eu-west\"");
        rule.setServeValue("true");
        rule.setEnabled(true);
        ruleMapper.insert(rule);
    }

    @Test
    void shouldSyncAllFlagsForApp() throws Exception {
        mockMvc.perform(get("/api/v1/eval/sync").param("appId", "test-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.length()").value(1))
                .andExpect(jsonPath("$.flags[0].key").value("feature_x"))
                .andExpect(jsonPath("$.flags[0].rules[0].attribute").value("region"));
    }

    @Test
    void shouldReturnErrorForUnknownApp() {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/api/v1/eval/sync").param("appId", "nonexistent"));
        });
    }
}
