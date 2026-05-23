package ru.sla.clarify.lib.google.firestore.codec

/**
 * Бросается, когда Firestore-документ нарушает ожидаемую схему: обязательное поле
 * отсутствует, значение поля имеет тип, отличный от ожидаемого, или enum-литерал не
 * распознан. Это всегда data-integrity bug (десинхронизация клиента и схемы документа),
 * а не runtime-ситуация, которую должен обрабатывать call site, — но мы делаем отдельный
 * тип, чтобы такие ошибки можно было отличить от случайного NPE/ClassCastException в
 * логах/крашлитике.
 *
 * Конструктор приватный — call site передаёт только аргументы через фабричные методы;
 * формирование сообщения живёт здесь, чтобы формат был единым.
 */
class DataMappingException private constructor(message: String) : RuntimeException(message) {

  companion object {

    fun missingField(key: String): DataMappingException {
      return DataMappingException("missing required field '$key'")
    }

    fun wrongType(key: String, expected: String, actual: Any?): DataMappingException {
      return DataMappingException("field '$key' expected $expected, got ${actual.typeName()}")
    }

    fun unknownEnumValue(serialName: String, value: String): DataMappingException {
      return DataMappingException("unknown enum value '$value' for $serialName")
    }

    fun rootMustBeStructure(): DataMappingException {
      return DataMappingException("FirestoreFormat: root value must be a structured type")
    }

    private fun Any?.typeName(): String = this?.let { it::class.simpleName } ?: "null"
  }
}
