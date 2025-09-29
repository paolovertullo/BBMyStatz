package com.mapovich.bbmystatz.ui.classifica

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ClassificaViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Classifica U13 2024 2025"
    }
    val text: LiveData<String> = _text
}