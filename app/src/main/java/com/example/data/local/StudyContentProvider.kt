package com.example.data.local

import android.content.Context
import com.example.data.model.DeckInfo
import com.example.data.model.GroupInfo
import com.example.data.model.StudyCard
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object StudyContentProvider {

  private var decks: List<DeckInfo> = emptyList()
  private var groups: List<GroupInfo> = emptyList()
  private var cards: List<StudyCard> = emptyList()
  private var cardsById: Map<String, StudyCard> = emptyMap()
  private var cardsByGroup: Map<String, List<StudyCard>> = emptyMap()
  private var cardsByDeck: Map<String, List<StudyCard>> = emptyMap()
  private var isInitialized = false

  fun initialize(context: Context) {
    if (isInitialized) return
    synchronized(this) {
      if (isInitialized) return

      try {
        // 1. Load Decks & Groups
        val decksJsonString = readAsset(context, "study_decks.json")
        val rootObj = JSONObject(decksJsonString)

        val decksArray = rootObj.getJSONArray("decks")
        val deckList = mutableListOf<DeckInfo>()
        for (i in 0 until decksArray.length()) {
          val obj = decksArray.getJSONObject(i)
          deckList.add(
            DeckInfo(
              deckId = obj.getString("deckId"),
              subjectId = obj.getString("subjectId"),
              subjectTitle = obj.getString("subjectTitle"),
              deckTitle = obj.getString("deckTitle"),
              type = obj.getString("type"),
              totalCards = obj.getInt("totalCards"),
              totalGroups = obj.getInt("totalGroups")
            )
          )
        }
        decks = deckList

        val groupsArray = rootObj.getJSONArray("groups")
        val groupList = mutableListOf<GroupInfo>()
        for (i in 0 until groupsArray.length()) {
          val obj = groupsArray.getJSONObject(i)
          groupList.add(
            GroupInfo(
              groupId = obj.getString("groupId"),
              deckId = obj.getString("deckId"),
              groupIndex = obj.getInt("groupIndex"),
              groupTitle = obj.getString("groupTitle"),
              totalCards = obj.getInt("totalCards")
            )
          )
        }
        groups = groupList

        // 2. Load Cards
        val cardsJsonString = readAsset(context, "study_cards.json")
        val cardArray = JSONArray(cardsJsonString)
        val cardList = ArrayList<StudyCard>(cardArray.length())
        for (i in 0 until cardArray.length()) {
          val obj = cardArray.getJSONObject(i)
          cardList.add(
            StudyCard(
              id = obj.getString("id"),
              deckId = obj.getString("deckId"),
              groupId = obj.getString("groupId"),
              subjectId = obj.getString("subjectId"),
              prompt = obj.getString("prompt"),
              answer = obj.getString("answer"),
              type = obj.getString("type")
            )
          )
        }
        cards = cardList
        cardsById = cardList.associateBy { it.id }
        cardsByGroup = cardList.groupBy { it.groupId }
        cardsByDeck = cardList.groupBy { it.deckId }

        isInitialized = true
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun readAsset(context: Context, filename: String): String {
    val inputStream = context.assets.open(filename)
    val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
    val sb = StringBuilder()
    var line: String? = reader.readLine()
    while (line != null) {
      sb.append(line)
      line = reader.readLine()
    }
    reader.close()
    return sb.toString()
  }

  fun getDecks(): List<DeckInfo> = decks

  fun getGroups(): List<GroupInfo> = groups

  fun getDeck(deckId: String): DeckInfo? = decks.firstOrNull { it.deckId == deckId }

  fun getGroup(groupId: String): GroupInfo? = groups.firstOrNull { it.groupId == groupId }

  fun getGroupsForDeck(deckId: String): List<GroupInfo> = groups.filter { it.deckId == deckId }

  fun getCardsForGroup(groupId: String): List<StudyCard> = cardsByGroup[groupId] ?: emptyList()

  fun getCardsForDeck(deckId: String): List<StudyCard> = cardsByDeck[deckId] ?: emptyList()

  fun getCardById(cardId: String): StudyCard? = cardsById[cardId]

  fun getAllCards(): List<StudyCard> = cards

  fun searchCards(query: String, subjectFilter: String? = null, deckFilter: String? = null, limit: Int = 100): List<StudyCard> {
    if (query.isBlank() && subjectFilter == null && deckFilter == null) return emptyList()
    val cleanQuery = query.trim().lowercase()
    return cards.asSequence()
      .filter { card ->
        (subjectFilter == null || card.subjectId == subjectFilter) &&
        (deckFilter == null || card.deckId == deckFilter) &&
        (cleanQuery.isEmpty() || card.prompt.lowercase().contains(cleanQuery) || card.answer.lowercase().contains(cleanQuery))
      }
      .take(limit)
      .toList()
  }

  /**
   * Generates distractors for a quiz question from the same deck or group
   */
  fun getDistractorsForCard(correctCard: StudyCard, count: Int = 3): List<String> {
    val candidates = (cardsByDeck[correctCard.deckId] ?: cards)
      .filter { it.id != correctCard.id && it.answer.isNotBlank() && it.answer != correctCard.answer }
      .map { it.answer }
      .distinct()
      .shuffled()

    return candidates.take(count)
  }
}
