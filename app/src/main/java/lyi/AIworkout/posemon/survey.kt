package lyi.AIworkout.posemon

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import lyi.AIworkout.posemon.data.SurveyResult

class survey : AppCompatActivity() {
    private lateinit var backtohomepage: Button
    private lateinit var firstsurveysubmit:Button
    private lateinit var less2hour:RadioButton
    private lateinit var _2_4hour:RadioButton
    private lateinit var _4_8hour:RadioButton
    private lateinit var more8hour:RadioButton
    private lateinit var less10permin:RadioButton
    private lateinit var _11_20permin:RadioButton
    private lateinit var more20permin:RadioButton
    private lateinit var _1_2hour:RadioButton
    private lateinit var _2_3hour:RadioButton
    private lateinit var _3_4hour:RadioButton
    private lateinit var more4hour:RadioButton
    private lateinit var less1hour:RadioButton
    private lateinit var survey1yes:RadioButton
    private lateinit var survey1no:RadioButton
    private lateinit var serveynotanswered: TextView
    private lateinit var survey1question:TextView
    private lateinit var relativeLayout:RelativeLayout
    private lateinit var relativeLayout1:RelativeLayout
    var quest1ansed = false
    var quest2 = false
    var ansfinished = false
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.survey)
        less2hour = findViewById(R.id.less2hour)
        less2hour.setOnClickListener{
            _4_8hour.isChecked = false
            more8hour.isChecked = false
        }
        _2_4hour = findViewById(R.id._2_4hour)
        _2_4hour.setOnClickListener {
            _4_8hour.isChecked = false
            more8hour.isChecked = false
        }
        _4_8hour = findViewById(R.id._4_8hour)
        _4_8hour.setOnClickListener {
            _2_4hour.isChecked = false
            less2hour.isChecked = false
        }
        more8hour = findViewById(R.id.more8hour)
        more8hour.setOnClickListener {
            _2_4hour.isChecked = false
            less2hour.isChecked = false
        }
        less10permin = findViewById(R.id.less10permin)
        _11_20permin = findViewById(R.id._11_20permin)
        more20permin = findViewById(R.id.more20permin)
        _1_2hour = findViewById(R.id._1_2hour)
        _2_3hour = findViewById(R.id._2_3hour)
        _3_4hour = findViewById(R.id._3_4hour)
        more4hour = findViewById(R.id.more4hour)
        less1hour = findViewById(R.id.less1hour)
        survey1yes = findViewById(R.id.survey1yes)
        survey1no = findViewById(R.id.survey1no)
        relativeLayout = findViewById(R.id.relativeLayout)
        relativeLayout1 = findViewById(R.id.relativeLayout1)
        backtohomepage = findViewById(R.id.backtohomepage)
        backtohomepage.setOnClickListener{
            if (quest1ansed && ansfinished == false){
                relativeLayout1.visibility = View.VISIBLE
                relativeLayout.visibility = View.INVISIBLE
            }else {
                val passdata = Intent(this, MainActivity3::class.java)
                //val back = Intent(this, MainActivity3::class.java)
                startActivity(passdata)
            }
        }
        serveynotanswered = findViewById(R.id.serveynotanswered)
        survey1question = findViewById(R.id.survey1question)
        firstsurveysubmit = findViewById(R.id.firstsurveysubmit)
        firstsurveysubmit.setOnClickListener{
            var ans1 = 0F
            var ans2 = 0
            var ans3 = 0

            var quest1ans = ""
            var question1answered = "第一題"
            var question2answered = "第二題"
            var question3answered = "第三題"
            if(survey1yes.isChecked()){
                survey1question.text = "是否正接受物理/醫學治療?"
                if(quest2){ survey1question.text = "請多休息和堅持治療" +
                            "（暫時不需要做健身操）"
                    survey1yes.visibility = View.INVISIBLE
                    survey1no.visibility = View.INVISIBLE
                }
                //quest1ansed = true
                quest2 = true

            }
            else if(survey1no.isChecked()){
                //quest1ans = "no"

                if(quest2){
                    survey1question.text = "請看醫生或接受治療"+
                            "（暫時不需要做健身操）"
                    survey1yes.visibility = View.INVISIBLE
                    survey1no.visibility = View.INVISIBLE
                    ansfinished = true
                }else{
                    quest1ansed = true
                    relativeLayout1.visibility = View.INVISIBLE
                    relativeLayout.visibility = View.VISIBLE
                }
            }
            if(quest1ansed){
                if(less2hour.isChecked()){
                    ans1 = 0.5F
                    question1answered = ""
                }
                else if(_2_4hour.isChecked()){
                    ans1 = 0.75F
                    question1answered = ""
                }
                if(_4_8hour.isChecked()){
                    ans1 = 1F
                    question1answered = ""
                }
                else if(more8hour.isChecked()) {
                    ans1 = 1.5F
                    question1answered = ""
                }
                if(less10permin.isChecked()){
                    ans2 = 0
                    question2answered = ""
                }
                else if(_11_20permin.isChecked()){
                    ans2 =3
                    question2answered = ""
                }
                else if(more20permin.isChecked()){
                    ans2 = 6
                    question2answered = ""
                }
                if(_1_2hour.isChecked()){
                    ans3 = 6
                    question3answered = ""
                }
                else if(_2_3hour.isChecked()){
                    ans3 = 4
                    question3answered = ""
                }
                else if(_3_4hour.isChecked()){
                    ans3 = 2
                    question3answered = ""
                }
                else if(more4hour.isChecked()){
                    ans3 = 0
                    question3answered = ""
                }
                else if(less1hour.isChecked()){
                    ans3 = 8
                    question3answered = ""
                }
                var total = (ans2 + ans3) * ans1
                //println(total)
                if(question1answered == "" && question2answered == "" && question3answered == ""){
                    SurveyResult.Total = total
                    val passdata = Intent(this, MainActivity3::class.java)
                    //val back = Intent(this, MainActivity3::class.java)
                    startActivity(passdata)
                }else if(question1answered == "" || question2answered == "" || question3answered == ""){
                    serveynotanswered.text =
                        "$question1answered $question2answered $question3answered 未完成"
                    serveynotanswered.alpha = 1F
                }else{
                    serveynotanswered.text = "請完成問卷"
                    serveynotanswered.alpha = 1F
                }
            }





            //val back = Intent(this, MainActivity3::class.java)

            //startActivity(back)
        }
    }
}