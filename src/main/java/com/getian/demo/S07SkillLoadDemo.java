package com.getian.demo;

import com.getian.core.*;
import com.getian.hooks.HookManager;
import com.getian.llm.AnthropicLLMClient;
import com.getian.permission.PermissionManager;
import com.getian.skill.SkillRegistry;
import com.getian.tool.LoadSkillTool;
import com.getian.tool.ToolRegistry;
import com.getian.utils.AnthropicClientUtils;
import com.getian.utils.ConfigUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class S07SkillLoadDemo {
    //完整Skills的定义由系统提示词来提供
    private static final String SYSTEM_PROMPT_TEMPLATE = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Skills available:\n%s\nUse load_skill to get full details when needed.";
    private static final File workDir = new File(".");

    public static void main(String[] args) {
        Properties properties = ConfigUtils.loadPropertiesFromResource("config.properties");
        String skillDir = properties.getProperty("skills.dir");
        File file = new File(skillDir);
        SkillRegistry skillRegistry = new SkillRegistry(file);
        String descriptions = skillRegistry.getDescriptions();
        AnthropicLLMClient client = AnthropicClientUtils.createClient(String.format(SYSTEM_PROMPT_TEMPLATE, descriptions));
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(workDir).registry(new LoadSkillTool(skillRegistry));
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        PermissionManager permissionManager = AnthropicClientUtils.createPermissionManager(workDir);
        HookManager hookManager = AnthropicClientUtils.createHookManager(workDir);
        AgentLoop mainAgent = new AgentLoop(client, registry, listener, permissionManager, hookManager);

        List<Message> history = new ArrayList<>();
        Scanner sc = new Scanner(System.in);

        System.out.println("s07: Skill Loading");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        while (true) {
            System.out.print("s07 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }

            history.add(Message.user(query));
            AssistantMessage answer = mainAgent.run(history);
            for (ContentBlock block : answer.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }
}
