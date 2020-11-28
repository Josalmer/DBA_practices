/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APB;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.util.Comparator;
import java.util.PriorityQueue;
/**
 * 
 * @author manuel, jose
 */
class Product {

    private String name;
    private String shop;
    private String sensorTicket;
    private int price;

    Product(String _name, String _shop, String _sensorTicket, int _price) {
        this.name = _name;
        this.sensorTicket = _sensorTicket;
        this.shop = _shop;
        this.price = _price;
    }
    
    String getSensorTicket(){ return this.sensorTicket; }
      
    String getShop(){ return this.shop; }
    
    String getName(){ return this.name; }
    
    int getPrice(){ return this.price; }
}


public class ProductCatalogue {
    
    private static final String S_THERMALDLX = "THERMALDLX";
    private static final String S_THERMALHQ = "THERMALHQ";
    private static final String S_RECHARGE = "CHARGE";
    
    private PriorityQueue<Product> thermalDLX;
    private PriorityQueue<Product> thermalHQ;
    private PriorityQueue<Product> recharge;
    
    
    /**
     * @author Manuel Pancorbo Castro
     */
    ProductCatalogue() {
        Comparator<Product> comparator = new Comparator<Product>() {
        @Override
        public int compare(Product arg0, Product arg1) {
            return Double.compare(arg0.getPrice(), arg1.getPrice());
        }
        };
        
        this.thermalDLX = new PriorityQueue(comparator);
        this.thermalHQ = new PriorityQueue(comparator);
        this.recharge = new PriorityQueue(comparator);   
    }
    
    public void setCatalogue(JsonArray catalogue){
        for(int i=0; i<catalogue.size(); i++){
            JsonObject shop = catalogue.get(i).asObject();
            this.update(shop.get("shop").asString(), shop.get("products").asArray());
        }
    }

    /**
     * @author Manuel Pancorbo Castro, Jose Saldaña
     * @param shoppingCenter tienda a la que corresponde el catalogo
     * @param catalogue array de productos disponibles en la tienda
     */
    private void update(String shoppingCenter, JsonArray catalogue) {
        for(int i = 0 ; i<catalogue.size(); i++){
            JsonObject product = catalogue.get(i).asObject();
        
            String ticket = product.get("reference").asString(); 
            String[] split = ticket.split("#");
            String name = split[0];
        
            int price = product.get("price").asInt();
        
            this.addProduct(name, new Product(name, shoppingCenter, ticket, price));
        }
    }
    
    /**
     * @author Manuel Pancorbo Castro, Jose Saldaña
     * @param sensorName nombre del producto para identificar la cola
     * @param order indica la prioridad que queremos (0-el mejor)
     */
    public Product bestOption(String productName, Integer order) {
        PriorityQueue<Product> queue = this.getQueue(productName);

	if(queue == null || order < 0 || order > queue.size())
	 return null;
       
        return (Product) queue.toArray()[order];
    }
    
    /**
     * @author Manuel Pancorbo Castro
     * @param sensorName nombre del producto para identificar la cola
     * @param product el producto a introducir en la cola
     */
    private void addProduct(String sensorName, Product product){
        sensorName = sensorName.toUpperCase();
        switch(sensorName){
            case ProductCatalogue.S_THERMALDLX:
                this.thermalDLX.add(product);
                break;
                
            case ProductCatalogue.S_THERMALHQ:
                this.thermalHQ.add(product);
                break;
                
            case ProductCatalogue.S_RECHARGE:
                this.recharge.add(product);
                break;
        }
    }
    
    /**
     * @author Manuel Pancorbo Castro
     * @param sensorName producto del que queremos la coleccion
     * @return devolvemos la cola correspondiente segun sensorName
     */
    private PriorityQueue<Product> getQueue(String sensorName){
        sensorName = sensorName.toUpperCase();
        PriorityQueue<Product> queue = null;
        switch(sensorName){
            case ProductCatalogue.S_THERMALDLX:
                queue = this.thermalDLX;
                break;
                
            case ProductCatalogue.S_THERMALHQ:
                queue = this.thermalHQ;
                break;
                
            case ProductCatalogue.S_RECHARGE:
                queue = this.recharge;
                break;
        }
        return queue;
    }
    
    
    //Alternativa intuitiva a coger por prioridad (bestOption)
     /**
     * @author Manuel Pancorbo Castro
     * @param sensorName producto del que queremos la mejor opcion
     * @return Devolvemos el producto mas barato y se descarta para la siguiente consulta
     */
    Product getAndDiscardBestOption(String sensorName){
        PriorityQueue<Product> queue = this.getQueue(sensorName);
	Product product = queue != null ? queue.poll() : null;
       	
	return product;
    }
    

    
}
