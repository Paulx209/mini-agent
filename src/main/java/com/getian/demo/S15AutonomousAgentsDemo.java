package com.getian.demo;

import com.getian.background.BackgroundTasks;
import com.getian.core.*;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.protocol.ProtocolService;
import com.getian.task.TaskService;
import com.getian.task.TaskStore;
import com.getian.team.MessageBus;
import com.getian.team.TeamMessage;
import com.getian.tool.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * S15 autonomous agents: idle teammates poll the task board and claim available work.
 */
public class S15AutonomousAgentsDemo {

    private static final String LEAD_SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "create_task, list_tasks, get_task, claim_task, complete_task, "
            + "spawn_teammate, send_message, check_inbox, "
            + "request_shutdown, request_plan, review_plan.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    private static final String TEAMMATE_SYSTEM_PROMPT_TEMPLATE =
            "You are '%s', a %s. Use tools to complete tasks. "
                    + "You can list and claim tasks from the board. "
                    + "Check inbox for protocol messages.\n\n"
                    + "Working directory: %s";


    public static void main(String[] args) {
        AnthropicConfig config = AnthropicClientUtils.defaultAnthropicConfig(LEAD_SYSTEM_PROMPT);

    }
}
