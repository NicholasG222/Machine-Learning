package com.example.proyekmlkel6

data class LoanData(
    val id: String,
    val gender: String,
    val marriageStatus: Double,
    var dependents: Double,
    var education: Double,
    var employment: Double,
    var income: Float,
    var coApplicantIncome: Float,
    var loanAmount: Float,
    var loanTerm: Double,
    var creditHistory: Double,
    var propertyArea: Double,
    var label: Int
)
