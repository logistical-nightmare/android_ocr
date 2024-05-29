package com.example.ocr_final

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider





data class MatchData (val time: String, val vendor: String, val inhouse: String)
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

    private val _noScanned = MutableStateFlow(0)
    val noScanned = _noScanned.asStateFlow()

    private val _matchDataList = MutableStateFlow<List<MatchData>>(emptyList())
    val matchDataList = _matchDataList.asStateFlow()


    @RequiresApi(Build.VERSION_CODES.O)
    fun addToMatchDataList(vendor: String, inhouse: String) {
        // Get the current date and time
        val currentDateTime = LocalDateTime.now()

        // Format the current date and time
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        // Create a new MatchData object with the current date and time
        val newMatchData = MatchData(formattedDateTime, vendor, inhouse)

        // Add the new MatchData to the existing list
        val currentList = _matchDataList.value.toMutableList()
        currentList.add(newMatchData)

        // Update the _matchDataList with the updated list
        _matchDataList.value = currentList.toList()

    }

    fun printMatchDataList() {
        val currentList = _matchDataList.value

        if (currentList.isEmpty()) {
            println("MatchData list is empty.")
            return
        }

        println("Printing MatchData list:")
        currentList.forEachIndexed { index, matchData ->
            println("Item ${index + 1}: Time=${matchData.time}, Vendor=${matchData.vendor}, Inhouse=${matchData.inhouse}")
        }
    }

    fun shareMatchDataList(context: Context) {
        val currentList = _matchDataList.value

        if (currentList.isEmpty()) {
            println("MatchData list is empty. Nothing to share.")
            return
        }

        val csvData = StringBuilder()
        // Write CSV header
        csvData.append("Time,Vendor,Inhouse\n")

        // Write each MatchData item as a new line in the CSV
        currentList.forEach { matchData ->
            csvData.append("${matchData.time},${matchData.vendor},${matchData.inhouse}\n")
        }

        try {
            // Save CSV data to a temporary file
            val file = File(context.cacheDir, "match_data.csv")
            file.writeText(csvData.toString())

            // Get URI for the file
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Your provider authority
                file
            )

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Launch the share menu
            context.startActivity(Intent.createChooser(shareIntent, "Share Match Data"))
        } catch (ex: Exception) {
            println("Error sharing MatchData list: ${ex.message}")
        }
    }

    fun resetVendorAndInhouse() {
        viewModelScope.launch {
            _vendor.value = ""
            _inhouse.value = ""
            _state.value = 1
        }
    }

    fun addNoScanned() {
        viewModelScope.launch(){
            _noScanned.value += 1
        }
    }

    fun undoState() {
        viewModelScope.launch(){

            if (_state.value == 2) {
                _vendor.value = ""
                _state.value -= 1
                _tryAgain.value = false
            }
            else if (_state.value== 3) {
                _inhouse.value = ""
                _state.value -= 1
                _tryAgain.value = false
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
        val codeRegex = "(?:.*?:\\s*|\\s+)?(?=.*\\d)([\\w\\d-]{8,})\\b".toRegex()
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
