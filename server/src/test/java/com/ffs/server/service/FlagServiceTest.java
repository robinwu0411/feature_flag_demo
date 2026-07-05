package com.ffs.server.service;

import com.ffs.server.TestConfig;
import com.ffs.server.mapper.ApplicationMapper;
import com.ffs.server.model.dto.CreateFlagRequest;
import com.ffs.server.model.dto.CreateRuleRequest;
import com.ffs.server.model.dto.CreateRolloutRequest;
import com.ffs.server.model.entity.*;
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

    @Autowired
    private ApplicationMapper applicationMapper;

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
        req.setCreatedBy("tester");
        Flag flag = flagService.create(req);
        flagId = flag.getId();
    }

    @Test
    void shouldCreateFlag() {
        Flag found = flagService.getById(flagId);
        assertThat(found.getKey()).isEqualTo("test_flag");
        assertThat(found.getFlagType()).isEqualTo("BOOLEAN");
    }

    @Test
    void shouldAddTargetingRule() {
        CreateRuleRequest req = new CreateRuleRequest();
        req.setPriority(1);
        req.setAttribute("region");
        req.setOperator("EQUALS");
        req.setValue("\"eu-west\"");
        req.setServeValue("true");

        TargetingRule rule = flagService.addRule(flagId, req);
        assertThat(rule.getId()).isNotNull();
        assertThat(rule.getFlagId()).isEqualTo(flagId);
    }

    @Test
    void shouldGetRulesOrderedByPriority() {
        CreateRuleRequest r1 = new CreateRuleRequest();
        r1.setPriority(2); r1.setAttribute("plan"); r1.setOperator("EQUALS");
        r1.setValue("\"premium\""); r1.setServeValue("true");
        flagService.addRule(flagId, r1);

        CreateRuleRequest r2 = new CreateRuleRequest();
        r2.setPriority(1); r2.setAttribute("region"); r2.setOperator("EQUALS");
        r2.setValue("\"eu-west\""); r2.setServeValue("true");
        flagService.addRule(flagId, r2);

        List<TargetingRule> rules = flagService.getRules(flagId);
        assertThat(rules).hasSize(2);
        assertThat(rules.get(0).getPriority()).isEqualTo(1);
    }

    @Test
    void shouldSetRollout() {
        CreateRolloutRequest req = new CreateRolloutRequest();
        req.setPercentage(30);
        req.setServeValue("true");
        flagService.setRollout(flagId, req);

        List<Rollout> rollouts = flagService.getRollouts(flagId);
        assertThat(rollouts).hasSize(1);
        assertThat(rollouts.get(0).getPercentage()).isEqualTo(30);
    }
}
