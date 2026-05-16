package com.edgellm.skills

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class SkillManager(private val context: Context) {

    private val skillsDir = File(context.filesDir, "skills").also { it.mkdirs() }

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills

    suspend fun loadAll() = withContext(Dispatchers.IO) {
        val found = skillsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> SkillParser.parse(File(dir, "SKILL.md")) }
            ?: emptyList()
        _skills.value = found
    }

    /** Import skill from a local directory (via file picker) */
    suspend fun importFromDirectory(srcPath: String) = withContext(Dispatchers.IO) {
        val src = File(srcPath)
        val dest = File(skillsDir, src.name)
        src.copyRecursively(dest, overwrite = true)
        loadAll()
    }

    /** Download and install skill from a URL pointing to a SKILL.md */
    suspend fun downloadFromUrl(url: String) = withContext(Dispatchers.IO) {
        val content = URL(url).readText()
        val skillName = url.substringAfterLast("/").substringBeforeLast(".")
        val dir = File(skillsDir, skillName).also { it.mkdirs() }
        File(dir, "SKILL.md").writeText(content)
        loadAll()
    }

    fun deleteSkill(skill: Skill) {
        File(skill.localPath).deleteRecursively()
        _skills.value = _skills.value.filter { it.name != skill.name }
    }

    /**
     * Build the system prompt for Agent Skills mode.
     * Appends all skill names/descriptions so the model can auto-invoke them.
     */
    fun buildSkillSystemPrompt(activeSkills: List<Skill>): String {
        if (activeSkills.isEmpty()) return ""
        val skillList = activeSkills.joinToString("\n") { skill ->
            "- ${skill.name}: ${skill.description}"
        }
        val skillInstructions = activeSkills.joinToString("\n\n") { skill ->
            "=== SKILL: ${skill.name} ===\n${skill.instructions}"
        }
        return """
You have access to the following skills. If the user's request matches a skill, invoke it automatically.

Available skills:
$skillList

$skillInstructions

Always prefer using a skill when applicable. If no skill matches, respond normally.
        """.trimIndent()
    }
}
