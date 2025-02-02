/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica2;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

public class Knowledge {
    Integer currentPositionX;
    Integer currentPositionY;
    Integer ludwigPositionX;
    Integer ludwigPositionY;
    Integer currentHeight;
    Integer energy;
    Integer orientation;
    Integer mapWidth;
    Integer mapHeight;
    Integer maxFlight;
    Double angular;
    Double distanceToLudwig;
    Integer nActionsExecuted;
    ArrayList<Integer> orientations;
    
    ArrayList<ArrayList<Integer>> map;
    ArrayList<ArrayList<Integer>> visitedAtMap;
    
    /**
     * Inicializa el mundo del agente con la información recibida al hacer login:
     * ancho, alto, máximo vuelo, etc
     * Además inicializa los mapas internos de altura de casillas y de
     * casillas visitadas en -1
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param answer JsonObject recibido al hacer login
     */
    public void initializeKnowledge(JsonObject answer) {
        this.energy = 1000;
        this.nActionsExecuted = 0;
        this.mapWidth = answer.get("width").asInt();
        this.mapHeight = answer.get("height").asInt();
        this.maxFlight = answer.get("maxflight").asInt();
        this.initializeMap();
        this.initializeVisitedAtMap();
        this.initializePossibleOrientations();
    }
    
    /**
     * Inicializa el mapa de alturas de los terrenos a -1 = altura no conocida
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     */
    public void initializeMap() {
        this.map = new ArrayList<>();
        for (int i = 0; i < this.mapWidth; i++) {
            ArrayList<Integer> row = new ArrayList<>();
            for (int j = 0; j < this.mapHeight; j++) {
                row.add(-1);
            }
            this.map.add(row);
        }
    }
    
    /**
     * Inicializa el mapa de visitado en a -1 = no visitado
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
     * Inicializa los posibles valorse a los que puede orientarse el agente:
     * -45 0 45 90 135 180 -135 -90
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param perception Percepción actualizada tras la lectura de sensores
     */
    void update(Perception perception) {
        this.currentPositionX = perception.gps.get(0);
        this.currentPositionY = perception.gps.get(1);
        this.currentHeight = perception.gps.get(2);
        this.orientation = perception.compass;
        this.angular = perception.angular;
        this.distanceToLudwig = perception.distance;
        for (int i = 0; i < perception.visual.size(); i++) {
            for (int j = 0; j < perception.visual.get(i).size(); j++) {
                int xPosition = this.currentPositionX - 3 + j;
                int yPosition = this.currentPositionY - 3 + i;
                if (xPosition >= 0 && yPosition >= 0 && xPosition < this.mapWidth && yPosition < this.mapHeight) {
                    this.map.get(xPosition).set(yPosition, perception.visual.get(i).get(j));
                }
            }
        }
        this.calculateLudwigPosition();
    }    
    
    /**
     * Calcula las coordenadas (x, y) en las que esta Ludwig usando:
     * la distancia a Ludwig + angular + trigonometría
     * @author Jose Saldaña
     */    
    void calculateLudwigPosition() {
        double alpha = 90 - this.angular;
        if (alpha < 0) {
            alpha += 360;
        }
        alpha = alpha / 180 * Math.PI;
        int xVariation = (int)Math.round(this.distanceToLudwig * Math.cos(alpha));
        int yVariation = (int)Math.round(this.distanceToLudwig * Math.sin(alpha));
        this.ludwigPositionX = this.currentPositionX + xVariation;
        this.ludwigPositionY = this.currentPositionY - yVariation;
    }
    
    /**
     * Calcula el nº de rotaciones necesarias para alcanzar una orientación determinada
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
     * @author Manuel Pancorbo
     * @param turns nº de turnos para alcanzar posición objetivo mediante rotaciones a derecha
     * @return booleano que indica si agente debe girar a derecha (falso = girar izquierda)
     */
    public boolean shouldTurnRight(int turns) {
        boolean rightTurn;
        if (turns > orientations.size() / 2)
            rightTurn = false;
        else
            rightTurn = true;

        return rightTurn;
    }
    
