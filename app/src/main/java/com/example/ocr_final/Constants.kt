package com.example.ocr_final

class Constants {
    companion object {
        const val REGEX =  "(?:.*?:\\s*|\\s+)?(?=.*\\d)([\\w\\d-]{8,})\\b"; // refer to regex documentation
        val vend_list = listOf("batch", "lot", "p.o.");
        val inhouse_list = listOf("vend")
    }
}
