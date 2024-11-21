/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.paymentchain.customer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentchain.customer.entities.Customer;
import com.paymentchain.customer.entities.CustomerProduct;
import com.paymentchain.customer.respository.CustomerRepository;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Collections;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 *
 * @author lalav
 */

/*Aqui todo se va a manejar lo que hay en el Swagger Ui, es decir todo lo que abra como controles
y se va a manejar para la base de datos
*/

//Este es el que da la indicacion de que todos los metodos se vuelvan controles

@RestController

//ESta indicacion es la que ayuda a la ruta, es decir nuestra ruta es customer 
@RequestMapping("/customer")


public class CustomerRestController {
    
    
    /*Autowired es el que se encarga de inyectar al objeto, es decir como el codigo de abajo que
    CustomerRepository, inyecta al otro pero en minusculas
    */
    @Autowired
    CustomerRepository customerRepository;
    
    
    
    
    //Este es el llamado para hacer el Web y y viene junto con su constructor
     private final WebClient.Builder webClientBuilder;
     
      public CustomerRestController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
      
      //EStas son las indicaciones que tiene la pagina para la espera
      //En caso de que haya un error no este esperando infinitamente
    HttpClient client = HttpClient.create()
            //Connection Timeout: is a period within which a connection between a client and a server must be established
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(EpollChannelOption.TCP_KEEPIDLE, 300)
            .option(EpollChannelOption.TCP_KEEPINTVL, 60)
            //Response Timeout: The maximun time we wait to receive a response after sending a request
            .responseTimeout(Duration.ofSeconds(1))
            // Read and Write Timeout: A read timeout occurs when no data was read within a certain 
            //period of time, while the write timeout when a write operation cannot finish at a specific time
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));
                connection.addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS));
            });
      
    
    
   
    //Este es un metodo Get, que busca o da a conocer informacion
    //Aqui lo que retorna es desde el metodo y mostrar todo por eso se usa el "findall"
    @GetMapping()
    public List<Customer> list() {
        return customerRepository.findAll();
    }
    
    //Igualmente muestra informacion pero solamente el "id", por eso solamente se usa
    //findById, para buscar todo lo que este dentro de id y esta dentro de parentesis
    //id, por que solo permite esa variable
    @GetMapping("/{id}")
    public Customer get(@PathVariable(name = "id") long id) {
        return customerRepository.findById(id).get();
    }
    
    
    
    //ESte metodo busca y actualiza los datos, primero lo que hace es buscar al cliente
    //mediante el id, y despues dice que si existe va a darle nuevamente code,name,iban,phone,surname
    //Una vez actualizado lo guarda en la base de datos
    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable(name = "id") long id, @RequestBody Customer input) {
         Customer find = customerRepository.findById(id).get();   
        if(find != null){     
            find.setCode(input.getCode());
            find.setName(input.getName());
            find.setIban(input.getIban());
            find.setPhone(input.getPhone());
            find.setSurname(input.getSurname());
        }
        Customer save = customerRepository.save(find);
           return ResponseEntity.ok(save);
    }
    
    
    //Aqui lo que hace es que asocia productos al cliente
    //Obtiene toda la lista de productos para despues asociarlas con el cliente
    @PostMapping
    public ResponseEntity<?> post(@RequestBody Customer input) {
        input.getProducts().forEach(x -> x.setCustomer(input));
        Customer save = customerRepository.save(input);
        return ResponseEntity.ok(save);
    }
    
    //Este metodo lo que hace es que te da toda la lista de clientes
    //Y despues mediante el metodo busca al cliente mediante id, una vez
    //Encontrado, lo eliminara
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable(name = "id") long id) {
          Optional<Customer> findById = customerRepository.findById(id);   
        if(findById.get() != null){               
                  customerRepository.delete(findById.get());  
        }
        return ResponseEntity.ok().build();
    }
    
    
    //Lo que hace este metodo es que busca por el tipo de codigo (code)
    //Para despues lanzar la lista de ClienteProductos (otro metodo) que obtiene
    //Todos los productos. Para despues buscar y te de el nombre del producto
    //Y te retorna el cliente obviamente con todo lo acordado
     @GetMapping("/full")
    public Customer getByCode(@RequestParam(name = "code") String code) {
        Customer customer = customerRepository.findByCode(code);
        List<CustomerProduct> products = customer.getProducts();
        products.forEach(x ->{
            String productName = getProductName(x.getId());
            x.setProductName(productName);
        });
        return customer;
    }
    
    
    
       //Este metodo manda a llamar la informacion desde localhost product
    //Para despues recoger el nombre del producto y darnolos como texto y retornarlo a nombre
    private String getProductName(long id) { 
        WebClient build = webClientBuilder.clientConnector(new ReactorClientHttpConnector(client))
                .baseUrl("http://localhost:8083/product")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8083/product"))
                .build();
        JsonNode block = build.method(HttpMethod.GET).uri("/" + id)
                .retrieve().bodyToMono(JsonNode.class).block();
        String name = block.get("name").asText();
        return name;
    } 
}
