/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.paymentchain.customer.entities;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.Data;

/**
 *
 * @author sotobotero
 */

//Simplemente aqui se va a conocer que tipo de variables va a ver en la tabla de BD
@Entity
@Data
public class Customer {
   @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
   private long id;
   private String code;
   private String name;
   private String phone;   
    private String iban;
    private String surname; 
    private String address;    
    
    //One to many indica que hay relacion de uno a muchos 
    //FEtch unicamente es para cargar por primera vez
    //mappedBy es la que indica quien es encargada de la relacion 
    //Cascade es por si una vez borras un cliente se borra con todo y sus productos
    //Orphan indica que si un producto no esta relacionado con cliente, sea eliminado
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)   
    private List<CustomerProduct> products;
    
    
    
    @Transient
    private List<?> transactions;
   
}
