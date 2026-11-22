package com.example.mrprivate

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test demonstrating and validating Compose testing v2 APIs (`androidx.compose.ui.test.junit4.v2.*`).
 */
@RunWith(AndroidJUnit4::class)
class ComposeTestingV2Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposeGreetingDisplayed() {
        composeTestRule.setContent {
            val appState = MrPrivateState(isPreviewMode = true)
            MrPrivateTheme(appState = appState) {
                Text(text = "MR Private")
            }
        }

        composeTestRule.onNodeWithText("MR Private").assertIsDisplayed()
    }
}
