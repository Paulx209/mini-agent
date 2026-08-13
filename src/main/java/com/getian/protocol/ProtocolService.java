package com.getian.protocol;

import com.alibaba.fastjson.JSONObject;
import com.getian.core.Message;
import com.getian.team.MessageBus;
import com.getian.team.TeamMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-29
 */

public class ProtocolService {
    private static final int DEFAULT_EXTENSION_TURNS = 10;
    private final MessageBus bus;
    private final AtomicInteger sequence = new AtomicInteger(0); //用来构造 ProtocolState requestId属性的
    private final Map<String, ProtocolState> pendingRequests = new ConcurrentHashMap<>();
    private final String LEAD = "lead";

    public ProtocolService(MessageBus bus) {
        this.bus = bus;
    }

    /**
     * MainAgent 指定某个subAgent 关机 | 创建ProtocolState 存map send消息
     */
    public String requestShutdown(String teammate){
        String requestId = requestId();
        JSONObject metadata = metadata(requestId);
        String content = "Please shut down gracefully.";
        ProtocolState state = new ProtocolState(requestId,"shutdown",LEAD,teammate,"pending","",System.currentTimeMillis());
        pendingRequests.put(requestId,state);
        bus.send(LEAD,teammate,content,"shutdown_request",metadata);
        System.out.println("  [protocol] shutdown_request -> " + teammate
                + " (" + requestId + ")");
        return "Shutdown request sent to " + teammate + " (req: " + requestId + ")";
    }

    /**
     * mainAgent -> subAgent 让subAgent创建Plan 不属于protocolState的范畴
     */
    public String requestPlan(String teammate, String task){
        bus.send(LEAD, teammate, "Please submit a plan for: " + task, "message");
        return "Asked " + teammate + " to submit a plan";
    }

    /**
     * subAgent -> mainAgent subAgent提交plan  plan_approval_request
     */
    public String submitPlan(String fromName, String plan) {
        String requestId = requestId();
        pendingRequests.put(requestId, new ProtocolState(requestId, "plan_approval",
                fromName, LEAD, "pending", plan, System.currentTimeMillis()));
        bus.send(fromName, LEAD, plan, "plan_approval_request", metadata(requestId));
        return "Plan submitted (" + requestId + "). Waiting for approval...";
    }

    /**
     * mainAgent -> subAgent mainAgent 检查 plan  plan_approval_response
     */
    public String reviewPlan(String requestId, boolean approve, String feedback) {
        ProtocolState state = pendingRequests.get(requestId);
        if (state == null) {
            return "Request " + requestId + " not found";
        }
        if (!"pending".equals(state.getStatus())) {
            return "Request " + requestId + " already " + state.getStatus();
        }
        state.setStatus(approve ? "approved" : "rejected");
        JSONObject metadata = metadata(requestId);
        metadata.put("approve", approve);
        String content = feedback == null || feedback.isBlank()
                ? (approve ? "Approved" : "Rejected")
                : feedback;
        bus.send(LEAD, state.getSender(), content,
                "plan_approval_response", metadata);
        System.out.println("  [protocol] plan " + (approve ? "approved" : "rejected")
                + " (" + requestId + ")");
        return "Plan " + (approve ? "approved" : "rejected") + " (" + requestId + ")";
    }

    public String requestTurnExtension(String agentName, String taskId,
                                       String progress, String remainingWork,
                                       String reason) {
        String requestId = requestId();
        JSONObject requestMetadata = metadata(requestId)
                .fluentPut("task_id", emptyIfNull(taskId))
                .fluentPut("progress", emptyIfNull(progress))
                .fluentPut("remaining_work", emptyIfNull(remainingWork))
                .fluentPut("reason", emptyIfNull(reason))
                .fluentPut("requested_turns", DEFAULT_EXTENSION_TURNS);
        pendingRequests.put(requestId, new ProtocolState(
                requestId, "turn_extension", agentName, LEAD, "pending",
                emptyIfNull(remainingWork), System.currentTimeMillis()));
        bus.send(agentName, LEAD,
                "Turn limit reached. Requesting additional turns.",
                "turn_extension_request", requestMetadata);
        return requestId;
    }

