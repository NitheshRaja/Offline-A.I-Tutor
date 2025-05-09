package com.google.mediapipe.examples.llminference.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.mediapipe.examples.llminference.R
import com.google.mediapipe.examples.llminference.ThemePreferenceManager
import com.google.mediapipe.examples.llminference.databinding.ActivitySignUpBinding
import com.google.mediapipe.examples.llminference.SecureStorage
import com.google.mediapipe.examples.llminference.UserInfo

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePreferenceManager.applyTheme(ThemePreferenceManager.loadThemePreference(this))
        
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                createAccount(name, email, password)
            }
        }

        binding.loginTextView.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.confirmPasswordInputLayout.error = null

        var isValid = true
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        }
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
             isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Enter a valid email address"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
             isValid = false
        } else if (password.length < 6) {
             binding.passwordInputLayout.error = "Password must be at least 6 characters"
             isValid = false
        }
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "Confirm password is required"
             isValid = false
        }
        if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Passwords do not match"
             isValid = false
        }
        return isValid
    }

    private fun createAccount(name: String, email: String, password: String) {
        showProgress()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            hideProgress()
                            if (profileTask.isSuccessful) {
                                val userInfo = UserInfo(name, email, "Email")
                                SecureStorage.saveUserInfo(this, userInfo)
                                
                                Toast.makeText(this, "Account created successfully! Please login.", 
                                    Toast.LENGTH_LONG).show()
                                finish()
                            } else {
                                Toast.makeText(this, "Account created, but failed to update profile: ${profileTask.exception?.message}", 
                                    Toast.LENGTH_LONG).show()
                                 finish()
                            }
                        }
                } else {
                    hideProgress()
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.signUpButton.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        binding.signUpButton.isEnabled = true
    }
} 