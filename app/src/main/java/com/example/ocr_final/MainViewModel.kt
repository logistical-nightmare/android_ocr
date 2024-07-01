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
import com.example.ocr_final.Constants




/**
 * Data class representing match data with time, vendor, and inhouse information.
 *
 * @property time The time when the match data was created.
 * @property vendor The vendor information.
 * @property inhouse The inhouse information.
 */
data class MatchData (val time: String, val vendor: String, val inhouse: String)

/**
 * ViewModel for managing OCR and match data operations.
 */
class MainViewModel : ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

    private val _vendor = MutableStateFlow("")
    val vendor = _vendor.asStateFlow()

    private val _inhouse = MutableStateFlow("")
    val inhouse = _inhouse.asStateFlow()

    private val _tryAgain = MutableStateFlow(false)
    val tryAgain = _tryAgain.asStateFlow()

    private val _state = MutableStateFlow("Capture Vendor")
    val state = _state.asStateFlow()

    private val _noScanned = MutableStateFlow(0)
    val noScanned = _noScanned.asStateFlow()

    private val _codes = MutableStateFlow<List<String>>(emptyList())
    val codes = _codes.asStateFlow()

    private val _hasMultipleCodes = MutableStateFlow(false)
    val hasMultipleCodes = _hasMultipleCodes.asStateFlow()

    private val _matchDataList = MutableStateFlow<List<MatchData>>(emptyList())
    val matchDataList = _matchDataList.asStateFlow()

    private fun clearCodes() {
        _codes.value = emptyList()
    }

    fun getCodes(): List<String> {
        return _codes.value
    }

    fun addCode(code: String) {
        val updatedCodes = _codes.value.toMutableList().apply {
            add(code)
        }
        _codes.value = updatedCodes
    }

    /**
     * Enables selecting of code when multiple codes are detected
     * Sets the selected code into the Vendor or Inhouse Label
     * @param selectedCode selected code from the array
     */
    fun onCodeSelected(selectedCode: String) {
        if(selectedCode.isNotEmpty()) {
            if (_state.value == "Capture Vendor") {
                if (selectedCode in _codes.value) {
                    _state.value = "Capture Inhouse"
                    _vendor.value = selectedCode
                    _tryAgain.value = false

                } else {
                    _tryAgain.value = true
                }
            } else if (_state.value == "Capture Inhouse") {
                if (selectedCode in _codes.value) {
                    _inhouse.value = selectedCode
                    _state.value = "Show Match Percentage"
                    _tryAgain.value = false
                }
                else {
                    _tryAgain.value = true
                }
            }
        }
        else {
            _tryAgain.value = true
        }
        _hasMultipleCodes.value = false
    }

    /**
     * Activated when the code selected popup is dismissed
     */
    fun dismissCodeSelection() {
        _tryAgain.value = true
        _hasMultipleCodes.value = false
    }

    /**
    * Enables selecting of code when multiple codes are detected
     * Sets the selected code into the Vendor or Inhouse Label
     * @param index index of the selected code from the array
     */
    fun selectCode(index: Int) {
        if (_state.value == "Capture Vendor") {
            if (index in _codes.value.indices) {
                _state.value = "Capture Inhouse"
                _vendor.value = _codes.value[index]
                _tryAgain.value = false

            } else {
                _tryAgain.value = true
            }
        } else if (_state.value == "Capture Inhouse") {
            if (index in _codes.value.indices) {
                _inhouse.value = _codes.value[index]
                _state.value = "Show Match Percentage"
                _tryAgain.value = false
            }
             else {
                _tryAgain.value = true
            }
        }
    }

    /**
     * Adds code to the match list data to be exported as csv later
     *
     * @param vendor vendor code of the match
     * @param inhouse inhouse code of the ,atch
     */
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



    /**
     * Prints the match data list to the console.
     */
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

    /**
     * Shares the match data list as a CSV file.
     *
     * @param context The context to use for sharing.
     */
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

    /**
     * Resets the vendor and inhouse information and updates the state.
     */
    fun resetVendorAndInhouse() {
        viewModelScope.launch {
            _vendor.value = ""
            _inhouse.value = ""
            _state.value = "Capture Vendor"
            _hasMultipleCodes.value = false
        }
    }

    /**
     * Increments the count of scanned items.
     */
    fun addNoScanned() {
        viewModelScope.launch(){
            _noScanned.value += 1
        }
    }

    /**
     * Reverts the state to the previous one and clears the relevant data.
     */
    fun undoState() {
        viewModelScope.launch(){

            if (_state.value == "Capture Inhouse") {
                _vendor.value = ""
                _state.value = "Capture Vendor"
                _tryAgain.value = false
            }
            else if (_state.value== "Show Match Percentage") {
                _inhouse.value = ""
                _state.value = "Capture Inhouse"
                _tryAgain.value = false
            }
        }
    }

    /**
     * Adds a new bitmap to the list of bitmaps.
     *
     * @param bitmap The bitmap to add.
     */
    fun onTakePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _bitmaps.value += bitmap
        }
    }

    /**
     * Extracts text from a given bitmap using ML Kit's Text Recognition.
     *
     * @param bitmap The bitmap to extract text from.
     */
    fun extractTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        clearCodes()

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


    /**
     * Modifies the extracted text to identify and store vendor or inhouse information based on the current state.
     *
     * @param originalText The extracted text to modify.
     */
    fun modifyText(originalText: String) {
        val lines = originalText.lines()
        // Keep the original regex but adjust it to account for lines that contain only the code.
        val codeRegex = Constants.REGEX.toRegex() //regex pattern from constants file
        var highestMatchPercentage = -1.0
        var bestMatch = ""

        val keywords = when (_state.value) {
            "Capture Vendor" -> Constants.vend_list
            "Capture Inhouse" -> Constants.inhouse_list
            else -> listOf("")
        }

        for (line in lines) {
            val matches = codeRegex.findAll(line.trim())
            for (match in matches) {
                val code = match.groupValues[1]
                addCode(code)
                if (!code.any { it.isDigit() }) {
                    continue
                }
                Log.d("code", code)
                val matchPercentage = calculateHighestMatchPercentage(line, keywords)
                Log.d("matching", "$line $keywords $matchPercentage")
                if (matchPercentage >= highestMatchPercentage) {
                    highestMatchPercentage = matchPercentage
                    bestMatch = code
                }
            }
        }

        if(highestMatchPercentage < 10.0 && _codes.value.size>1) _hasMultipleCodes.value = true

        if (!_hasMultipleCodes.value) {
            if (_state.value == "Capture Vendor") {
                _vendor.value = bestMatch
                if (bestMatch.isNotEmpty()) {
                    _state.value = "Capture Inhouse"
                    _tryAgain.value = false
                } else {
                    _tryAgain.value = true
                }
            } else if (_state.value == "Capture Inhouse") {
                _inhouse.value = bestMatch
                if (bestMatch.isNotEmpty()) {
                    _state.value = "Show Match Percentage"
                    _tryAgain.value = false
                } else {
                    _tryAgain.value = true
                }
            }
        }
    }

    /**
     * Calculates the highest match percentage between a code and a list of keywords.
     *
     * @param code The code to compare.
     * @param keywords The list of keywords to compare against.
     * @return The highest match percentage.
     */
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
