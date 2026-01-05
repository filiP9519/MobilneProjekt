package com.dji.mobilneprojekt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dji.mobilneprojekt.data.AppDatabase
import com.dji.mobilneprojekt.Repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(AppDatabase.getDatabase(application))

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState
    private val _currentUserId = MutableStateFlow(-1)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    var loggedInUser : Int = -1
        private set

    fun login(username : String, password : String){
        if (username.isBlank() || password.isBlank()){
            _authState.value = AuthState.Error("Username and password cannot be empty!")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch{
        val user = repository.login(username, password)
            if(user != null){
                _currentUserId.value = user.userID
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error("Invalid username or password")
            }
        }
    }

    fun register(username: String, password : String){
        if (username.isBlank() || password.isBlank()){
            _authState.value = AuthState.Error("Username and password cannot be empty!")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch{
            val success = repository.register(username, password)
            if (success){
                //autologin
                login(username, password)
            } else {
                _authState.value = AuthState.Error("Username already exists")
            }
        }
    }

    fun signOut (){
        _currentUserId.value = -1
        _authState.value = AuthState.Unauthenticated
    }

}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()

    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
