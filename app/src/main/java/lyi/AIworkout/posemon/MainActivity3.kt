package lyi.AIworkout.posemon

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import lyi.AIworkout.posemon.data.SurveyResult



var diff: String? = null
class MainActivity3 : AppCompatActivity() {
    private lateinit var Questions: Button
    private lateinit var Activities: Button
    private lateinit var Informa: Button
    private lateinit var dang: TextView
    private lateinit var level: TextView
    private lateinit var DIFF: TextView


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)
        dang = findViewById(R.id.dang)
        level = findViewById(R.id.level)
        DIFF = findViewById(R.id.difff)

        loadData()


        val total = SurveyResult.Total

        if (total in 0.0..6.0) {
            diff = "低"
            dang.alpha = 1F
            level.alpha = 1F
            dang.text = "低"
            SurveyResult.Diff = diff
        } else if (total > 6 && total <= 12) {
            diff = "中"
            dang.alpha = 1F
            level.alpha = 1F
            dang.text = "中"
            SurveyResult.Diff = diff
        } else if (total > 12) {
            diff = "高"
            dang.alpha = 1F
            level.alpha = 1F
            dang.text = "高"
            SurveyResult.Diff = diff
        }

        //println(diff)
        //println("++"+DIFF.text)
        Questions = findViewById(R.id.button4)
        Questions.setOnClickListener {
            val question = Intent(this, survey::class.java)
            startActivity(question)
        }
        Activities = findViewById(R.id.button5)
        Activities.setOnClickListener {
            //println(dang.text)
            if (level.alpha != 0F) {
                val activityopen = Intent(this, MainActivity::class.java)
                startActivity(activityopen)
            } else {
                dang.text = "請先完成問卷"
                dang.alpha = 1F
            }
        }
        Informa = findViewById(R.id.button6)
        Informa.setOnClickListener {
            val inform = Intent(this, MainActivity1::class.java)
            startActivity(inform)
        }

    }

    override fun onStop() {
        super.onStop()
        dang.text = null
        saveData()
    }
    private fun saveData() {
        if(dang.text != null) {
            val savedText = dang.text.toString()
            dang.text = savedText
            //val savedText1 = DIFF.text.toString()
            val sharedPrefercences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefercences.edit()
            editor.apply {
                putString("STRING_KEY", savedText)
                //println("DIFF  "+DIFF.text)
                //putString("STRING_KEY", savedText1)
            }.apply()
        }
    }

    private fun loadData() {
        val sharedPrefercences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedtext = sharedPrefercences.getString("STRING_KEY", null)
        //val savedtext1 = sharedPrefercences.getString("STRING_KEY", null)
        if (savedtext != null) {
            dang.text = savedtext
            //DIFF.text = savedtext1
            println("Diff = "+SurveyResult.Diff+"  dang:"+savedtext)

            if(dang.text == "低" || dang.text == "中" || dang.text == "高") {
                level.alpha = 1F
                dang.alpha = 1F
                SurveyResult.Diff = dang.text as String
            }
            /**if(dang.text == "低"){
                SurveyResult.Diff = 1
            }else if(dang.text == "中"){
                SurveyResult.Diff = 2
            }else if(dang.text == "高"){
                SurveyResult.Diff = 3
            }*/
        }
    }
}