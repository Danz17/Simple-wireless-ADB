package com.phenix.wirelessadb.theme

/**
 * Theme mode options (v1.2.0).
 */
enum class ThemeMode {
  /** Follow system setting */
  SYSTEM,

  /** Always light theme */
  LIGHT,

  /** Always dark theme */
  DARK;

  companion object {
    val DEFAULT = SYSTEM

    fun fromOrdinal(ordinal: Int): ThemeMode {
      return entries.getOrElse(ordinal) { DEFAULT }
    }
  }
}
