package com.example.ocr_final

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

    private val _vendor = MutableStateFlow("")
    val vendor = _vendor.asStateFlow()

    private val _inhouse = MutableStateFlow("")
    val inhouse = _inhouse.asStateFlow()

    private val _tryAgain = MutableStateFlow(false)
    val tryAgain = _tryAgain.asStateFlow()

    private val _state = MutableStateFlow(1)
    val state = _state.asStateFlow()

    fun resetVendorAndInhouse() {
        viewModelScope.launch {
            _vendor.value = ""
            _inhouse.value = ""
            _state.value = 1
        }
    }

    fun undoState() {
        viewModelScope.launch(){

            if (_state.value == 2) {
                _vendor.value = ""
                _state.value -= 1
            }
            else if (_state.value== 3) {
                _inhouse.value = ""
                _state.value -= 1
            }
        }
    }

    fun onTakePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _bitmaps.value += bitmap
        }
    }

    fun extractTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Log.d("text", "Extracted text: $extractedText")

                modifyText(extractedText)
            }
            .addOnFailureListener { e ->
                Log.d("text", "Text recognition failed: $e")
            }
    }



    fun modifyText(originalText: String) {
        val lines = originalText.lines()
        // Keep the original regex but adjust it to account for lines that contain only the code.
        val codeRegex = "(?:.*?:\\s*|\\s+)?(?=.*\\d)([\\w\\d]{10,})\\b".toRegex()
        var highestMatchPercentage = 0.0
        var bestMatch = ""

        val keywords = when (_state.value) {
            1 -> listOf("batch", "lot", "p.o.")
            2 -> listOf("vend")
            else -> listOf("")
        }

        for (line in lines) {
            val matches = codeRegex.findAll(line.trim())
            for (match in matches) {
                val code = match.groupValues[1]
                val matchPercentage = calculateHighestMatchPercentage(line, keywords)
                if (matchPercentage >= highestMatchPercentage) {
                    highestMatchPercentage = matchPercentage
                    bestMatch = code
                }
            }
        }

        if (_state.value == 1) {
            _vendor.value = bestMatch
            if (bestMatch.isNotEmpty()) {
                _state.value = 2
                _tryAgain.value = false
            } else {
                _tryAgain.value = true
            }
        } else if (_state.value == 2) {
            _inhouse.value = bestMatch
            if (bestMatch.isNotEmpty()) {
                _state.value = 3
                _tryAgain.value = false
            } else {
                _tryAgain.value = true
            }
        }
    }
    private fun calculateHighestMatchPercentage(code: String, keywords: List<String>): Double {
        if (keywords.isEmpty()) return 0.0

        var highestMatchPercentage = 0.0

        for (keyword in keywords) {
            if (keyword.isNotEmpty()) {
                val keywordChars = keyword.toCharArray().toSet()
                val codeChars = code.toCharArray().toSet()

                val intersection = keywordChars.intersect(codeChars).size
                val union = keywordChars.union(codeChars).size

                val matchPercentage =
                    if (union == 0) 0.0 else (intersection.toDouble() / union) * 100

                if (matchPercentage > highestMatchPercentage) {
                    highestMatchPercentage = matchPercentage
                }
            }
        }

        return highestMatchPercentage
    }
}
