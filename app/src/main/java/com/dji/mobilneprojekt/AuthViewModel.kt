package com.dji.mobilneprojekt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
class AuthViewModel : ViewModel() {
    private val auth : FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _authState = MutableLiveData<AuthState>();
    val authState : LiveData<AuthState> = _authState

    //checking status of authentication
    init {
        checkAuthStatus()
    }
    //check whether user is already authenticated or not
    fun checkAuthStatus (){
        if (auth.currentUser != null) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, password: String, username: String) {

        if(email.isEmpty() || password.isEmpty() || username.isEmpty()){
            _authState.value = AuthState.Error("Email or password cannot be empty.")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task->
                if(task.isSuccessful){
                 _authState.value = AuthState.Authenticated
                }else{
                _authState.value = AuthState.Error(task.exception?.message ?:"Something went wrong.")
                }
            }
    }

    fun register(email: String, password: String, username: String) {

        if(email.isEmpty() || password.isEmpty() || username.isEmpty()){
            _authState.value = AuthState.Error("Email or password cannot be empty.")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task->
                if(task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                    val user = auth.currentUser?.uid
                    if (user != null) {
                        val userProfile = mapOf("username" to username, "email" to email)
                        Firebase.firestore.collection("users").document(user).set(userProfile)
                    }
                }else{
                    _authState.value = AuthState.Error(task.exception?.message ?:"Something went wrong.")
                }
            }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

}
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()

    data class Error(val message: String) : AuthState()

}