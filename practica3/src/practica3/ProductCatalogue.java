/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import com.eclipsesource.json.JsonArray;
import java.util.ArrayList;

class Product {

    String name;
    String shop;
    ArrayList<Shop> shopsInfo = new ArrayList();

    Product(String _name) {
        this.name = _name;
    }
}

class Shop {

    String shop;
    int price;

    Shop(String shopName, int productPrice) {
        this.shop = shopName;
        this.price = productPrice;
    }

}

public class ProductCatalogue {

    ArrayList<Product> catalogue = new ArrayList();
    
    ProductCatalogue() {
        this.catalogue.add(new Product("THERMALDLX"));
    }
//    Product thermalDLX = new Product("THERMALDLX");
//    Product thermalHQ = new Product("THERMALHQ");
//    Product map = new Product("MAP");
//    Product recharge = new Product("CHARGE");

    public void update(String shoppingCenter, JsonArray catalogue) {
        
    }
    
    public Product bestOption(String productName, Integer order) {
        return catalogue.get(0);
    }
}
