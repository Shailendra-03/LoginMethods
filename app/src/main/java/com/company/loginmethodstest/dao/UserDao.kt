package com.company.loginmethodstest.dao

import com.company.loginmethodstest.entities.Users
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class UserDao {

    val db=Firebase.firestore
    private val collection=db.collection("Users")

    suspend fun addUserToDatabase(users: Users){
        val userName=users.email.substringBefore("@")
        collection.document(userName).set(users).await()
    }

    suspend fun addUserToDatabaseFromEmail(userName: String, user: Users) {
        collection.document(userName).set(user).await()
    }

}