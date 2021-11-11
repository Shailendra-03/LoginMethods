package com.company.loginmethodstest

import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.loginmethodstest.dao.UserDao
import com.company.loginmethodstest.entities.Users
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class SignUpViewModel : ViewModel() {
    private val storageRef=Firebase.storage.reference
    private val userDao=UserDao()

    private var _progressBar=MutableLiveData(false)
    val progressBar:LiveData<Boolean> get() = _progressBar

    private var _currentUser= MutableLiveData<FirebaseUser?>()
    val currentuser: LiveData<FirebaseUser?> get()=_currentUser
    private var _errorSignUpMessage = MutableLiveData("")
    val errorSignupMessage: LiveData<String> get() = _errorSignUpMessage
    private val auth=Firebase.auth


    fun createUserWithEmailAndPassword(userName: String,user: Users,userPassword: String) {
        _progressBar.value=true
        viewModelScope.launch(Dispatchers.IO){
            try{
                auth.createUserWithEmailAndPassword(user.email,userPassword).await()
                uploadUserImageToStorage(userName,user)
            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    _progressBar.value=false
                    _errorSignUpMessage.value=e.message
                }
            }
        }
    }

    private fun uploadUserImageToStorage(userName: String,user: Users) {
        viewModelScope.launch(Dispatchers.IO){
            try {
                val childRef=storageRef.child("userImages/${user.email}")
               childRef.putFile(user.imageUrl.toUri()).await()
                val url=childRef.downloadUrl.await()
                user.imageUrl=url.toString()
                addUserToDataBase(userName,user)

            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    _progressBar.value=false
                    _errorSignUpMessage.value=e.message
                }
            }
        }
    }

    private fun addUserToDataBase(userName: String, user: Users) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.addUserToDatabaseFromEmail(userName,user)
                withContext(Dispatchers.Main){
                    _errorSignUpMessage.value="Successfully Logged In"
                    _currentUser.value=auth.currentUser
                    _progressBar.value=false
            }
            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    _progressBar.value=false
                    _errorSignUpMessage.value=e.message
                }

            }
        }

    }

    fun checkForDetails(userName: String, user: Users, userPassword: String): Boolean {
        when{
            user.imageUrl.isEmpty()->{
                _errorSignUpMessage.value="Enter"
                return false
            }
            userName.isEmpty()->{
                _errorSignUpMessage.value="Enter"
                return false
            }
            user.displayName.isEmpty()->{
                _errorSignUpMessage.value="Enter"
                return false
            }
            user.dateOfBirth.isEmpty()->{
                _errorSignUpMessage.value="Enter"
                return false
            }
            user.email.isEmpty()||!user.email.contains("@",true)->{
                _errorSignUpMessage.value="Enter"
                return false
            }
            userPassword.length<8->{
                _errorSignUpMessage.value="Enter"
                return false
            }
            else->{
                return true
            }
        }
    }

}