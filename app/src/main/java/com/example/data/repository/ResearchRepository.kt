package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.dao.ConferenceDao
import com.example.data.dao.PaperDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.ConferenceEntity
import com.example.data.model.PaperEntity
import com.example.data.model.UserProfileEntity
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ResearchRepository(
    private val paperDao: PaperDao,
    private val conferenceDao: ConferenceDao,
    private val userProfileDao: UserProfileDao
) {
    val allPapers: Flow<List<PaperEntity>> = paperDao.getAllPapers()
    val allConferences: Flow<List<ConferenceEntity>> = conferenceDao.getAllConferences()
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    suspend fun getPaperById(id: Int): PaperEntity? = paperDao.getPaperById(id)

    suspend fun insertConference(conference: ConferenceEntity) = withContext(Dispatchers.IO) {
        conferenceDao.insertConference(conference)
    }

    suspend fun toggleConferenceFavorite(conference: ConferenceEntity) = withContext(Dispatchers.IO) {
        conferenceDao.updateConference(conference.copy(isFavorite = !conference.isFavorite))
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        userProfileDao.insertProfile(profile)
    }

    suspend fun deletePaper(paper: PaperEntity) = withContext(Dispatchers.IO) {
        paperDao.deletePaper(paper)
    }

    suspend fun clearPaperHistory() = withContext(Dispatchers.IO) {
        paperDao.clearHistory()
    }

    suspend fun checkAndPrepopulateConferences() = withContext(Dispatchers.IO) {
        val existingConferences = allConferences.firstOrNull()
        if (existingConferences.isNullOrEmpty()) {
            val defaultChennaiConferences = listOf(
                ConferenceEntity(
                    name = "IEEE International Symposium on AI & Robotic Sensing",
                    acronym = "IEEE-ISARS 2026",
                    location = "IIT Madras Research Park, Taramani, Chennai",
                    dates = "October 12-14, 2026",
                    topics = "Artificial Intelligence, Robotic Vision, Smart Sensors, IoT In Chennai",
                    description = "A premier event co-organized by IIT Madras showcasing state-of-the-art advances in cognitive robotics and smart sensing systems, featuring panels from global researchers.",
                    websiteUrl = "https://iitm-isars2026.org",
                    submissionDeadline = "2026-08-01",
                    isFavorite = true
                ),
                ConferenceEntity(
                    name = "Chennai International Cybersecurity and Cryptography Summit",
                    acronym = "CICCS 2026",
                    location = "SRM University, Kattankulathur Campus, Chennai",
                    dates = "December 05-07, 2026",
                    topics = "Cryptography, Network Security, Blockchain, Cyber Forensics",
                    description = "Hosted at SRM Kattankulathur, CICCS focuses on next-generation security paradigms, cryptographic standards, and secure communication systems.",
                    websiteUrl = "https://srm-ciccs2026.edu.in",
                    submissionDeadline = "2026-09-15"
                ),
                ConferenceEntity(
                    name = "Annual National Conference on Advanced Computing Systems",
                    acronym = "NCACS 2026",
                    location = "Anna University, Guindy Campus, Chennai",
                    dates = "August 18-19, 2026",
                    topics = "High Performance Computing, Cloud Computing, Edge Intelligence",
                    description = "Anna University's flagship computing conference, uniting research scholars from across South India to discuss high-performance architectures and distributed cloud orchestration.",
                    websiteUrl = "https://annauniv.edu/ncacs2026",
                    submissionDeadline = "2026-06-30",
                    isFavorite = false
                ),
                ConferenceEntity(
                    name = "VIT Chennai International Symposium on Deep Learning & Vision",
                    acronym = "VITC-ISDLV 2026",
                    location = "VIT Chennai Campus, Vandalur-Kelambakkam Road, Chennai",
                    dates = "November 20-22, 2026",
                    topics = "Computer Vision, Generative AI, Medical Image Processing, Autonomous Vehicles",
                    description = "An in-depth workshop and research symposium focusing on convolutional and transformer networks applied to surveillance, driverless vehicles, and automated Chennai health diagnoses.",
                    websiteUrl = "https://chennai.vit.ac.in/isdlv2026",
                    submissionDeadline = "2026-09-01"
                ),
                ConferenceEntity(
                    name = "Madras University Research Expo on Mathematical Informatics",
                    acronym = "MUREMI 2026",
                    location = "University of Madras, Chepauk Campus, Chennai",
                    dates = "September 10-11, 2026",
                    topics = "Mathematical Modeling, Bioinformatics, Algorithmic Graph Theory",
                    description = "Celebrated at Madras University Chepauk, MUREMI highlights discrete mathematics, neural network algorithms, and data structures applied to statistical biology.",
                    websiteUrl = "https://unom.ac.in/muremi2026",
                    submissionDeadline = "2026-07-15"
                )
            )
            conferenceDao.insertConferences(defaultChennaiConferences)
        }
    }

    suspend fun analyzePlagiarismAndSave(
        title: String,
        authors: String,
        abstractContent: String,
        content: String
    ): PaperEntity = withContext(Dispatchers.IO) {
        val checkedDate = System.currentTimeMillis()
        val apiKey = BuildConfig.GEMINI_API_KEY

        val instructions = """
            You are an advanced academic plagiarism evaluator and similarity detector.
            Analyze the research paper title, abstract, and content provided. Perform an in-depth lexical and structural analysis, simulating database matching against millions of publications from IEEE Xplore, ACM Digital Library, Springer Link, Anna University Repositories, and generic web databases.
            
            You MUST respond with a single, strictly valid JSON object matching this exact schema:
            {
              "plagiarismPercentage": 24,
              "explanation": "Brief explanation summarizing where the major overlapping portions are found and what they are about (2-3 sentences).",
              "sources": [
                {
                  "name": "Name of publication/repository/website matched",
                  "percentage": 15,
                  "description": "Short explanation of which section or phrases matched this source."
                }
              ],
              "suggestions": [
                {
                  "originalText": "Full sentence from the input that has high similarity.",
                  "rewrittenText": "An ethically paraphrased, dynamic alternative sentence.",
                  "benefit": "Why the rephrased version is superior and how it corrects the similarity issue."
                }
              ]
            }
            
            Guidelines:
            - "plagiarismPercentage" MUST be an integer between 0 and 100. Be realistic and varied depending on the text complexity and standard expressions used.
            - Ensure "sources" list at least 1-3 sources if plagiarism percentage is > 0, otherwise return an empty array if plagiarism is 0%.
            - Provide 1-3 highly practical "suggestions" for rephrasing based on original text.
            - Return ONLY valid, parsaeable JSON. Do not wrap in markdown ```json or other text format, just return the raw JSON object.
        """.trimIndent()

        val prompt = """
            TITLE: $title
            AUTHORS: $authors
            ABSTRACT: $abstractContent
            CONTENT: $content
        """.trimIndent()

        // Create a temporary paper entity to show scanning status
        val paperId = paperDao.insertPaper(
            PaperEntity(
                title = title,
                authors = authors,
                abstractContent = abstractContent,
                fullContent = content,
                plagiarismPercentage = 0,
                matchedConference = "Evaluating...",
                matchSourcesJson = "[]",
                checkedDate = checkedDate,
                status = "Analyzing",
                aiExplanation = "Connecting to academic repository analyzer...",
                rephrasingSuggestionsJson = "[]"
            )
        ).toInt()

        Log.d("ResearchRepository", "Stored temporary scan paper with ID: $paperId")

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "$instructions\n\nINPUT CODES:\n$prompt")
                        )
                    )
                ),
                generationConfig = GeminiConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                )
            )

            val rawResponse = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from AI engine")

            Log.d("ResearchRepository", "Gemini Plagiarism Result Text: $jsonText")

            // Parse response json
            val adapter = RetrofitClient.moshi.adapter(PlagiarismAnalysisResponse::class.java)
            // Sometimes Gemini puts ```json ... ``` despite instructions. Clean it up just in case
            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val analysis = adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse JSON schema")

            // Determine matched conference dynamically based on content keywords
            val lowerContent = "$title $abstractContent $content".lowercase()
            val recommendedConf = when {
                lowerContent.contains("security") || lowerContent.contains("crypto") || lowerContent.contains("blockchain") -> "CICCS 2026 (Chennai)"
                lowerContent.contains("sensor") || lowerContent.contains("robot") || lowerContent.contains("iot") -> "IEEE-ISARS 2026 (IITM)"
                lowerContent.contains("deep learning") || lowerContent.contains("vision") || lowerContent.contains("image") -> "VITC-ISDLV 2026 (VIT Chennai)"
                lowerContent.contains("cloud") || lowerContent.contains("edge") || lowerContent.contains("parallel") -> "NCACS 2026 (Anna University)"
                else -> "MUREMI 2026 (University of Madras)"
            }

            val sourcesAdapter = RetrofitClient.moshi.adapter(List::class.java) // generic list representation
            val suggestionsAdapter = RetrofitClient.moshi.adapter(List::class.java)

            // Convert lists to JSON string for local storage representation
            val listSourcesJson = RetrofitClient.moshi.adapter(PlagiarismSource::class.java).let { itemAdapter ->
                "[" + analysis.sources.joinToString(",") { itemAdapter.toJson(it) } + "]"
            }
            val listSuggestionsJson = RetrofitClient.moshi.adapter(RephrasingSuggestion::class.java).let { itemAdapter ->
                "[" + analysis.suggestions.joinToString(",") { itemAdapter.toJson(it) } + "]"
            }

            val finalPaper = PaperEntity(
                id = paperId,
                title = title,
                authors = authors,
                abstractContent = abstractContent,
                fullContent = content,
                plagiarismPercentage = analysis.plagiarismPercentage,
                matchedConference = recommendedConf,
                matchSourcesJson = listSourcesJson,
                checkedDate = checkedDate,
                status = "Scanned",
                aiExplanation = analysis.explanation,
                rephrasingSuggestionsJson = listSuggestionsJson
            )

            paperDao.insertPaper(finalPaper)
            finalPaper

        } catch (e: Exception) {
            Log.e("ResearchRepository", "AI Scan Error: ", e)
            val failedPaper = PaperEntity(
                id = paperId,
                title = title,
                authors = authors,
                abstractContent = abstractContent,
                fullContent = content,
                plagiarismPercentage = 0,
                matchedConference = "Check Failed",
                matchSourcesJson = "[]",
                checkedDate = checkedDate,
                status = "Failed",
                aiExplanation = "Scan failed: ${e.localizedMessage ?: "Could not connect to Gemini API. Please make sure your GEMINI_API_KEY is configured in your Secrets panel."}",
                rephrasingSuggestionsJson = "[]"
            )
            paperDao.insertPaper(failedPaper)
            failedPaper
        }
    }
}
