package com.ffs.sample;

import com.ffs.sdk.FeatureFlagClient;
import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DemoRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);
    private final FeatureFlagClient ffClient;

    public DemoRunner(FeatureFlagClient ffClient) {
        this.ffClient = ffClient;
    }

    @Override
    public void run(String... args) throws Exception {
        // give SDK time to complete initial sync
        Thread.sleep(3000);

        FFUser user = FFUser.builder()
                .id("user_1")
                .region("eu-west")
                .plan("premium")
                .build();

        log.info("========== SDK Evaluation Demo ==========");

        // boolean flag
        boolean enabled = ffClient.isEnabled("new_checkout_ui", user);
        log.info("[new_checkout_ui] isEnabled={}", enabled);

        // boolean flag
        boolean dark = ffClient.isEnabled("dark_mode", user);
        log.info("[dark_mode] isEnabled={}", dark);

        // number flag
        int max = ffClient.intValue("max_search_results", user, 20);
        log.info("[max_search_results] intValue={}", max);

        // explainability
        Map<String, EvalResult> results = ffClient.evaluateAll(user,
                "new_checkout_ui", "dark_mode", "max_search_results");
        for (Map.Entry<String, EvalResult> entry : results.entrySet()) {
            EvalResult r = entry.getValue();
            log.info("Explain [{}]: value={}, reason={}, userId={}, region={}, release={}, trace={}",
                    r.getFlagKey(), r.getValue(), r.getReason(),
                    r.getUserId(), r.getRegion(), r.getReleaseVersion(), r.getTrace());
        }

        log.info("==========================================");
    }
}
