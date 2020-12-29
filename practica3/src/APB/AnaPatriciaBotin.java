package APB;

import MapOption.Coordinates;
import Communications.APBCommunicationAssistant;
import Drone.DroneKnowledge;
import IntegratedAgent.IntegratedAgent;
import JSONParser.APBjsonParser;

import com.eclipsesource.json.*;
import java.util.ArrayList;

public class AnaPatriciaBotin extends IntegratedAgent {

    boolean printMessages = true;

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
        this._communications.setPrintMessages(printMessages);
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
            if (this.printMessages) {
                Info("\n\n\033[33m APB - Current Status: " + this.status);
            }
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
                    this.coordinateTeam();
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    /**
     * Subscribe Word Manager con Analytics
     *
     * @author Jose Saldaña, Manuel Pancorbo
     */
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
     * Comparte mapa y session id con los drones
     *
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

    /**
     * Comparte session id con Awacs
     *
     * @author Domingo Lopez, Miguel García
     */
    public void shareSessionIdWithAwacs() {
        this._communications.shareSessionIdWithAwacs();
    }

    /**
     * Subscribe World Manager por Regular
     *
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

    /**
     * Recoge el dinero que le mandan los drones
     *
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void collectMoney() {
        while (this.adminData.collectedMoney < 3) {
            JsonArray money = this._communications.listenAndCollectMoney();
            this.adminData.bitcoins.addAll(jsonParser.getMoney(money));
            this.adminData.collectedMoney++;
        }
    }

    /**
     * Guarda toda la información que necesita de los distintos shopping centers
     *
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void investigateMarket() {
        JsonArray shops = this._communications.askShoppingCenters();
        this.shopsInfo.setCatalogue(shops);
        System.out.println("\n\nCATALOGO\n: " + this.shopsInfo.toString());
        this.status = APBStatus.SHOPPING;
    }

    /**
     * Compra inicial, x tickets de recarga + y ticket de thermal dlx (x = nº de
     * drones, y = nº de seekers)
     *
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

    /**
     * Compra el sensor especificado
     *
     * @author Manuel Pancorbo
     * @param SensorName nombre del sensor a comprar
     * @return Código del sensor comprado
     */
    String buy(String SensorName) {
        String sensorCode = this.buyAndGetCode(SensorName);
        if (sensorCode == null) {
            this.status = APBStatus.FINISHED;
            return null;
        } else {
            return sensorCode;
        }
    }

    /**
     * Compra un ticket de regalo
     *
     * @author Manuel Pancorbo
     */
    void buyRecharge() {
        String sensorCode = this.buyAndGetCode("CHARGE");
        if (sensorCode != null) {
            this.adminData.rechargeTickets.add(sensorCode);
        } else {
            this.status = APBStatus.FINISHED;
        }
    }

    /**
     * Compra el sensor especificado (intenta en varias tiendas)
     *
     * @author Manuel Pancorbo
     * @param sensorName nombre del sensor a comprar
     * @return Código del sensor especificado
     */
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

    /**
     * Manda las instrucciones iniciales a los drones
     *
     * @author Jose Saldaña
     */
    public void sendInitialInstructionsToDrones() {
        this.sendInitialInstructionsToSeeker("Buscador Saldaña", 1);
        this.sendInitialInstructionsToRescuer("Manuel al Rescate", 2);
        if (this._communications.getDronesNumber() == 4) {
            this.sendInitialInstructionsToSeeker("Buscador Domingo", 3);
            this.sendInitialInstructionsToRescuer("Migue al Rescate", 4);
        }
        this.status = APBStatus.RESCUEING;
    }

    /**
     * Manda las instrucciones iniciales al seeker
     *
     * @author Jose Saldaña
     * @param DroneName nombre del Drone
     * @param order número de orden del Drone
     */
    public void sendInitialInstructionsToSeeker(String DroneName, Integer order) {
        Coordinates initialPos = this.calculateDroneInitialPosition(order);
        String sensor = order == 1 ? this.adminData.sensor1 : this.adminData.sensor2;
        this._communications.sendInitialInstructions(DroneName, initialPos, this.adminData.popRechargeTicket(), sensor);
    }

    /**
     * Manda las instrucciones iniciales al rescuer
     *
     * @author Jose Saldaña
     * @param DroneName nombre del Drone
     * @param order número de orden del Drone
     */
    public void sendInitialInstructionsToRescuer(String DroneName, Integer order) {
        Coordinates initialPos = this.calculateDroneInitialPosition(order);
        this._communications.sendInitialInstructions(DroneName, initialPos, this.adminData.popRechargeTicket(), null);
    }

