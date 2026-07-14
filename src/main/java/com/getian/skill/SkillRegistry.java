package com.getian.skill;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 技能目录
 * <p>
 * 启动时只把 name/description 这类便宜信息放进 system prompt，
 * 真正的 SKILL.md 正文等模型调用 load_skill 时再加载。
 */
public class SkillRegistry {
    private Map<String, Skill> skillMap = new LinkedHashMap<>();

    public SkillRegistry(File skillDir) {
        //扫描skill目录 进行加载
        scan(skillDir);
    }

    public void scan(File skillDir) {
        //1.列出来所有的目录
        File[] files = skillDir.listFiles(File::isDirectory);
        if (files == null) {
            return;
        }
        //2.遍历没有一个目录，创建SKILL.md文件路径
        for (File dirFile : files) {
            File skillFile = new File(dirFile, "SKILL.md");
            if (!skillFile.isFile()) {
                continue;
            }
            //3.判断是否存在，如果存在的话，获取文件中的内容
            String raw = FileUtil.readString(skillFile, StandardCharsets.UTF_8);
            Skill skill = parse(dirFile.getName(), raw);
            skillMap.put(skill.getName(), skill);
        }
    }

    private Skill parse(String fallbackName, String raw) {
        String name = fallbackName;
        String body = raw;
        String description = firstHeading(raw);
        //1.解析raw 通过---进行分隔
        if (raw.startsWith("---")) {
            String[] res = raw.split("---", 3);
            //2. "" | name+description | body
            if (res.length >= 3) {
                //3. name description中间通过空行间隔
                String[] lines = res[1].split("\\R");
                for (String line : lines) {
                    if (line.trim().startsWith("name:")) {
                        name = line.substring("name:".length()).trim();
                    } else if (line.trim().startsWith("description:")) {
                        description = line.substring("description:".length()).trim();
                    }
                }
                body = res[2].trim();
                if(description == null || description.isBlank()){
                    description = firstHeading(body);
                }
            }
        }
        return new Skill(name, description, body);
    }

    private String firstHeading(String raw) {
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                return trimmed.replaceFirst("^#+", "").trim();
            }
        }
        return "";
    }

    /**
     * 交给大模型的 只包括name 和 description
     * @return initialPrompt
     */
    public String getDescriptions() {
        if (skillMap.isEmpty()) {
            return "no skills available";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Skill> entry : skillMap.entrySet()) {
            String name = entry.getKey();
            Skill skill = entry.getValue();
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(" - ")
                    .append(name)
                    .append(": ")
                    .append(skill.getDescription());
        }
        return sb.toString();
    }

    public Skill find(String name) {
        return skillMap.get(name);
    }

    public Collection<Skill> all() {
        return skillMap.values();
    }

}
