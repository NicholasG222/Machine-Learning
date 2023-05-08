package com.example.proyekmlkel6

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.*
import java.lang.Math.sqrt
import java.nio.charset.StandardCharsets
import java.util.HashMap
import kotlin.math.log10
import kotlin.math.pow


class MainActivity : AppCompatActivity() {
    fun readCsv(inputStream: InputStream): List<LoanData> {
        val reader = inputStream.bufferedReader()
        val header = reader.readLine()

        return reader.lineSequence()
            .filter { it.isNotBlank()}

            .map {

                val (id, gender, married, dependents, education, employment,
                    income, coApplicantIncome, loanAmount, term, history, propertyArea, loanStatus) = it.split(',', ignoreCase = false, limit = 13)
                var marriageStatus = 0
                var label = 0
                var isGraduate = 0
                var isEmployed = 0
                var area = 0
                var dependentInt = 0
                if(dependents == "3+"){
                    dependentInt = 3
                }else{
                    dependentInt = dependents.trim().toInt()
                }
                if(married == "Yes"){
                    marriageStatus = 1
                }
                if(loanStatus == "Y"){
                    label = 1
                }
                if(education == "Graduate"){
                    isGraduate = 1
                }
                if(employment == "Yes"){
                    isEmployed = 1
                }
                if(propertyArea == "Semiurban"){
                    area = 1
                }else if(propertyArea == "Rural"){
                    area = 2
                }
                LoanData(id, gender, normalization(marriageStatus), normalization(dependentInt), normalization(isGraduate), normalization(isEmployed),
                    normalization(income.toFloat()), normalization(coApplicantIncome.toFloat()), normalization(loanAmount.toFloat()), normalization(term.toInt()),
                    normalization(history.toInt()), normalization(area), label)



            }.toList()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Firebase.firestore
        var k = 0
        val viewCluster = findViewById<TextView>(R.id.viewCluster)
        var dataBaru: LoanData = LoanData("LP001000", "Male", 0.0, 0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0, 0.0, 0.0, 0)
        //RADIO BUTTONS
        var rbGender1 = findViewById<RadioButton>(R.id.rb11)
        var rbGender2 = findViewById<RadioButton>(R.id.rb12)
        var rbMarried1 = findViewById<RadioButton>(R.id.rb21)
        var rbMarried2 = findViewById<RadioButton>(R.id.rb22)
        var rbLulus1 = findViewById<RadioButton>(R.id.rb31)
        var rbLulus2 = findViewById<RadioButton>(R.id.rb32)
        var rbEmployed1 = findViewById<RadioButton>(R.id.rb41)
        var rbEmployed2 = findViewById<RadioButton>(R.id.rb42)
        var rbArea1 = findViewById<RadioButton>(R.id.rb51)
        var rbArea2 = findViewById<RadioButton>(R.id.rb52)
        var rbArea3 = findViewById<RadioButton>(R.id.rb53)

        //EDIT TEXTS
        var editTanggungan = findViewById<EditText>(R.id.editDependents)

        var editIncome = findViewById<EditText>(R.id.editIncome)
        var editCIncome = findViewById<EditText>(R.id.editCIncome)
        var editKredit = findViewById<EditText>(R.id.editKredit)
        var editWaktu = findViewById<EditText>(R.id.editWaktu)
        var editHistory = findViewById<EditText>(R.id.editHistory)
        var editD = findViewById<EditText>(R.id.editD)
        var editMinSize = findViewById<EditText>(R.id.editMinSize)

        //BUTTONS
        val submit = findViewById<Button>(R.id.buttonSubmit)
        val clusterButton = findViewById<Button>(R.id.buttonCluster)
        val writeData = findViewById<Button>(R.id.buttonWrite)




        val inputStream = resources.openRawResource(R.raw.data)
        val loanInfo = readCsv(inputStream)
        for(i in loanInfo){
            db.collection("tbML").document(i.id).set(i)
        }
        submit.setOnClickListener {
            var dependents = editTanggungan.text.toString().toInt()
            if (dependents >= 3) {
                dependents = 3
            }
            val idNum = loanInfo.last().id.takeLast(4).toInt() + 1
            val id = "LP00$idNum"
            var genderInput = "Female"

            if (rbGender2.isChecked) {
                genderInput = "Male"
            }
            var marriage = 0
            if (rbMarried1.isChecked) {
                marriage = 1
            }
            var educated = 0
            if (rbLulus1.isChecked) {
                educated = 1
            }
            var employed = 0
            if (rbEmployed1.isChecked) {
                employed = 1
            }
            var tempatTinggal = 0
            if (rbArea2.isChecked) {
                tempatTinggal = 1
            } else if (rbArea3.isChecked) {
                tempatTinggal = 2
            }
            val editK = findViewById<EditText>(R.id.editK)
            if(! editK.text.isBlank()) {
                k = editK.text.toString().toInt()
            }
            else {
                k = 3
            }
           dataBaru = LoanData(
                id,
                genderInput,
                normalization(marriage),
                normalization(dependents),
                normalization(educated),
                normalization(employed),
                normalization(editIncome.text.toString().toFloat()),
                normalization(editCIncome.text.toString().toFloat()),
                normalization(editKredit.text.toString().toFloat()),
                normalization(editWaktu.text.toString().toInt()),
                normalization(editHistory.text.toString().toInt()),
                normalization(tempatTinggal),
                0
            )

            val hasil = kNN(dataBaru, loanInfo, k)
           if(hasil){
               viewCluster.setText("Anda berhak menerima pinjaman")
               dataBaru.label = 1
           }else{
               viewCluster.setText("Anda tidak berhak menerima pinjaman")
               dataBaru.label = 0
           }


        }
        clusterButton.setOnClickListener {
            val arrayClusters = GDBSCAN(loanInfo, editD.text.toString().toInt(),editMinSize.text.toString().toInt())
            viewCluster.setText(arrayClusters.toString())

        }
        writeData.setOnClickListener {
            db.collection("tbML").document(dataBaru.id).set(dataBaru)
        }

    }

