package com.getian.hooks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-10
 */

public class HookManager {
    private final Map<String, List<Hook>> hooks = new LinkedHashMap<>();

    public HookManager register(String event, Hook hook) {
        //如果key存在，返回value；key不存在，执行对应的func
        hooks.computeIfAbsent(event, key -> new ArrayList<>()).add(hook);
        return this;
    }

    public HookDecision trigger(String event, HookContext hookContext) {
        List<Hook> hooks = this.hooks.get(event);
        if (hooks == null || hooks.isEmpty()) {
            return HookDecision.pass();
        }
        for (Hook hook : hooks) {
            HookDecision res = hook.execute(hookContext);
            if (res != null && res.isBlocked()) {
                return res;
            }
        }
        return HookDecision.pass();
    }
}
