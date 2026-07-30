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
    private final MessageBus bus;
    private final AtomicInteger sequence = new AtomicInteger(0);
    private final Map<String, ProtocolState> pendingRequests = new ConcurrentHashMap<>();
    private final String LEAD = "lead";

    public ProtocolService(MessageBus bus) {
        this.bus = bus;
    }

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
    public String requestPlan(String teammate, String task) throws Exception {
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

    public List<TeamMessage> consumeLeadInBox() {
        List<TeamMessage> teamMessages = bus.read(LEAD);
        for (TeamMessage message : teamMessages) {
            JSONObject metadata = message.getMetadata();
            if (metadata == null) {
                continue;
            }
            String requestId = metadata.getString("request_id");
            String messageType = message.getType();
            if (requestId != null && messageType != null && messageType.endsWith("_response")) {
                matchResponse(requestId, messageType, metadata.getBooleanValue("approve"));
            }
        }
        return teamMessages;
    }

    /**
     * 该方法目前只适用于处理：shutdown 事件的
     */
    private void matchResponse(String requestId, String messageType, boolean approve) {
        ProtocolState state = pendingRequests.get(requestId);
        if (state == null) {
            System.out.println("  [protocol] unknown request_id: " + requestId);
            return;
        }
        String protocolType = state.getType();
        if ("plan_approval".equals(protocolType) && !"plan_approval_response".equals(messageType)) {
            System.out.println("  [protocol] type mismatch: expected plan_approval_response, got "
                    + messageType);
            return;
        }
        if("shutdown".equals(protocolType) && !"shutdown_response".equals(messageType)){
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

    public boolean isProtocolMessage(TeamMessage message) {
        String messageType = message.getType();
        return "shutdown_request".equals(messageType) || "plan_approval_response".equals(messageType);
    }

    public boolean handleTeammateProtocolMessage(String name, TeamMessage message, List<Message> messages) {
        String type = message.getType();
        JSONObject metadata = message.getMetadata();
        String requestId = metadata == null ? "" : metadata.getString("request_id");
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
