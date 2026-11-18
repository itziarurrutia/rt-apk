package es.unavarra.rtapk.domain;

/** Resultado de búsqueda: el asset y el campo por el que ha coincidido el texto buscado. */
public record AssetMatch(AssetSearchField matchedBy, Asset asset) {
}
