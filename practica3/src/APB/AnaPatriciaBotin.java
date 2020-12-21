package APB;

import Communications.APBCommunicationAssistant;
import Drone.DroneKnowledge;
import IntegratedAgent.IntegratedAgent;
import JSONParser.APBjsonParser;

import com.eclipsesource.json.*;
import java.util.ArrayList;

public class AnaPatriciaBotin extends IntegratedAgent {

    APBCommunicationAssistant _communications;
    Administration adminData = new Administration();
    APBStatus status;
    APBjsonParser jsonParser = new APBjsonParser();

    ProductCatalogue shopsInfo = new ProductCatalogue();

    public AnaPatriciaBotin() {

    }

    @Override
    public void setup() {
        super.setup();

        this._communications = new APBCommunicationAssistant(this, "Sphinx", _myCardID);

        if (this._communications.chekingPlatform()) {
            this.status = APBStatus.SUBSCRIBED_TO_PLATFORM;
            _exitRequested = false;
        } else {
            System.out.println(this.getLocalName() + " failed subscribing to" + "Sphinx" + " and DIE");
            _exitRequested = true;
        }
    }

    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("\n\n\033[33m APB - Current Status: " + this.status);
            switch (this.status) {
                case SUBSCRIBED_TO_PLATFORM:
                    this.checkingWorld();
                    this.shareSessionIdWithAwacs();
                    break;
                case SUBSCRIBED_TO_WORLD:
                    this.shareSessionIdAndMap();
                    this.checkingRadio();
                    break;
                case SUBSCRIBED_TO_RADIO:
                    this.collectMoney();
                    this.investigateMarket();
                    break;
                case SHOPPING:
                    this.initialShopping();
                    break;
                case FINISHED_SHOPPING:
                    this.sendInitialInstructionsToDrones();
                    break;
                case RESCUEING:
                    this._communications.waitIddle();
                    this.status = APBStatus.FINISHED;

                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    void checkingWorld() {
        JsonObject response = this._communications.checkingWorld();
        if (response == null) {
            this.status = APBStatus.FINISHED;
        } else {
            this.adminData.map = this.jsonParser.getMap(response.get("map").asObject());
            this.status = APBStatus.SUBSCRIBED_TO_WORLD;

        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void shareSessionIdAndMap() {
        JsonArray parsedMap = jsonParser.parseMap(this.adminData.map);
        while (this.adminData.agentsSubscribed < 4) {
            System.out.print("\nNumero agentes:" + this.adminData.agentsSubscribed);
            this._communications.listenAndShareSessionId(parsedMap);
            this.adminData.agentsSubscribed++;
        }
    }

    public void shareSessionIdWithAwacs() {
        this._communications.shareSessionIdWithAwacs();
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void checkingRadio() {
        boolean logedIn = this._communications.checkingRadio("LISTENER");
        if (logedIn) {
            this.status = APBStatus.SUBSCRIBED_TO_RADIO;
        } else {
            this.status = APBStatus.FINISHED;
        }
    }

    void collectMoney() {
        while (this.adminData.collectedMoney < 3) {
            JsonArray money = this._communications.listenAndCollectMoney();
            this.adminData.bitcoins.addAll(jsonParser.getMoney(money));
            this.adminData.collectedMoney++;
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void investigateMarket() {
        JsonArray shops = this._communications.askShoppingCenters();
        this.shopsInfo.setCatalogue(shops);
        System.out.println("\n\nCATALOGO\n: " + this.shopsInfo.toString());
        this.status = APBStatus.SHOPPING;
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void initialShopping() {
        this.adminData.sensor1 = this.buy("THERMALDELUX");
        if (this._communications.getDronesNumber().equals(4)) {
            this.adminData.sensor2 = this.buy("THERMALDELUX");
        }

        for (int i = 0; i < this._communications.getDronesNumber(); i++) {
            this.buyRecharge();
        }

        if (this.status != APBStatus.FINISHED) {
            this.status = APBStatus.FINISHED_SHOPPING;
        }
    }

    String buy(String SensorName) {
        String sensorCode = this.buyAndGetCode(SensorName);
        if (sensorCode == null) {
            this.status = APBStatus.FINISHED;
            return null;
        } else {
            return sensorCode;
        }
    }

    void buyRecharge() {
        String sensorCode = this.buyAndGetCode("CHARGE");
        if (sensorCode != null) {
            this.adminData.rechargeTickets.add(sensorCode);
        } else {
            this.status = APBStatus.FINISHED;
        }
    }
    
    public String buyAndGetCode(String sensorName) {
        int option = 0;
        String sensorCode = null;
        Product product = null;
        while (sensorCode == null && option < 3) {
            product = this.shopsInfo.getAndDiscardBestOption(sensorName);
            ArrayList<String> payment = this.adminData.getMoney(product.getPrice());
            JsonArray parsedPayment = this.jsonParser.parseMoney(payment);
            if (product != null) {
                sensorCode = this._communications.buyCommunication(product.getSensorTicket(), product.getShop(), parsedPayment);
            }
            if (sensorCode != null) {
                this.adminData.updateWastedMoney(product.getPrice());
            }
            option++;
        }
        return sensorCode;
    }

    public void sendInitialInstructionsToDrones() {
        this.sendInitialInstructionsToSeeker("Buscador Saldaña", 1);
        this.sendInitialInstructionsToRescuer("Manuel al Rescate", 2);
        this.status = APBStatus.RESCUEING;
    }

    public void sendInitialInstructionsToSeeker(String DroneName, Integer order) {
        Integer initialPos = this.calculateDroneInitialPosition(order);
        String sensor = order == 1 ? this.adminData.sensor1 : this.adminData.sensor2;
        this._communications.sendInitialInstructions(DroneName, initialPos, this.adminData.popRechargeTicket(), sensor);
    }

    public void sendInitialInstructionsToRescuer(String DroneName, Integer order) {
        Integer initialPos = this.calculateDroneInitialPosition(order);
        this._communications.sendInitialInstructions(DroneName, initialPos, this.adminData.popRechargeTicket(), null);
    }

    public Integer calculateDroneInitialPosition(Integer order) {
        Integer initialPos = 0;
        int xSize = this.adminData.map.size();
        int ySize = this.adminData.map.get(0).size();
        switch (order) {
            case 1:
                initialPos = 14;
                while (this.adminData.map.get(initialPos).get(initialPos) > this.adminData.maxFlight) {
                    initialPos++;
                }
                this.adminData.initialPosition1 = initialPos;
                break;
            case 2:
                initialPos = this.adminData.initialPosition1 + 1;
                while (this.adminData.map.get(initialPos).get(initialPos) > this.adminData.maxFlight) {
                    initialPos++;
                }
                this.adminData.initialPosition2 = initialPos;
                break;
            case 3:
                initialPos = xSize - 14;
                while (this.adminData.map.get(initialPos).get(initialPos) > this.adminData.maxFlight) {
                    initialPos--;
                }
                this.adminData.initialPosition3 = initialPos;
                break;
            case 4:
                initialPos = this.adminData.initialPosition3 - 1;
                while (this.adminData.map.get(initialPos).get(initialPos) > this.adminData.maxFlight) {
                    initialPos--;
                }
                this.adminData.initialPosition4 = initialPos;
                break;
        }
        return initialPos;
    }

    void coordinateTeam(){

        if(this.adminData.rescued == 10){
            this.status = APBStatus.FINISHED;
            Info("\n\n\033[33m APB - MISSION COMPLETED: Se ha rescatado a los 10 alemanes");
            return;
        } 

        this.checkRescuers();
        this.checkRechargeRequests();
    }

    private void checkRescuers() {
       JsonObject response = this._communications.coordinateTeam("aleman"); 

       if(response == null){ return;}

       Coordinates aleman = this.jsonParser.getAleman(response);
       this.adminData.rescue(aleman);
    }

    private void checkRechargeRequests(){
       JsonObject response = this._communications.coordinateTeam("recharge"); 

       if(response == null){ return;}

       String ticket = this.adminData.popRechargeTicket();

       if(ticket == null){
           this.buyRecharge();
           ticket = this.adminData.popRechargeTicket(); //Habria que comprobar si no se pueden comprar mas
       }

       this._communications.sendRecharge(ticket);
    }
    
    void logout() {
//        this.checkMessagesAndOrderToLogout();
        this._communications.switchOffAwacs();
        this._communications.checkoutWorld();
        this._communications.checkoutPlatform();
        _exitRequested = true;
    }

    void checkMessagesAndOrderToLogout() {
        boolean pendingRequest = true;
        while (pendingRequest) {
            pendingRequest = this._communications.checkMessagesAndOrderToLogout();
        }
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

}
