/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Drone;

import MapOption.Coordinates;
import com.eclipsesource.json.JsonObject;
import java.util.ArrayList;

public class DroneKnowledge {
    
    Integer currentPositionX;
    Integer currentPositionY;

    Integer currentHeight;
    Integer energy;
    Integer orientation;
    Integer mapWidth;
    Integer mapHeight;
    Integer maxFlight = 256;
    Integer nActionsExecuted;
    Integer nActionsExecutedToGetCorner;
    ArrayList<Integer> orientations;

    ArrayList<ArrayList<Integer>> map;
    ArrayList<ArrayList<Integer>> visitedAtMap;
    ArrayList<ArrayList<Double>> thermalMap; //Mapa termal de 7x7 desde la posición en la que estamos

    Integer alemanes;
    ArrayList<Coordinates> germans;

    /**
     * Inicializa el mundo del agente con la información recibida al hacer
     * login: ancho, alto, máximo vuelo, etc Además inicializa los mapas
     * internos de altura de casillas y de casillas visitadas en -1
     *
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param answer JsonObject recibido al hacer login
     */
    public void initializeKnowledge(JsonObject answer) {
        this.orientation = 90;
        this.alemanes = 0;
        this.currentPositionX = answer.get("content").asObject().get("x").asInt();
        this.currentPositionY = answer.get("content").asObject().get("y").asInt();
        this.currentHeight = this.map.get(this.currentPositionX).get(this.currentPositionY);
        this.nActionsExecuted = 0;
        this.nActionsExecutedToGetCorner = 0;
        this.initializeVisitedAtMap();
        this.initializeThermalMap();
        this.initializePossibleOrientations();
        this.germans = new ArrayList<Coordinates>();
    }

    /**
     * Inicializa el mapa de visitado en a -1 = no visitado
     *
     * @author Jose Saldaña
     */
    public void initializeVisitedAtMap() {
        this.visitedAtMap = new ArrayList<>();
        for (int i = 0; i < this.mapWidth; i++) {
            ArrayList<Integer> row = new ArrayList<>();
            for (int j = 0; j < this.mapHeight; j++) {
                row.add(-1);
            }
            this.visitedAtMap.add(row);
        }
    }

