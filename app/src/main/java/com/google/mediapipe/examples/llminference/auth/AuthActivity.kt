package com.google.mediapipe.examples.llminference.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.mediapipe.examples.llminference.HomeActivity
import com.google.mediapipe.examples.llminference.R
import com.google.mediapipe.examples.llminference.ThemePreferenceManager
import com.google.mediapipe.examples.llminference.databinding.ActivityAuthBinding
import com.google.mediapipe.examples.llminference.auth.SignUpActivity
import com.google.mediapipe.examples.llminference.SecureStorage
import com.google.mediapipe.examples.llminference.UserInfo
import com.google.mediapipe.examples.llminference.util.KeystoreHelper

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "AuthActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePreferenceManager.applyTheme(ThemePreferenceManager.loadThemePreference(this))
        
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.signUpTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.forgotPasswordTextView.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(this, "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            return false
        }
        binding.emailInputLayout.error = null

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            return false
        }
        binding.passwordInputLayout.error = null

        return true
    }

    private fun loginUser(email: String, password: String) {
        showProgress()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideProgress()
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userInfo = UserInfo(
                        user?.displayName ?: "",
                        user?.email ?: "",
                        "Email"
                    )
                    SecureStorage.saveUserInfo(this, userInfo)
                    
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        binding.loginButton.isEnabled = true
    }
}
