package ru.sla.clarify.lib.google.firestore.codec

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Контракт между [FirestoreFormat] и кастомными сериализаторами Firestore-нативных
 * типов ([com.google.firebase.Timestamp], `DocumentReference`, `GeoPoint`, `Blob`,
 * ...). Стандартные Encoder/Decoder из kotlinx.serialization умеют только примитивы
 * (String/Long/Int/Double/Boolean/Char) — для Firestore-нативных значений мы передаём
 * их «опаково» через эти методы, и encoder кладёт их в `Map<String, Any?>` как есть,
 * а Firestore SDK уже знает что с ними делать.
 */
internal interface FirestoreOpaqueEncoder : Encoder {
  /** Записать значение «как есть» — без дальнейшей сериализации. */
  fun encodeOpaque(value: Any)
}

internal interface FirestoreOpaqueDecoder : Decoder {
  /** Достать значение «как есть» — без дальнейшей десериализации. */
  fun decodeOpaque(): Any?
}
