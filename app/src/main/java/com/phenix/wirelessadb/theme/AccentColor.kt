package com.phenix.wirelessadb.theme

import androidx.annotation.ColorRes
import com.phenix.wirelessadb.R

/**
 * Accent color presets for the app theme (v1.2.0).
 *
 * Each accent defines primary and secondary colors for consistent theming.
 */
enum class AccentColor(
  val displayName: String,
  @ColorRes val primaryColor: Int,
  @ColorRes val primaryVariant: Int,
  @ColorRes val onPrimary: Int
) {
  BLUE(
    displayName = "Blue",
    primaryColor = R.color.accent_blue_primary,
    primaryVariant = R.color.accent_blue_variant,
    onPrimary = R.color.accent_on_primary
  ),

  TEAL(
    displayName = "Teal",
    primaryColor = R.color.accent_teal_primary,
    primaryVariant = R.color.accent_teal_variant,
    onPrimary = R.color.accent_on_primary
  ),

  PURPLE(
    displayName = "Purple",
    primaryColor = R.color.accent_purple_primary,
    primaryVariant = R.color.accent_purple_variant,
    onPrimary = R.color.accent_on_primary
  ),

  ORANGE(
    displayName = "Orange",
    primaryColor = R.color.accent_orange_primary,
    primaryVariant = R.color.accent_orange_variant,
    onPrimary = R.color.accent_on_primary
  ),

  PINK(
    displayName = "Pink",
    primaryColor = R.color.accent_pink_primary,
    primaryVariant = R.color.accent_pink_variant,
    onPrimary = R.color.accent_on_primary
  ),

  GREEN(
    displayName = "Green",
    primaryColor = R.color.accent_green_primary,
    primaryVariant = R.color.accent_green_variant,
    onPrimary = R.color.accent_on_primary
  );

  companion object {
    val DEFAULT = TEAL

    fun fromOrdinal(ordinal: Int): AccentColor {
      return entries.getOrElse(ordinal) { DEFAULT }
    }
  }
}
