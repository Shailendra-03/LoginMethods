package com.company.loginmethodstest


import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.loginmethodstest.dao.UserDao
import com.company.loginmethodstest.entities.Users
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class SignInViewModel : ViewModel() {

    private var _progressBar=MutableLiveData(false)
    val progressBar:LiveData<Boolean> get() = _progressBar
    private var _currentUser=MutableLiveData<FirebaseUser?>()
    val currentUser:LiveData<FirebaseUser?> get()=_currentUser
    private var _errorLogInMessage =MutableLiveData("")
    val errorLogInMessage:LiveData<String> get() = _errorLogInMessage
    private val auth=Firebase.auth

    private val userDao=UserDao()
    fun signInWithEmailAndPassword(email: String, password: String) {
        _progressBar.value=true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    auth.signInWithEmailAndPassword(email,password).await()
                    withContext(Dispatchers.Main){
                        _currentUser.value=auth.currentUser
                        _progressBar.value=false
                    }
                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        _errorLogInMessage.value=e.message
                        _progressBar.value=false
                    }
                }
            }
    }

    fun getSignInAccount(data: Intent) {
        _progressBar.value=true
        try {
            val account=GoogleSignIn.getSignedInAccountFromIntent(data).result
            account?.let {
                firebaseAuthWithGoogle(it.idToken!!)
            }
        }catch (e:Exception){
            _progressBar.value=false
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credentials=GoogleAuthProvider.getCredential(idToken,null)
        viewModelScope.launch(Dispatchers.IO){
            try {
                auth.signInWithCredential(credentials).await()
                    auth.currentUser?.let { addUserToDatabase(it) }
            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    _errorLogInMessage.value=e.message
                    _progressBar.value=false
                }
            }
        }
    }

    fun signInWithFacebook(fragment: SignInFragment,callbackManager: CallbackManager) {
        LoginManager.getInstance().logInWithReadPermissions(fragment, listOf("email","public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager,object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                if (result != null) {
                    handleFacebookAccessToken(result.accessToken)
                }
            }
            override fun onCancel() {
                _errorLogInMessage.value="You have cancelled the login"
            }

            override fun onError(error: FacebookException?) {
                _errorLogInMessage.value=error?.message
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        _progressBar.value=true
        val credential = FacebookAuthProvider.getCredential(token.token)
        viewModelScope.launch(Dispatchers.IO){
            try {
                auth.signInWithCredential(credential).await()
                auth.currentUser?.let { addUserToDatabase(it) }
            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    _errorLogInMessage.value=e.message
                    _progressBar.value=false
                }
            }
        }
    }

    private fun addUserToDatabase(firebaseUser: FirebaseUser){
        val user=Users(firebaseUser.photoUrl.toString(),firebaseUser.displayName.toString(),firebaseUser.email.toString(),"")
        viewModelScope.launch(Dispatchers.IO){
            userDao.addUserToDatabase(user)
            withContext(Dispatchers.Main){
                _currentUser.value=auth.currentUser
                _errorLogInMessage.value="Successfully logged in"
                _progressBar.value=false
            }
        }
    }

    fun sendResetEmailPassword(email: String) {
        _progressBar.value=true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                auth.sendPasswordResetEmail(email).await()
                withContext(Dispatchers.Main){
                    _errorLogInMessage.value="A password reset link has been sent to your email"
                    _progressBar.value=false
                }
            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    _progressBar.value=false
                    _errorLogInMessage.value=e.message
                }
            }


        }
    }
}