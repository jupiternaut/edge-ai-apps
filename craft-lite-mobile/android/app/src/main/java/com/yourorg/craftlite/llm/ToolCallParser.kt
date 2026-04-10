package com.yourorg.craftlite.llm

import com.yourorg.craftlite.domain.tools.ToolRequest
import javax.inject.Inject

class ToolCallParser @Inject constructor() {
  fun parse(rawText: String): ToolRequest? {
    return null
  }
}