    fun euclidean(data1: LoanData, data2: LoanData): Double{
        return sqrt((data1.marriageStatus - data2.marriageStatus).toDouble().pow(2) + (data1.dependents - data2.dependents).toDouble().pow(2) + (data1.education - data2.education).toDouble().pow(2) +
                (data1.employment - data2.employment).toDouble().pow(2) + (data1.income - data2.income).toDouble().pow(2) + (data1.coApplicantIncome - data2.coApplicantIncome).toDouble().pow(2) +
                (data1.loanAmount - data2.loanAmount).toDouble().pow(2) + (data1.loanTerm - data2.loanTerm).toDouble().pow(2) + (data1.creditHistory - data2.creditHistory).toDouble().pow(2) +
                (data1.propertyArea - data2.propertyArea).toDouble().pow(2))
    }
    fun kNN(dataTest: LoanData, arrayTraining: List<LoanData>, k: Int): Boolean{
        var listDist = mutableListOf<Double>()
        for(i in arrayTraining){
            var data = i
            listDist.add(euclidean(data, dataTest))

        }
        listDist.sort()
        var list = listDist.take(k)
        Log.d("debugList", list.toString())
        var listElement = mutableListOf<LoanData>()

        var listD = mutableListOf<Double>()

        var visited = mutableListOf<LoanData>()
        for(i in 0 until list.size){

            for(j in arrayTraining){
                    if(euclidean(j, dataTest) == list[i] && !(visited.contains(j))){
                        listElement.add(j)
                        listD.add(1/(euclidean(j, dataTest).pow(2)))
                        visited.add(j)
                    }
            }
        }
        Log.d("debugList", listElement.toString())
        Log.d("debugList", listD.toString())
        var labelYes = 0.0
        var labelNo = 0.0
        for(i in 0 until listElement.size){
            Log.d("label", listElement.get(i).label.toString())
            if(listElement.get(i).label == 0){
                labelNo += listD.get(i)
            }else{
                labelYes += listD.get(i)
            }
        }
        Log.d("debugY", labelYes.toString())
        Log.d("debugY", labelNo.toString())
        return labelYes > labelNo

    }
    fun digit(value: Int): Int{
        return value.toString().length
    }
    fun GDBSCAN(data: List<LoanData>, d: Int, minSize: Int): HashMap<String, MutableList<MutableList<LoanData>>>{
        var clusters = mutableListOf<MutableList<LoanData>>()
        var noise = mutableListOf<MutableList<LoanData>>()
        var visited = mutableListOf<Int>()
        for(i in 0 until 50){
            if(!(visited.contains(i))) {
                var cluster = mutableListOf<LoanData>()
                cluster.add(data.get(i))
                for (j in 0 until 50) {
                    if (i != j && !(visited.contains(j))) {

                        var distance = euclidean(data.get(i), data.get(j))
                        if (distance <= d) {
                            cluster.add(data.get(j))
                            visited.add(j)
                        }

                    }
                }

                if (cluster.size >= minSize) {
                    clusters.add(cluster.distinct() as MutableList<LoanData>)
                } else {
                    noise.add(cluster.distinct() as MutableList<LoanData>)
                }
                Log.d("debug", cluster.toString())
            }

        }
        return hashMapOf(
            "Cluster" to clusters,
            "Noise data" to noise
        )
    }
    fun training(list: List<LoanData>, k: Int): Double{
        var correct = 0
        for(i in list){
            var res = kNN(i, list, k)
            var label = false
            if(i.label == 1){
                label = true
            }
            if(label == res){
                correct++
            }
        }
        return (correct.toDouble() / list.size.toDouble()) * 100.0

    }
    fun normalization(value: Int): Double{
        var normal = 0.0
        if(value == 0){
            normal = value / (10.0.pow(1))
        }else {
            normal = value / (10.0.pow(digit(value)))
        }
        return normal
    }
    fun normalization(value: Float): Float{
        var normal = 0.0
        if(value == 0.0f){
            normal = value / (10.0.pow(1))
        }else {
            normal = value / (10.0.pow(digit(value.toInt())))
        }
        return normal.toFloat()
    }
}

operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]
operator fun <T> List<T>.component8() = this[7]
operator fun <T> List<T>.component9() = this[8]
operator fun <T> List<T>.component10() = this[9]
operator fun <T> List<T>.component11() = this[10]
operator fun <T> List<T>.component12() = this[11]
operator fun <T> List<T>.component13() = this[12]
operator fun <T> List<T>.component14() = this[13]
operator fun <T> List<T>.component15() = this[14]
operator fun <T> List<T>.component16() = this[15]
operator fun <T> List<T>.component17() = this[16]
operator fun <T> List<T>.component18() = this[17]

