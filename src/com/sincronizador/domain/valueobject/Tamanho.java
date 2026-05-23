package com.sincronizador.domain.valueobject;

public enum Tamanho {

    // Adulto
    P,
    M,
    G,
    GG,
    _2GG,
    _3GG,
    _4GG,

    // Infantil
    _16,
    _18,
    _20,
    _22,
    _24,
    _26,
    _28;

    
    public String getValorExibicao() {
        if (name().startsWith("_")) {
            return name().substring(1);
        }
        return name();
    }
}
