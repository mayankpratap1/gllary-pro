package com.edgellm.skills

data class Skill(
    val name: String,
    val description: String,
    val homepage: String? = null,
    val instructions: String,      // Full markdown body after frontmatter
    val localPath: String          // Directory path where SKILL.md lives
)
