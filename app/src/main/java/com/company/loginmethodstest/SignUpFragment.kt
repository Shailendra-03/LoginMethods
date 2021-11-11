package com.company.loginmethodstest

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.company.loginmethodstest.databinding.SignUpFragmentBinding
import com.company.loginmethodstest.entities.Users
import com.google.android.gms.dynamic.SupportFragmentWrapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class SignUpFragment : Fragment(),View.OnClickListener {
    private lateinit var auth:FirebaseAuth

    private val registerForImage=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            val data:Intent?=result.data
            data?.data?.let {
                userImageUri=it
                binding.ivUser.setImageURI(userImageUri)
            }
        }
    }
    private var userImageUri:Uri?=null
    private lateinit var dateSetListener:DatePickerDialog.OnDateSetListener
    private lateinit var binding: SignUpFragmentBinding

    private val cal=Calendar.getInstance()
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= SignUpFragmentBinding.inflate(inflater,container,false)
        auth=Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel=ViewModelProvider(this).get(SignUpViewModel::class.java)
        dateSetListener=DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }
        binding.etDateOfBirth.setOnClickListener(this)
        binding.ivUser.setOnClickListener(this)
        binding.btnRegister.setOnClickListener(this)
        binding.ivBack.setOnClickListener(this)
        viewModel.currentuser.observe(viewLifecycleOwner){
            if(it!=null&&it==auth.currentUser){
                startActivity(Intent(this.requireContext(),MainActivity::class.java))
                requireActivity().finish()
            }
        }
        viewModel.errorSignupMessage.observe(viewLifecycleOwner){
            if(!it.isNullOrEmpty()){
                Toast.makeText(requireContext(),it,Toast.LENGTH_LONG).show()
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

    override fun onClick(v: View?) {
        when(v){
            binding.etDateOfBirth->{
                val datePickerDialog=DatePickerDialog(requireContext(),dateSetListener,cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                datePickerDialog.datePicker.maxDate=System.currentTimeMillis()
                datePickerDialog.show()
            }
            binding.ivUser->{
                try{
                    val intent=Intent(Intent.ACTION_GET_CONTENT)
                    intent.type="image/*"
                    registerForImage.launch(intent)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            binding.ivBack->{
                requireActivity().onBackPressed()
            }
            binding.btnRegister->{
                val userName=binding.etUsername.text.toString().trim{ it<=' ' }
                val name=binding.etName.text.toString().trim { it<=' ' }
                val dateOfBirth= binding.etDateOfBirth.text?.trim { it<=' ' }
                val userEmail=binding.etEmail.text.toString().trim { it<=' ' }
                val userPassword=binding.etPassword.text.toString().trim { it<=' ' }
                val user=Users(userImageUri.toString(),name,userEmail,dateOfBirth.toString())
                if(viewModel.checkForDetails(userName,user,userPassword)){
                    viewModel.createUserWithEmailAndPassword(userName,user,userPassword)
                }
            }
        }
    }

    private fun updateDateInView() {
        val myFormat="dd.MM.yyyy"
        val sdf=SimpleDateFormat(myFormat,Locale.getDefault())
        binding.etDateOfBirth.setText(sdf.format(cal.time).toString())
    }

    override fun onDestroyView() {
        (activity as LoginActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        super.onDestroyView()
    }
}