package com.yourorg.craftlite.skills

import javax.inject.Inject

class SkillPromptFormatter @Inject constructor() {
  fun format(skills: List<String>): String {
    return skills.mapIndexed { index, value -> "${index + 1}. $value" }.joinToString("\n")
  }
}
