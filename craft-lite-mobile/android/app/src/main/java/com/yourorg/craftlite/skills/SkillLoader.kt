package com.yourorg.craftlite.skills

import javax.inject.Inject

class SkillLoader @Inject constructor() {
  suspend fun loadSkillTexts(): List<String> = listOf(
    "Use structured tools before guessing about workspace contents.",
    "Do not assume shell access exists on mobile.",
  )
}