    /**
     * Determina si el agente debe comenzar a aproximarse al suelo para recargar
     * la batería
     * @author Jose Saldaña
     * @return booleano que indica si el agente debe recargar
     */
    public boolean needRecharge() {
        return this.energy < ((1 * (this.currentHeight - this.getFloorHeight())) + 30);
    }
    
    /**
     * Consulta si una casilla se encuentra dentro de los límites del mundo
     * @author Jose Saldaña
     * @param x componente x de una casilla
     * @param y componente y de una casilla
     * @return booleano que indica si dicha casilla se encuentra dentro del mundo
     */
    public boolean insideMap(int x, int y) {
        return (x >= 0 && x < this.mapWidth && y >= 0 && y < this.mapHeight);
    }
    
    /**
     * Comprueba si la distancia entre el agente y el suelo es menor que 5
     * @author Jose Saldaña
     * @return booleano que indica si se puede ejecutar touchD
     */
    public boolean canTouchDown() {
        return (this.currentHeight - this.getFloorHeight() < 5);
    }
    
    /**
     * Consulta si estoy encima de Ludwig
     * @author Jose Saldaña
     * @return booleano que indica si estoy sobre Ludwig
     */
    public boolean amIAboveLudwig() {
        return ((int)this.currentPositionX == (int)this.ludwigPositionX && (int)this.currentPositionY == (int)this.ludwigPositionY);
    }
    
    /**
     * Comprueba si he excedido límite de acciones sin encontrar el objetivo
     * @author Jose Saldaña
     * @return booleano que indica si no puedo alcanzar el objetivo
     */
    public boolean cantReachTarget() {
        return this.nActionsExecuted > 10000;
    }
    
    /**
     * Consulta la altura a la que se encuentra el agente
     * @author Jose Saldaña
     * @return altura actual del agente
     */
    public int getFloorHeight() {
        return this.map.get(this.currentPositionX).get(this.currentPositionY);
    }

    /**
     * Modifica el estado interno del agente tras avanzar a siguiente casilla
     * @author Domingo Lopez
     */
    public void moveForward(){
        int ABScurrentOrientation = Math.abs(this.orientation); //Valor absoluto de la orientación
        
        //Posición X
        if(ABScurrentOrientation != 0 && ABScurrentOrientation != 180)
            this.currentPositionX += this.orientation/ABScurrentOrientation;

        //Posición Y
        if(ABScurrentOrientation != 90)
            if(ABScurrentOrientation == 45 || ABScurrentOrientation == 0)
                this.currentPositionY -= 1;
            else
                this.currentPositionY += 1;
        
        // Update memory
        this.visitedAtMap.get(this.currentPositionX).set(this.currentPositionY, this.nActionsExecuted);
    }

    /**
     * Realiza un giro a izquierda o derecha
     * @author Manuel Pancorbo
     * @param actualOrientation orientación actual del agente
     * @param rightTurn booleano que indica si gira a la derecha, false = gira a izquierda
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
     * @author Domingo Lopez
     * @author Manuel Pancorbo
     * @param action acción para consultar energía
     * @param nSensors nº de sensores cargados en dron
     * @return cantidad de energía para la acción action
     */
    public int energyCost(AgentAction action, int nSensors) {
        int energy = 0;
        switch (action) {
            case moveF: energy = 1; break;
            case moveUP: energy = 5; break;
            case moveD: energy = 5; break;
            case rotateL: energy = 1; break;
            case rotateR: energy = 1; break;
            case touchD: energy = this.getFloorHeight(); break;
            case LECTURA_SENSORES: energy = nSensors; break;
        }
        return energy;
    }

    /**
     * Modifica el estado interno del agente tras realizar una acción
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @param nextMovement acción a ejecutar
     */
    public void manageMovement(AgentAction nextMovement) {
        this.nActionsExecuted += 1;
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
     * @author Manuel Pancorbo
     */
    public void fullRecharge(){
        this.energy = 1000;
    }
    
    /**
     * Comprueba si es necesario hacer una recarga antes de avanzar
     * @author Manuel Pancorbo
     * @return 1 si debería recargar en la casilla actual, 0 en caso contrario
     */
    public boolean shouldIRechargueFirst(AgentOption bestOption){
        boolean shouldIRechargue = (this.currentHeight - bestOption.floorHeight) + 30 > this.energy;
        return shouldIRechargue;
    }
}
