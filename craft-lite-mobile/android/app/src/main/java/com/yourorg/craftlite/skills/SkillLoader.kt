package com.yourorg.craftlite.skills

import javax.inject.Inject

class SkillLoader @Inject constructor() {
  suspend fun loadSkillTexts(): List<String> = emptyList()
}
