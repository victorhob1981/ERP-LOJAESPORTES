package com.sincronizador.application.dto;

public enum EstadoProdutoCatalogo {

    OK,                 // Tem imagem e tamanhos corretos
    DESATUALIZADO,      // Tem imagem, mas tamanhos divergentes
    SEM_IMAGEM,         // Existe no ERP, mas não no catálogo
    ORFAO               // Existe no catálogo, mas não no ERP
}
