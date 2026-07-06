package com.ffs.sample.controller;

import com.ffs.sdk.FeatureFlagClient;
import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private final FeatureFlagClient ffClient;

    public DemoController(FeatureFlagClient ffClient) { this.ffClient = ffClient; }

    @GetMapping("/checkout")
    public Map<String, Object> checkout(
            @RequestParam(defaultValue = "user_1") String userId,
            @RequestParam(defaultValue = "eu-west") String region,
            @RequestParam(defaultValue = "premium") String plan) {
        FFUser user = FFUser.builder().id(userId).region(region).plan(plan).build();
        boolean newCheckout = ffClient.isEnabled("new_checkout_ui", user);
        boolean darkMode = ffClient.isEnabled("dark_mode", user);
        int maxResults = ffClient.intValue("max_search_results", user, 20);

        Map<String, Object> resp = new HashMap<>();
        resp.put("user_id", userId); resp.put("region", region); resp.put("plan", plan);
        resp.put("new_checkout_ui", newCheckout);
        resp.put("dark_mode", darkMode);
        resp.put("max_search_results", maxResults);

        log.info("Checkout [{}, {}, {}]: new_checkout_ui={}, dark_mode={}, max_search_results={}",
                userId, region, plan, newCheckout, darkMode, maxResults);

        Map<String, EvalResult> details = ffClient.evaluateAll(user,
                "new_checkout_ui", "dark_mode", "max_search_results");
        resp.put("explainability", details);

        for (var entry : details.entrySet()) {
            EvalResult r = entry.getValue();
            log.info("Explain [{}]: value={}, reason={}, userId={}, region={}, release={}",
                    r.getFlagKey(), r.getValue(), r.getReason(),
                    r.getUserId(), r.getRegion(), r.getReleaseVersion());
        }
        return resp;
    }

    @GetMapping("/explain/{flagKey}")
    public Map<String, Object> explain(@PathVariable String flagKey,
            @RequestParam(defaultValue = "user_1") String userId,
            @RequestParam(defaultValue = "eu-west") String region,
            @RequestParam(defaultValue = "premium") String plan) {
        FFUser user = FFUser.builder().id(userId).region(region).plan(plan).build();
        Map<String, EvalResult> results = ffClient.evaluateAll(user, flagKey);
        EvalResult result = results.get(flagKey);
        Map<String, Object> resp = new HashMap<>();
        if (result != null) {
            resp.put("flag", result.getFlagKey()); resp.put("value", result.getValue());
            resp.put("reason", result.getReason()); resp.put("trace", result.getTrace());
            resp.put("releaseVersion", result.getReleaseVersion());
            resp.put("userId", result.getUserId());
            resp.put("region", result.getRegion());
        } else { resp.put("error", "Flag not found"); }
        return resp;
    }
}