    /**
     * Inicializa el mapa de visitado en a -1 = no visitado
     *
     * @author Jose Saldaña
     */
    public void initializeThermalMap() {
        this.thermalMap = new ArrayList<>();
        for (int i = 0; i < this.mapWidth; i++) {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < this.mapHeight; j++) {
                row.add(-1.0);
            }
            this.thermalMap.add(row);
        }
    }

    /**
     * Inicializa los posibles valorse a los que puede orientarse el agente: -45
     * 0 45 90 135 180 -135 -90
     *
     * @author Manuel Pancorbo
     */
    public void initializePossibleOrientations() {
        this.orientations = new ArrayList<>();

        // -45 0 45 90 135 180 -135 -90
        this.orientations.add(-45);
        this.orientations.add(0);
        this.orientations.add(45);
        this.orientations.add(90);
        this.orientations.add(135);
        this.orientations.add(180);
        this.orientations.add(-135);
        this.orientations.add(-90);
    }

    /**
     * Actualiza el conocimiento del mundo con la última percepción recibida
     *
     * @author Jose Saldaña
     * @param thermal Percepción del Thermal actualizada tras la lectura de
     * sensores
     */
    void updateThermalMap(ArrayList<ArrayList<Double>> thermal) {
        double thermalValue;
        for (int i = 0; i < thermal.size(); i++) {
            for (int j = 0; j < thermal.get(i).size(); j++) {
                int xPosition = this.currentPositionX - 15 + j;
                int yPosition = this.currentPositionY - 15 + i;
                if (xPosition >= 0 && yPosition >= 0 && xPosition < this.mapWidth && yPosition < this.mapHeight) {
                    thermalValue = thermal.get(i).get(j);
                    if (thermalValue == 0 && this.thermalMap.get(xPosition).get(yPosition) == -1.0) {
                        this.germans.add(new Coordinates(xPosition, yPosition));
                        //this.agent._communications.informGermanFound(xPosition, yPosition);
                        this.alemanes++;
                        //if (this.agent.printMessages) {
                            System.out.println("\n\n\033[36m " + "Encontrados " + this.alemanes + " alemanes");
                        //}
                    }
                    this.thermalMap.get(xPosition).set(yPosition, thermalValue);
                }
            }
        }
    }

    Coordinates getGerman(){      
        if(this.germans.isEmpty())
            return null;
        
        Coordinates next = this.germans.get(0);
        this.germans.remove(0);
        return next;
    }
    /**
     * Calcula el nº de rotaciones necesarias para alcanzar una orientación
     * determinada
     *
     * @author Manuel Pancorbo
     * @param wantedOrientation Orientación que queremos alcanzar
     * @return nº de giros necesarios para alcanzar dicha orientación
     */
    public int howManyTurns(int wantedOrientation) {
        int turns = 0;
        int actual = this.orientations.indexOf(this.orientation);
        int next = (actual + turns) % this.orientations.size();
        while (this.orientations.get(next) != wantedOrientation) {
            turns++;
            next = (actual + turns) % this.orientations.size();
        }

        return turns;
    }

    /**
     * Determina si para alcanzar una orientación se debe girar a derecha o no,
     * en función del nº de giros para llegar rotando hacia a la derecha
     *
     * @author Manuel Pancorbo
     * @param turns nº de turnos para alcanzar posición objetivo mediante
     * rotaciones a derecha
     * @return booleano que indica si agente debe girar a derecha (falso = girar
     * izquierda)
     */
    public boolean shouldTurnRight(int turns) {
        boolean rightTurn;
        if (turns > orientations.size() / 2) {
            rightTurn = false;
        } else {
            rightTurn = true;
        }

        return rightTurn;
    }

    /**
     * Determina si el agente debe comenzar a aproximarse al suelo para recargar
     * la batería
     *
     * @author Jose Saldaña
     * @return booleano que indica si el agente debe recargar
     */
    public boolean needRecharge() {
        return this.energy < ((1 * (this.currentHeight - this.getFloorHeight())) + 120);
//        return this.energy < 900;
    }

    /**
     * Consulta si una casilla se encuentra dentro de los límites del mundo
     *
     * @author Jose Saldaña
     * @param x componente x de una casilla
     * @param y componente y de una casilla
     * @return booleano que indica si dicha casilla se encuentra dentro del
     * mundo
     */
    public boolean insideMap(int x, int y) {
        return (x >= 0 && x < this.mapWidth && y >= 0 && y < this.mapHeight);
    }

    /**
     * Comprueba si la distancia entre el agente y el suelo es menor que 5
     *
     * @author Jose Saldaña
     * @return booleano que indica si se puede ejecutar touchD
     */
    public boolean canTouchDown() {
        return (this.currentHeight - this.getFloorHeight() < 5);
    }

    /**
     * Consulta si estoy encima de Ludwig
     *
     * @author Jose Saldaña
     * @return booleano que indica si estoy sobre Ludwig
     */
    public boolean amIAboveTarget(int targetPositionX, int targetPositionY) {
        return ((int) this.currentPositionX == targetPositionX
                && (int) this.currentPositionY == targetPositionY);
    }

    /**
     * Comprueba si he excedido límite de acciones sin encontrar el objetivo
     *
     * @author Jose Saldaña, Domingo López
     * @return booleano que indica si no puedo alcanzar el objetivo
     */
    public boolean maxLimitActionPermited() {
        return this.nActionsExecuted > 10000;
    }

    /**
     * Comprueba si he excedido límite de acciones para llegar a las esquinas
     *
     * @author Domingo López
     * @return booleano que indica si no puedo llegar a las esquinas
     */
    public boolean cantReachTarget() {
        return this.nActionsExecutedToGetCorner > 800;
    }

    /**
     * Consulta la altura a la que se encuentra el agente
     *
     * @author Jose Saldaña
     * @return altura actual del agente
     */
    public int getFloorHeight() {
        return this.map.get(this.currentPositionX).get(this.currentPositionY);
    }

    /**
     * Modifica el estado interno del agente tras avanzar a siguiente casilla
     *
     * @author Domingo Lopez
     */
    public void moveForward() {
        int ABScurrentOrientation = Math.abs(this.orientation); // Valor absoluto de la orientación

        // Posición X
        if (ABScurrentOrientation != 0 && ABScurrentOrientation != 180) {
            this.currentPositionX += this.orientation / ABScurrentOrientation;
        }

        // Posición Y
        if (ABScurrentOrientation != 90) {
            if (ABScurrentOrientation == 45 || ABScurrentOrientation == 0) {
                this.currentPositionY -= 1;
            } else {
                this.currentPositionY += 1;
            }
        }

        // Update memory
        this.visitedAtMap.get(this.currentPositionX).set(this.currentPositionY, this.nActionsExecuted);
    }

    /**
     * Realiza un giro a izquierda o derecha
     *
     * @author Manuel Pancorbo
     * @param actualOrientation orientación actual del agente
     * @param rightTurn booleano que indica si gira a la derecha, false = gira a
     * izquierda
     * @return Nueva orientación del agente tras giro
     */
    public int getNextOrientation(int actualOrientation, boolean rightTurn) {
        int turn = rightTurn ? 1 : 7;
        int actual = this.orientations.indexOf(actualOrientation);
        int next = (actual + turn) % this.orientations.size();

        return this.orientations.get(next);
    }

    /**
     * Devuelve la energía que gasta una acción
     *
     * @author Domingo Lopez
     * @author Manuel Pancorbo
     * @param action acción para consultar energía
     * @param nSensors nº de sensores cargados en dron
     * @return cantidad de energía para la acción action
     */
    public int energyCost(DroneAction action) {
        int energy = 0;
        switch (action) {
            case moveF:
                energy = 1;
                break;
            case moveUP:
                energy = 5;
                break;
            case moveD:
                energy = 5;
                break;
            case rotateL:
                energy = 1;
                break;
            case rotateR:
                energy = 1;
                break;
            case touchD:
                energy = this.currentHeight - this.getFloorHeight();
                break;
            case LECTURA_SENSORES:
                energy = 8;
                break;
        }
        return energy;
    }

    /**
     * Modifica el estado interno del agente tras realizar una acción
     *
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @param nextMovement acción a ejecutar
     */
    public void manageMovement(DroneAction nextMovement) {
        this.nActionsExecuted += 1;
        this.nActionsExecutedToGetCorner += 1;
        switch (nextMovement) {
            case moveF:
                this.moveForward();
                break;
            case moveUP:
                this.currentHeight += 5;
                break;
            case moveD:
                this.currentHeight -= 5;
                break;
            case rotateL:
                this.orientation = getNextOrientation(this.orientation, false);
                break;
            case rotateR:
                this.orientation = getNextOrientation(this.orientation, true);
                break;
            case touchD:
                this.currentHeight = this.getFloorHeight();
                break;
        }
    }

    /**
     * Recarga completamente la batería
     *
     * @author Manuel Pancorbo
     */
    public void fullRecharge() {
        this.energy = 1000;
    }

    /**
     * Comprueba si necesita recargar antes de optar por la opción ganadora
     *
     * @author Domingo López
     */
    public boolean shouldIRechargueFirst(SeekerOption bestOption) {
        boolean shouldIRechargue = (this.currentHeight - bestOption.floorHeight) + 30 > this.energy;
        return shouldIRechargue;
    }

    public boolean shouldIRechargueFirst(DroneOption bestOption) {
        boolean shouldIRechargue = (this.currentHeight - bestOption.floorHeight) + 30 > this.energy;
        return shouldIRechargue;
    }

    /**
     * Comprueba si en el mapa thermal actual hay alemanes, y que no hemos
     * avisado ya.
     *
     * @author Domingo López
     */
    public ArrayList<Integer> checkIfFrankfurts(ArrayList<JsonObject> alemanes) {
        ArrayList<Integer> indicesAlemanesEncontrados = new ArrayList<>();

        for (int i = 0; i < this.thermalMap.size(); i++) {
            for (int j = 0; j < this.thermalMap.get(i).size(); j++) {
                if (this.thermalMap.get(i).get(j) == 0) {
                    //Hemos encontrado un alemán.
                    int xAleman = this.currentPositionX - 15 + i; //15 Porque en el Thermal estamos siempre en la posición (15,15)
                    int yAleman = this.currentPositionY - 15 + j;

                    //Debemos comprobar que ese Alemán no lo hemos encontrado ya, para no volver a decírselo a Anapatricia
                    boolean terminado = false;
                    for (int k = 0; k < alemanes.size() && !terminado; k++) {
                        int XalemanCapturado = alemanes.get(k).asObject().get("x").asInt();
                        int YalemanCapturado = alemanes.get(k).asObject().get("y").asInt();

                        if (XalemanCapturado == xAleman && YalemanCapturado == yAleman) {
                            //Descartamos el alemán.
                            terminado = true;
                        }
                    }

                    if (!terminado) {//El alemán no está entre los que hemos recogido.
                        JsonObject aleman = new JsonObject();
                        aleman.set("x", xAleman);
                        aleman.set("y", yAleman);
                        alemanes.add(aleman);
                        indicesAlemanesEncontrados.add(alemanes.size() - 1);
                    }
                }

            }

        }

        return indicesAlemanesEncontrados;
    }

}
