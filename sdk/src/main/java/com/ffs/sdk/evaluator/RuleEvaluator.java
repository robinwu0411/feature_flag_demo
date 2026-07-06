package com.ffs.sdk.evaluator;

import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import com.ffs.sdk.model.FlagConfig;
import com.ffs.sdk.model.TraceEntry;

public class RuleEvaluator {

    public EvalResult evaluate(FlagConfig flag, FFUser user) {
        if (!flag.isEnabled()) {
            return buildResult(flag, user, flag.getDefaultValue(), "disabled");
        }
        String value = "BOOLEAN".equalsIgnoreCase(flag.getType())
                ? "true"
                : flag.getDefaultValue();
        return buildResult(flag, user, value, "enabled");
    }

    private EvalResult buildResult(FlagConfig flag, FFUser user, String value, String reason) {
        EvalResult r = new EvalResult(flag.getKey(), value, reason);
        r.setReleaseVersion(flag.getReleaseVersion());
        r.setFlagUpdatedAt(flag.getUpdatedAt());
        r.setUserId(user != null ? user.getId() : null);
        r.setRegion(user != null ? user.getAttribute("region") : null);
        r.addTrace(new TraceEntry("enabled_check",
                "enabled=" + flag.isEnabled() + " type=" + flag.getType() + " default=" + flag.getDefaultValue(),
                flag.isEnabled()));
        return r;
    }
}
