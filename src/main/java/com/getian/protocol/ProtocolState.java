package com.getian.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolState {
    private String requestId;

    private String type;

    private String sender;

    private String target;

    private String status;

    private String payload;

    private long createdAt;
}
