package com.flangenet.smack.Controller

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager.*
import com.flangenet.smack.R
import com.flangenet.smack.Services.AuthService
import com.flangenet.smack.Services.UserDataService
import com.flangenet.smack.Utilities.BROADCAST_USER_DATA_CHANGE
import kotlinx.android.synthetic.main.activity_create_user.*

class CreateUserActivity : AppCompatActivity() {

    var userAvatar = "profileDefault"
    var avatarColor = "[0.5, 0.5, 0.5, 1]"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)
        createSpinner.visibility = View.INVISIBLE
    }

    fun generateUserAvatar(view: View) {
        val random = java.util.Random()
        val color = random.nextInt(2)
        val avatar = random.nextInt(28)

        if (color == 0) {
            userAvatar = "light$avatar"
        }else {
            userAvatar = "dark$avatar"
        }
        val  resourceId = resources.getIdentifier(userAvatar, "drawable", packageName)
        createAvatarImageView.setImageResource(resourceId)

    }

    fun generateColorClicked(view: View){
        val random = java.util.Random()
        val red = random.nextInt(255)
        val green = random.nextInt(255)
        val blue = random.nextInt(255)

        createAvatarImageView.setBackgroundColor(Color.rgb(red,green, blue))

        val savedR = red.toDouble()/255
        val savedG = green.toDouble()/255
        val savedB = blue.toDouble()/255

        avatarColor = "[$savedR, $savedG, $savedB, 1]"
        println(this.avatarColor)

    }

    fun createUserClicked(view: View){
        enableSpinner(true)
        val userName = createUserNameText.text.toString()
        val email = createEmailText.text.toString()
        val password = createPasswordText.text.toString()

        if (userName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()){
            AuthService.registerUser(this, email, password) { registerSuccess ->
                if (registerSuccess){
                    AuthService.loginUser(this, email, password) { loginSuccess ->
                        if (loginSuccess){
                            AuthService.createUser(this,userName, email, userAvatar, avatarColor){ createSuccess ->
                                if (createSuccess) {

                                    val userDataChange = Intent(BROADCAST_USER_DATA_CHANGE)
                                    LocalBroadcastManager.getInstance(this).sendBroadcast(userDataChange)
                                    enableSpinner(false)
                                    finish()
                                } else {
                                    errorToast("3")
                                }
                            }
                        } else {
                            errorToast("2")
                        }
                    }
                } else {
                    errorToast("1")
                }
            }
        } else {
            Toast.makeText(this, "Make sure username, email, and password are filled in.", Toast.LENGTH_LONG).show()
            enableSpinner(false)
        }



    }

    fun errorToast( extraMsg :String){
        Toast.makeText(this, "Something went wrong, please try again $extraMsg", Toast.LENGTH_LONG).show()
        enableSpinner(false)
    }
    fun enableSpinner(enable : Boolean){
        if (enable){
            createSpinner.visibility = View.VISIBLE
        }else{
            createSpinner.visibility = View.INVISIBLE
        }
        createUserBtn.isEnabled = !enable
        createAvatarImageView.isEnabled = !enable
        backgroundColorButton.isEnabled = !enable
    }
}
