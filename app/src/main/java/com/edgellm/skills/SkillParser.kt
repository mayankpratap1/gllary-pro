package com.edgellm.skills

import java.io.File

/**
 * Parses SKILL.md files in Edge Gallery format.
 *
 * Format:
 * ---
 * name: my-skill
 * description: What this skill does
 * homepage: https://optional-link.com
 * ---
 * # Instructions
 * Full markdown body...
 */
object SkillParser {

    fun parse(file: File): Skill? {
        if (!file.exists()) return null
        val text = file.readText()

        // Extract frontmatter between --- markers
        val fmRegex = Regex("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)", RegexOption.DOT_MATCHES_ALL)
        val match = fmRegex.find(text.trim()) ?: return null

        val frontmatter = match.groupValues[1]
        val body = match.groupValues[2].trim()

        val meta = mutableMapOf<String, String>()
        frontmatter.lines().forEach { line ->
            val colon = line.indexOf(':')
            if (colon > 0) {
                meta[line.substring(0, colon).trim()] = line.substring(colon + 1).trim()
            }
        }

        val name = meta["name"] ?: return null
        val description = meta["description"] ?: return null

        return Skill(
            name = name,
            description = description,
            homepage = meta["homepage"],
            instructions = body,
            localPath = file.parent ?: ""
        )
    }
}
