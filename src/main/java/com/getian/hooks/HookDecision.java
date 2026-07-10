package com.getian.hooks;

import lombok.Data;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-10
 */

@Data
public class HookDecision {
    private boolean blocked;
    private String message;

    public HookDecision(boolean blocked,String message){
        this.blocked = blocked;
        this.message = message;
    }

    public static HookDecision pass(){
        return new HookDecision(false,null);
    }

    public static HookDecision deny(String rejectReason){
        return new HookDecision(true,rejectReason);
    }

}
