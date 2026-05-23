package com.sincronizador.domain.model;
import com.sincronizador.domain.valueobject.Tipo;

public class Produto {

    private final String clube;
    private final String modelo;
    private final Tipo tipo;

    public Produto(String clube, String modelo, Tipo tipo) {
        this.clube = clube;
        this.modelo = modelo;
        this.tipo = tipo;
    }

    public String getClube() {
        return clube;
    }

    public String getModelo() {
        return modelo;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
