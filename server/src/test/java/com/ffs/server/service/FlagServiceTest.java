package com.ffs.server.service;

import com.ffs.server.TestConfig;
import com.ffs.server.model.dto.CreateFlagRequest;
import com.ffs.server.model.entity.Flag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FlagServiceTest {

    @Autowired
    private FlagService flagService;

    private Long flagId;

    @BeforeEach
    void setUp() {
        CreateFlagRequest req = new CreateFlagRequest();
        req.setKey("test_flag");
        req.setName("Test Flag");
        req.setFlagType("BOOLEAN");
        req.setDefaultValue("false");
        req.setEnabled(true);
        req.setReleaseVersion("v1.0.0");
        req.setAppName("test-app");
        req.setCreatedBy("tester");
        Flag flag = flagService.create(req);
        flagId = flag.getId();
    }

    @Test
    void shouldCreateFlag() {
        Flag found = flagService.getById(flagId);
        assertThat(found.getKey()).isEqualTo("test_flag");
        assertThat(found.getFlagType()).isEqualTo("BOOLEAN");
        assertThat(found.getAppName()).isEqualTo("test-app");
    }

    @Test
    void shouldListAllFlags() {
        List<Flag> flags = flagService.listAll();
        assertThat(flags).isNotEmpty();
    }

    @Test
    void shouldUpdateFlag() {
        CreateFlagRequest req = new CreateFlagRequest();
        req.setName("Updated Flag");
        req.setEnabled(false);
        Flag updated = flagService.update(flagId, req);
        assertThat(updated.getName()).isEqualTo("Updated Flag");
        assertThat(updated.getEnabled()).isFalse();
    }

    @Test
    void shouldDeleteFlag() {
        flagService.delete(flagId);
        try {
            flagService.getById(flagId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }
}
