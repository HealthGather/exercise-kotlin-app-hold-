package lyi.AIworkout.posemon

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView


class MainActivity2 : AppCompatActivity() {
    private lateinit var back: Button
    private lateinit var nameinputtext: EditText
    private lateinit var phoneinputtext: EditText
    private lateinit var passwordinputtext: EditText
    private lateinit var login: Button
    private lateinit var wrongpass:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        back = findViewById(R.id.main)
        nameinputtext = findViewById(R.id.name_input)
        phoneinputtext = findViewById(R.id.phone_input)
        passwordinputtext = findViewById(R.id.password_input)
        login = findViewById(R.id.login_)
        wrongpass = findViewById(R.id.wrong)
        back.setOnClickListener {
            val back = Intent(this, MainActivity3::class.java)
            startActivity(back)

        }
        login.setOnClickListener {
            val name = nameinputtext.text.toString()
            val phone = phoneinputtext.text.toString()
            val password = passwordinputtext.text.toString()
            println("Username: $name Password: $password")
            if (name == "student" && phone == "12345678" && password == "12345678") {
                val opensurvey = Intent(this, survey::class.java)
                startActivity(opensurvey)

            }else {
                wrongpass.alpha = 1F
            }
        }
    }
}