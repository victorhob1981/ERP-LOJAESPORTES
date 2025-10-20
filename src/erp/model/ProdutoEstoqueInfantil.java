package erp.model;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProdutoEstoqueInfantil {
    private final StringProperty modelo;
    private final StringProperty clube;
    private final StringProperty tipo;
    private final IntegerProperty quantidadeTotal;
    
    // Propriedades para tamanhos numéricos
    private final Map<String, IntegerProperty> quantidadesPorTamanho = new HashMap<>();

    public ProdutoEstoqueInfantil(String modelo, String clube, String tipo) {
        this.modelo = new SimpleStringProperty(modelo);
        this.clube = new SimpleStringProperty(clube);
        this.tipo = new SimpleStringProperty(tipo);
        this.quantidadeTotal = new SimpleIntegerProperty(0);
        
        // Inicializa as propriedades para os tamanhos infantis
        String[] tamanhos = {"16", "18", "20", "22", "24", "26", "28"};
        for (String tamanho : tamanhos) {
            quantidadesPorTamanho.put(tamanho, new SimpleIntegerProperty(0));
        }
    }

    // Getters e Properties
    public String getModelo() { return modelo.get(); }
    public StringProperty modeloProperty() { return modelo; }
    public String getClube() { return clube.get(); }
    public StringProperty clubeProperty() { return clube; }
    public String getTipo() { return tipo.get(); }
    public StringProperty tipoProperty() { return tipo; }
    public int getQuantidadeTotal() { return quantidadeTotal.get(); }
    public IntegerProperty quantidadeTotalProperty() { return quantidadeTotal; }

    // Métodos para acessar quantidades por tamanho
    public IntegerProperty quantidade16Property() { return quantidadesPorTamanho.get("16"); }
    public IntegerProperty quantidade18Property() { return quantidadesPorTamanho.get("18"); }
    public IntegerProperty quantidade20Property() { return quantidadesPorTamanho.get("20"); }
    public IntegerProperty quantidade22Property() { return quantidadesPorTamanho.get("22"); }
    public IntegerProperty quantidade24Property() { return quantidadesPorTamanho.get("24"); }
    public IntegerProperty quantidade26Property() { return quantidadesPorTamanho.get("26"); }
    public IntegerProperty quantidade28Property() { return quantidadesPorTamanho.get("28"); }
    
    public void setQuantidadeParaTamanho(String tamanho, int quantidade) {
        if (tamanho != null && quantidadesPorTamanho.containsKey(tamanho.trim())) {
            quantidadesPorTamanho.get(tamanho.trim()).set(quantidade);
            recalcularTotal();
        }
    }

    private void recalcularTotal() {
        int total = quantidadesPorTamanho.values().stream()
                                         .mapToInt(IntegerProperty::get)
                                         .sum();
        this.quantidadeTotal.set(total);
    }
}