    public String reviewTurnExtension(String requestId, boolean approve,
                                      int additionalTurns, String feedback) {
        ProtocolState state = pendingRequests.get(requestId);
        if (state == null) {
            return "Request " + requestId + " not found";
        }
        if (!"turn_extension".equals(state.getType())) {
            return "Request " + requestId + " is not a turn extension request";
        }
        if (!"pending".equals(state.getStatus())) {
            return "Request " + requestId + " already " + state.getStatus();
        }
        state.setStatus(approve ? "approved" : "rejected");
        int grantedTurns = approve ? Math.max(1, additionalTurns) : 0;
        JSONObject responseMetadata = metadata(requestId)
                .fluentPut("approve", approve)
                .fluentPut("additional_turns", grantedTurns);
        String content = feedback == null || feedback.isBlank()
                ? (approve ? "Additional turns approved." : "Additional turns rejected.")
                : feedback;
        bus.send(LEAD, state.getSender(), content,
                "turn_extension_response", responseMetadata);
        return approve
                ? "Approved " + grantedTurns + " additional turns (" + requestId + ")"
                : "Turn extension rejected (" + requestId + ")";
    }

    /**
     * mainAgent 消费 mailbox 中的消息
     */
    public List<TeamMessage> consumeLeadInBox() {
        List<TeamMessage> teamMessages = bus.read(LEAD);
        for (TeamMessage message : teamMessages) {
            JSONObject metadata = message.getMetadata();
            if (metadata == null) {
                continue;
            }
            String requestId = metadata.getString("request_id");
            String messageType = message.getType();
            // 只有 shutdown_response 的messageType需要处理 state#status属性
            if (requestId != null && messageType != null && messageType.endsWith("_response")) {
                matchResponse(requestId, messageType, metadata.getBooleanValue("approve"));
            }
        }
        return teamMessages;
    }

    /**
     * 处理当前requestId 对应的 protocolState
     * 该方法目前只适用于处理：shutdown 事件的
     */
    private void matchResponse(String requestId, String messageType, boolean approve) {
        ProtocolState state = pendingRequests.get(requestId);
        if (state == null) {
            System.out.println("  [protocol] unknown request_id: " + requestId);
            return;
        }
        String protocolType = state.getType();
        if("shutdown".equals(protocolType) && !"shutdown_response".equals(messageType)){
            System.out.println("  [protocol] type mismatch: expected shutdown_response, got "
                    + messageType);
            return;
        }
        // always false
        if("plan_approval".equals(protocolType) && !"plan_approval_response".equals(messageType)){
            System.out.println("  [protocol] type mismatch: expected shutdown_response, got "
                    + messageType);
            return;
        }

        if(!"pending".equals(state.getStatus())){
            System.out.println("  [protocol] " + requestId + " already "
                    + state.getStatus() + ", ignoring duplicate");
            return;
        }

        state.setStatus(approve ? "approved" : "rejected");
        System.out.println("  [protocol] " + state.getType() + " "
                + state.getStatus() + " (" + requestId + ")");
    }

    private JSONObject metadata(String requestId) {
        return new JSONObject()
                .fluentPut("request_id", requestId);
    }

    private String requestId() {
        return String.format("req_%06d", sequence.incrementAndGet());
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    public boolean isTeammateProtocolMessage(TeamMessage message) {
        String messageType = message.getType();
        return "shutdown_request".equals(messageType)
                || "plan_approval_response".equals(messageType)
                || "turn_extension_response".equals(messageType);
    }

    public boolean isProtocolMessage(TeamMessage message){
        String messageType = message.getType();
        boolean teammateProtocolMessage = isTeammateProtocolMessage(message);
        if(!teammateProtocolMessage){
            return "shutdown_response".equals(messageType)
                    || "plan_approval_request".equals(messageType)
                    || "turn_extension_request".equals(messageType);
        }
        return true;
    }

    /**
     * 处理协议消息 : shutdown_request  / plan_approval_response
     */
    public boolean handleTeammateProtocolMessage(String name, TeamMessage message, List<Message> messages) {
        String type = message.getType();
        JSONObject metadata = message.getMetadata();
        String requestId = metadata == null ? "" : metadata.getString("request_id");
        //准备shutdown
        if("shutdown_request".equals(type)){
            JSONObject responseMeta = metadata(requestId);
            responseMeta.fluentPut("approve",true);
            bus.send(name,LEAD,"Shutting down gracefully.","shutdown_response",responseMeta);
            System.out.println("  [protocol] " + name
                    + " approved shutdown (" + requestId + ")");
            return true;
        }
        if("plan_approval_response".equals(type)){
            boolean approve = metadata != null && metadata.getBooleanValue("approve");
            String text = approve
                    ? "[Plan approved] Proceed with the task."
                    : "[Plan rejected] Feedback: " + message.getContent();
            messages.add(Message.user(text));
        }
        return false;
    }
}
