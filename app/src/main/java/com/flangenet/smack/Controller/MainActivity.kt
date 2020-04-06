package com.flangenet.smack.Controller

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flangenet.smack.Model.Channel
import com.flangenet.smack.R
import com.flangenet.smack.Services.AuthService
import com.flangenet.smack.Services.MessageService
import com.flangenet.smack.Services.UserDataService
import com.flangenet.smack.Utilities.BROADCAST_USER_DATA_CHANGE
import com.flangenet.smack.Utilities.SOCKET_URL
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : AppCompatActivity() {


    val socket = IO.socket(SOCKET_URL)
    lateinit var channelAdapter: ArrayAdapter<Channel>

    private fun setupAdapters(){
        channelAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,MessageService.channels)
        channel_list.adapter = channelAdapter
        //this.channel_list.adapter = channelAdapter


    }


    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        socket.connect()
        socket.on("channelCreated", onNewChannel)


        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_home,
            R.id.nav_gallery,
            R.id.nav_slideshow
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupAdapters()

        if (App.prefs.isLoggedIn) {
            AuthService.findUserByEmail(this){}
        }
    }


    override fun onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(userDataChangeReceiver, IntentFilter(BROADCAST_USER_DATA_CHANGE))

        super.onResume()
    }




    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userDataChangeReceiver)
        socket.disconnect()
        super.onDestroy()
    }

    private val userDataChangeReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent?) {
            println("Broadcast worked")
            println(App.prefs.isLoggedIn)

            if (App.prefs.isLoggedIn) {
                userNameNavHeader.text = UserDataService.name
                userEmailNavHeader.text = UserDataService.email
                val resourceId = resources.getIdentifier(UserDataService.avatarName,"drawable", packageName)
                userImageNavHeader.setImageResource(resourceId)
                userImageNavHeader.setBackgroundColor(UserDataService.returnAvatarColor(UserDataService.avatarColor))
                println(UserDataService.name)
                println(UserDataService.email)
                println(userNameNavHeader.text)
                println(userEmailNavHeader.text)
                println(resourceId)

                userEmailNavHeader.refreshDrawableState()

                loginBtnNavHeader.text = "Logout"

                MessageService.getChannels(context) { complete ->
                    if (complete) {
                        channelAdapter.notifyDataSetChanged()

                    }
                }
            }
        }
    }



    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun loginBtnNavClicked(view: View) {
        if (App.prefs.isLoggedIn) {
            // Log out
            UserDataService.logout()
            userNameNavHeader.text = ""
            userEmailNavHeader.text= ""
            userImageNavHeader.setImageResource(R.drawable.profiledefault)
            userImageNavHeader.setBackgroundColor(Color.TRANSPARENT)
            loginBtnNavHeader.text = "Login"


        } else {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }

    }

    fun addChannelClicked(view: View){
        if (App.prefs.isLoggedIn){
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.add_channel_dialog, null)

            builder.setView(dialogView)
                .setPositiveButton("Add"){dialog: DialogInterface?, which: Int ->
                    // Perform some logic when clicked
                    val nameTextField = dialogView.findViewById<EditText>(R.id.addChannelNameTxt)
                    val descTextField = dialogView.findViewById<EditText>(R.id.addChannelDesc)
                    val channelName = nameTextField.text.toString()
                    val channelDesc = descTextField.text.toString()

                    // Create channel with name and description
                    socket.emit("newChannel",channelName,channelDesc)


                }
                .setNegativeButton("Cancel"){dialog: DialogInterface?, which: Int ->
                    // Cancel and close the dialog

                }
                .show()
        }

    }


    private val onNewChannel = Emitter.Listener { args ->
        // println(args[0] as String)
        runOnUiThread{
            val channelName = args[0] as String
            val channelDescription = args[1] as String
            val channelId = args[2] as String

            val newChannel = Channel(channelName, channelDescription, channelId)
            MessageService.channels.add(newChannel)
            channelAdapter.notifyDataSetChanged()

        }

    }

    fun sendMessageBtnClicked(view: View){
        hideKeyboard()
    }


    fun hideKeyboard(){
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus?.windowToken,0)
        }
    }


}
