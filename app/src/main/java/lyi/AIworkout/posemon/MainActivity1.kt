package lyi.AIworkout.posemon

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.w3c.dom.Text

class MainActivity1 : AppCompatActivity() {
    private lateinit var back: Button
    private lateinit var nameinputtext: EditText
    private lateinit var phoneinputtext: EditText
    private lateinit var passwordinputtext: EditText
    private lateinit var login: Button
    private lateinit var wrongpass:TextView
    private lateinit var oshctext:TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
        oshctext = findViewById(R.id.oshctext)
        oshctext.movementMethod = LinkMovementMethod.getInstance()
        back = findViewById(R.id.button9)

        back.setOnClickListener {
            val back = Intent(this, MainActivity3::class.java)
            startActivity(back)

        }

    }
}