package com.company.loginmethodstest

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.company.loginmethodstest.databinding.SignInFragmentBinding
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.*

class SignInFragment : Fragment() {

    private val resultLauncherForGoogle=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            val data: Intent? =result.data
            if(data!=null){
                viewModel.getSignInAccount(data)
            }
    }

    private lateinit var binding:SignInFragmentBinding
    private lateinit var callbackManager:CallbackManager

    private lateinit var viewModel: SignInViewModel
    private lateinit var auth:FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= SignInFragmentBinding.inflate(inflater,container,false)
        viewModel=ViewModelProvider(requireActivity()).get(SignInViewModel::class.java)
        auth=Firebase.auth
        callbackManager=CallbackManager.Factory.create()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        binding.btnSignIn.setOnClickListener {
            viewModel.signInWithEmailAndPassword(binding.etUsername.text.toString(),binding.etPassword.text.toString())
        }

        binding.ibGmail.setOnClickListener {
            signInWithGoogle()
        }

        binding.ibFacebook.setOnClickListener {
            viewModel.signInWithFacebook(this,callbackManager)
        }

        binding.tvForgotPassword.setOnClickListener {
            val email=binding.etUsername.text.toString().trim{ it<=' '}
            if(email.isNotEmpty() ||email.contains("@")){
                viewModel.sendResetEmailPassword(email)
            }else{
                Toast.makeText(requireContext(),"Please Enter Correct Email",Toast.LENGTH_LONG).show()
            }
        }

        viewModel.currentUser.observe(viewLifecycleOwner){
            if(it==auth.currentUser&&it!=null){
                startActivity(Intent(this.requireContext(),MainActivity::class.java))
                requireActivity().finish()
            }
        }
        viewModel.errorLogInMessage.observe(viewLifecycleOwner){
            if(!it.isNullOrEmpty()){
                Toast.makeText(this.requireContext(),it,Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.progressBar.observe(viewLifecycleOwner){
            if(it==true){
                binding.progressBar.visibility=View.VISIBLE
            }else{
                binding.progressBar.visibility=View.GONE
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode,resultCode,data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun signInWithGoogle() {
        val options=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()

        val googleSignInClient= GoogleSignIn.getClient(this.requireActivity(),options)
        val signInIntent=googleSignInClient.signInIntent
        resultLauncherForGoogle.launch(signInIntent)
    }

}