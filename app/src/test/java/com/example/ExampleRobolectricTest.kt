package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chennai Research Hub", appName)
  }

  @Test
  fun test_viewmodel() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.ResearchViewModel(app)
    org.junit.Assert.assertNotNull(viewModel)
  }

  @Test
  fun test_main_activity() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup() // Creates, starts, and resumes the activity
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun test_auth_flow() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.ResearchViewModel(app)

    // Verify initial state is logged out
    org.junit.Assert.assertFalse(viewModel.isLoggedIn.value)

    // Default credentials should succeed
    val loginAdmin = viewModel.login("admin", "admin123")
    org.junit.Assert.assertTrue(loginAdmin)
    org.junit.Assert.assertTrue(viewModel.isLoggedIn.value)

    // Logout should reset state
    viewModel.logout()
    org.junit.Assert.assertFalse(viewModel.isLoggedIn.value)

    // Signing up a new academic account
    val signupNew = viewModel.signup("dr_hemanth", "secret123", "secret123")
    org.junit.Assert.assertTrue(signupNew)

    // Login with the newly registered credentials
    val loginNew = viewModel.login("dr_hemanth", "secret123")
    org.junit.Assert.assertTrue(loginNew)
    org.junit.Assert.assertTrue(viewModel.isLoggedIn.value)
  }

  @Test
  fun test_change_password_flow() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.ResearchViewModel(app)

    // Log in
    viewModel.login("admin", "admin123")
    org.junit.Assert.assertEquals("admin", viewModel.loggedInUser.value)

    // Fail old password mismatch
    val changeFailOld = viewModel.changePassword("wrong", "newSecret", "newSecret")
    org.junit.Assert.assertFalse(changeFailOld)

    // Fail confirm mismatch
    val changeFailMismatch = viewModel.changePassword("admin123", "newSecret", "different")
    org.junit.Assert.assertFalse(changeFailMismatch)

    // Success change
    val changeSuccess = viewModel.changePassword("admin123", "newSecret", "newSecret")
    org.junit.Assert.assertTrue(changeSuccess)

    // Check login with new password
    viewModel.logout()
    org.junit.Assert.assertFalse(viewModel.isLoggedIn.value)

    val loginWithOld = viewModel.login("admin", "admin123")
    org.junit.Assert.assertFalse(loginWithOld)

    val loginWithNew = viewModel.login("admin", "newSecret")
    org.junit.Assert.assertTrue(loginWithNew)
  }
}