    /**
     * Calcula las posiciones iniciales del drone, para un nº de orden dado
     *
     * @author Jose Saldaña
     * @param order nº de orden del drone (de 1 a 4)
     * @return Coordenadas de inicio para ese drone
     */
    public Coordinates calculateDroneInitialPosition(Integer order) {
        Coordinates initialPos = new Coordinates(0, 0);
        int xSize = this.adminData.map.size();
        int ySize = this.adminData.map.get(0).size();
        switch (order) {
            case 1:
                initialPos = new Coordinates(15, 15);
                while (this.adminData.map.get(initialPos.x).get(initialPos.y) > this.adminData.maxFlight) {
                    initialPos.x++;
                    initialPos.y++;
                }
                this.adminData.initialPosition1 = initialPos;
                break;
            case 2:
                initialPos.x = this.adminData.initialPosition1.x + 5;
                initialPos.y = this.adminData.initialPosition1.y + 5;
                while (this.adminData.map.get(initialPos.x).get(initialPos.y) > this.adminData.maxFlight) {
                    initialPos.x = initialPos.x + 5;
                    initialPos.y = initialPos.y + 5;
                }
                this.adminData.initialPosition2 = initialPos;
                break;
            case 3:
                initialPos.x = xSize - 15;
                initialPos.y = ySize - 15;
                while (this.adminData.map.get(initialPos.x).get(initialPos.y) > this.adminData.maxFlight) {
                    initialPos.x--;
                    initialPos.y--;
                }
                this.adminData.initialPosition3 = initialPos;
                break;
            case 4:
                initialPos.x = this.adminData.initialPosition3.x - 5;
                initialPos.y = this.adminData.initialPosition3.y - 5;
                while (this.adminData.map.get(initialPos.x).get(initialPos.y) > this.adminData.maxFlight) {
                    initialPos.x = initialPos.x - 5;
                    initialPos.y = initialPos.y - 5;
                }
                initialPos = new Coordinates(90, 0);
                this.adminData.initialPosition4 = initialPos;
                break;
        }
        return initialPos;
    }

    /**
     * Coordina el rescate
     *
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void coordinateTeam() {
        JsonObject request = this.checkRequests();
        if (request != null) {
            switch (request.get("key").asString()) {
                case "aleman":
                    this.saveAleman(request);
                    break;
                case "recharge":
                    this.manageRecharge();
                    break;
                case "mission":
                    int rescuer = request.get("content").asObject().get("number").asInt();
                    if (rescuer == 1) {
                        this.adminData.rescuer1Iddle = true;
                    } else {
                        this.adminData.rescuer2Iddle = true;
                    }
                    break;
                case "end":
                    this.status = APBStatus.FINISHED;
                    break;
            }
        }
        if (this.adminData.rescuer1Iddle && this.adminData.alemanes.size() > 0) {
            this.sendRescueMission(1);
        } else if (this.adminData.rescuer2Iddle && this.adminData.alemanes.size() > 0) {
            this.sendRescueMission(2);
        } else if (this.adminData.rescuer1Iddle && this.adminData.rescued >= 10) {
            this.sendBackHomeMission(1);
        } else if (this.adminData.rescuer2Iddle && this.adminData.rescued >= 10) {
            this.sendBackHomeMission(2);
        }
    }

    /**
     * Chequea si hay peticiones de los drones
     *
     * @author Jose Saldaña
     * @return JsonObject con la Petición de un drone
     */
    private JsonObject checkRequests() {
        JsonObject response;

        response = this._communications.coordinateTeam("aleman");
        if (response != null) {
            return response;
        }

        response = this._communications.coordinateTeam("recharge");
        if (response != null) {
            return response;
        }

        response = this._communications.coordinateTeam("mission");
        if (response != null) {
            return response;
        }
        
        response = this._communications.coordinateTeam("end");
        if (response != null) {
            return response;
        }

        return null;
    }

    /**
     * Almacena la posición de un aleman encontrado por 1 seeker y manda nueva
     * misión a seeker
     *
     * @author Jose Saldaña
     * @param request del drone que rescato al aleman
     */
    private void saveAleman(JsonObject request) {
        Coordinates aleman = this.jsonParser.getAleman(request.get("content").asObject());
        this.adminData.alemanes.add(aleman);
        this._communications.nextSeekerMission(this.adminData.found);
    }

    /**
     * Manda a un rescuer a rescatar a un aleman
     *
     * @author Jose Saldaña
     * @param number Orden del rescuer (1 o 2)
     */
    private void sendRescueMission(int number) {
        Coordinates aleman = this.adminData.rescueAleman();
        if (number == 1) {
            this.adminData.rescuer1Iddle = false;
        } else {
            this.adminData.rescuer2Iddle = false;
        }
        this._communications.sendRescueMission(aleman, number);
    }

    /**
     * Manda a un rescuer a casa
     *
     * @author Jose Saldaña
     * @param number Orden del rescuer (1 o 2)
     */
    private void sendBackHomeMission(int number) {
        Coordinates initialPosition = new Coordinates(0, 0);
        if (number == 1) {
            initialPosition = this.adminData.initialPosition2;
            this.adminData.rescuer1Iddle = false;
        } else {
            initialPosition = this.adminData.initialPosition4;
            this.adminData.rescuer2Iddle = false;
        }
        this._communications.sendBackHomeMission(initialPosition, number);
    }

    /**
     * Compra y manda un ticket de recarga al drone que lo ha solicitado
     *
     * @author Manuel Pancorbo
     */
    private void manageRecharge() {
        String ticket = this.adminData.popRechargeTicket();

        if (ticket == null) {
            this.buyRecharge();
            ticket = this.adminData.popRechargeTicket();
        }

        this._communications.sendRecharge(ticket);
    }

    /**
     * Manda cancel al World Manager y a la Plataforma
     *
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void logout() {
        this.endMission();
        this._communications.switchOffAwacs();
        this._communications.checkoutWorld();
        this._communications.checkoutPlatform();
        _exitRequested = true;
    }

    /**
     * Manda finalizar las misiones de los drones
     *
     * @author Jose Saldaña
     */
    void endMission() {
        this._communications.sendFinishMissionMsg("Buscador Saldaña");
        this._communications.sendFinishMissionMsg("Manuel al Rescate");
        this._communications.sendFinishMissionMsg("Buscador Domingo");
        this._communications.sendFinishMissionMsg("Migue al Rescate");
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

}
