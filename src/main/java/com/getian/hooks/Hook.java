package com.getian.hooks;

public interface Hook {
    //钩子函数执行
    HookDecision execute(HookContext context);
}
